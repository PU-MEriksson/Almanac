package com.gezimos.katapult

import com.gezimos.katapult.model.AppModel
import com.gezimos.katapult.util.AppOrder
import com.gezimos.katapult.util.OrderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOrderTest {
    private fun app(name: String) = AppModel("pkg.$name", name, "Main")
    private fun a(name: String) = OrderItem.App(app(name))
    private fun br(title: String = "") = OrderItem.PageBreak(title)

    /** Pages as readable text, e.g. ["Nature: A B", ": C"]. */
    private fun pages(items: List<OrderItem>, perPage: Int) =
        AppOrder.split(items, perPage).map { p -> "${p.title}: " + p.apps.joinToString(" ") { it.label } }

    private fun labels(items: List<OrderItem>) =
        items.map { if (it is OrderItem.App) it.app.label else "|${(it as OrderItem.PageBreak).title}" }

    // --- split ---

    @Test
    fun withoutBreaksSlicesIntoFullPages() {
        assertEquals(listOf(": A B", ": C"), pages(listOf(a("A"), a("B"), a("C")), 2))
    }

    @Test
    fun breakStartsNewPageEvenIfNotFull() {
        val items = listOf(br("Nature"), a("A"), a("B"), br("Tools"), a("C"))
        assertEquals(listOf("Nature: A B", "Tools: C"), pages(items, 4))
    }

    @Test
    fun overflowContinuesWithSameTitle() {
        val items = listOf(br("Nature"), a("A"), a("B"), a("C"), br("Tools"), a("D"))
        assertEquals(listOf("Nature: A B", "Nature: C", "Tools: D"), pages(items, 2))
        val split = AppOrder.split(items, 2)
        assertEquals(split[0].breakIndex, split[1].breakIndex)
    }

    @Test
    fun emptyListGivesOneEmptyPage() {
        assertEquals(listOf(": "), pages(emptyList(), 4))
    }

    @Test
    fun breakAtStartOrEndMakesNoEmptyPage() {
        assertEquals(listOf("X: A B"), pages(listOf(br("X"), a("A"), a("B"), br("Y")), 4))
    }

    @Test
    fun twoBreaksInARowMakeNoEmptyPageAndLaterTitleWins() {
        val items = listOf(a("A"), br("Old"), br("New"), a("B"))
        assertEquals(listOf(": A", "New: B"), pages(items, 4))
    }

    @Test
    fun onlyBreaksGivesOneEmptyPage() {
        assertEquals(listOf(": "), pages(listOf(br("X"), br("Y")), 4))
    }

    // --- reconcile ---

    @Test
    fun oldOrderWithoutBreaksLoadsUnchanged() {
        val installed = listOf(app("B"), app("A"), app("C"))
        val result = AppOrder.reconcile(listOf("pkg.C", "pkg.A", "pkg.B"), installed)
        assertEquals(listOf("C", "A", "B"), labels(result))
    }

    @Test
    fun breaksSurviveAndRemovedAppsAreDropped() {
        val saved = listOf("#page:Nature", "pkg.A", "pkg.Gone", "#page:Tools", "pkg.B")
        val result = AppOrder.reconcile(saved, listOf(app("A"), app("B")))
        assertEquals(listOf("|Nature", "A", "|Tools", "B"), labels(result))
    }

    @Test
    fun newAppsGoOnTheLastPageWhenThereAreBreaks() {
        val saved = listOf("#page:Nature", "pkg.M", "#page:Tools", "pkg.T")
        val installed = listOf(app("M"), app("T"), app("Z"), app("B"))
        val result = AppOrder.reconcile(saved, installed)
        assertEquals(listOf("|Nature", "M", "|Tools", "T", "B", "Z"), labels(result))
    }

    @Test
    fun newAppsAreSortedInWhenThereAreNoBreaks() {
        val result = AppOrder.reconcile(listOf("pkg.A", "pkg.C"), listOf(app("A"), app("C"), app("B")))
        assertEquals(listOf("A", "B", "C"), labels(result))
    }

    @Test
    fun encodeAndReconcileRoundTrip() {
        val items = listOf(br("Nature"), a("A"), br(), a("B"))
        val keys = AppOrder.encode(items)
        assertEquals(listOf("#page:Nature", "pkg.A", "#page:", "pkg.B"), keys)
        assertEquals(items, AppOrder.reconcile(keys, listOf(app("A"), app("B"))))
    }

    // --- editing ---

    @Test
    fun insertAndRemoveBreak() {
        val items = listOf(a("A"), a("B"), a("C"))
        val withBreak = AppOrder.insertBreakBefore(items, "pkg.B")
        assertEquals(listOf("A", "|", "B", "C"), labels(withBreak))
        assertTrue(AppOrder.hasBreakBefore(withBreak, "pkg.B"))
        assertEquals(withBreak, AppOrder.insertBreakBefore(withBreak, "pkg.B")) // no double break
        assertEquals(items, AppOrder.insertBreakBefore(items, "pkg.A")) // first app already starts a page
        assertEquals(items, AppOrder.removeBreakBefore(withBreak, "pkg.B"))
    }

    @Test
    fun startsPageForFirstAppAndAfterBreak() {
        val items = listOf(br("X"), a("A"), a("B"), br(), a("C"))
        assertTrue(AppOrder.startsPage(items, "pkg.A"))
        assertFalse(AppOrder.startsPage(items, "pkg.B"))
        assertTrue(AppOrder.startsPage(items, "pkg.C"))
    }

    @Test
    fun setTitleRenamesGroupOrAddsBreakAtTop() {
        val items = listOf(a("A"), br("Old"), a("B"), a("C"))
        assertEquals(listOf("A", "|New", "B", "C"), labels(AppOrder.setTitle(items, "pkg.C", " New ")))
        assertEquals(listOf("|Top", "A", "|Old", "B", "C"), labels(AppOrder.setTitle(items, "pkg.A", "Top")))
        assertEquals(listOf("A", "|", "B", "C"), labels(AppOrder.setTitle(items, "pkg.B", "")))
        assertEquals("Old", AppOrder.titleOf(items, "pkg.C"))
    }

    @Test
    fun swapAcrossPagesKeepsBreaks() {
        val items = listOf(br("Nature"), a("A"), br("Tools"), a("B"))
        assertEquals(listOf("|Nature", "B", "|Tools", "A"), labels(AppOrder.swap(items, "pkg.A", "pkg.B")))
    }

    @Test
    fun movePageGroupLaterAndEarlier() {
        val items = listOf(br("Nature"), a("A"), a("B"), br("Tools"), a("C"))
        val toolsFirst = AppOrder.moveGroup(items, 0, +1)
        assertEquals(listOf("|Tools", "C", "|Nature", "A", "B"), labels(toolsFirst))
        assertEquals(toolsFirst, AppOrder.moveGroup(items, 3, -1))
    }

    @Test
    fun movingGroupTakesItsOverflowPagesAlong() {
        val items = listOf(br("Nature"), a("A"), a("B"), a("C"), br("Tools"), a("D"))
        val moved = AppOrder.moveGroup(items, 4, -1)
        assertEquals(listOf("Tools: D", "Nature: A B", "Nature: C"), pages(moved, 2))
    }

    @Test
    fun appsBeforeFirstBreakGetTheirOwnBreakWhenMoved() {
        val items = listOf(a("A"), br("Tools"), a("B"))
        assertEquals(listOf("|Tools", "B", "|", "A"), labels(AppOrder.moveGroup(items, -1, +1)))
        assertEquals(listOf("|Tools", "B", "|", "A"), labels(AppOrder.moveGroup(items, 1, -1)))
    }

    @Test
    fun cannotMovePastTheEnds() {
        val items = listOf(a("A"), br("Tools"), a("B"))
        assertEquals(items, AppOrder.moveGroup(items, -1, -1))
        assertEquals(items, AppOrder.moveGroup(items, 1, +1))
        assertEquals(emptyList<OrderItem>(), AppOrder.moveGroup(emptyList(), -1, +1))
    }

    @Test
    fun movingSkipsBreaksWithoutApps() {
        val items = listOf(br("X"), a("A"), br("Empty"), br("Y"), a("B"))
        assertEquals(listOf("Y: B", "X: A"), pages(AppOrder.moveGroup(items, 0, +1), 4))
    }

    @Test
    fun moveToEmptySlotGoesToEndOfThatPage() {
        val items = listOf(br("Nature"), a("A"), br("Tools"), a("B"), a("C"))
        val nature = AppOrder.split(items, 4)[0]
        assertEquals(listOf("|Nature", "A", "C", "|Tools", "B"), labels(AppOrder.moveToEndOf(items, "pkg.C", nature)))
    }
}
