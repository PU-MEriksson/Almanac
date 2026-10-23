package com.gezimos.katapult.util

import com.gezimos.katapult.model.AppModel

/** One entry in the manual app order: an app, or a page break that starts a new page. */
sealed interface OrderItem {
    data class App(val app: AppModel) : OrderItem
    data class PageBreak(val title: String = "") : OrderItem
}

/**
 * One screen of the app drawer. [breakIndex] is the position in the order list of the
 * page break this page belongs to (-1 for apps before the first break). A group that
 * overflows onto more pages repeats the same [title] and [breakIndex] on each of them.
 */
data class AppPage(val title: String, val apps: List<AppModel>, val breakIndex: Int)

/** Page breaks in the manual app order. Plain Kotlin so it can be unit tested. */
object AppOrder {
    /** Saved key for a page break. Package names can't start with '#', so it can't clash. */
    const val BREAK_PREFIX = "#page:"

    fun isBreakKey(key: String) = key.startsWith(BREAK_PREFIX)

    fun encode(items: List<OrderItem>): List<String> = items.map {
        when (it) {
            is OrderItem.App -> it.app.key
            is OrderItem.PageBreak -> BREAK_PREFIX + it.title
        }
    }

    fun appsOf(items: List<OrderItem>): List<AppModel> =
        items.mapNotNull { (it as? OrderItem.App)?.app }

    /**
     * A new page starts at a page break or when the page is full. Groups with no apps
     * (two breaks in a row, a break at the end) never make an empty page; the last of
     * several adjacent breaks gives the title. Always returns at least one page.
     */
    fun split(items: List<OrderItem>, perPage: Int): List<AppPage> {
        val size = perPage.coerceAtLeast(1)
        val pages = mutableListOf<AppPage>()
        var title = ""
        var breakIndex = -1
        var current = mutableListOf<AppModel>()
        fun flush() {
            if (current.isEmpty()) return
            pages += AppPage(title, current, breakIndex)
            current = mutableListOf()
        }
        items.forEachIndexed { i, item ->
            when (item) {
                is OrderItem.PageBreak -> {
                    flush()
                    title = item.title
                    breakIndex = i
                }
                is OrderItem.App -> {
                    if (current.size == size) flush()
                    current += item.app
                }
            }
        }
        flush()
        return pages.ifEmpty { listOf(AppPage("", emptyList(), -1)) }
    }

    /**
     * Matches the saved order against the installed apps. Page breaks are always kept and
     * apps that are gone are dropped. New apps go at the end (the last page) when the order
     * has page breaks; without breaks they're slotted in alphabetically, as before.
     */
    fun reconcile(savedOrder: List<String>, currentApps: List<AppModel>): List<OrderItem> {
        val appMap = currentApps.associateBy { it.key }.toMutableMap()
        val result = mutableListOf<OrderItem>()
        for (key in savedOrder) {
            if (isBreakKey(key)) {
                result += OrderItem.PageBreak(key.removePrefix(BREAK_PREFIX))
            } else {
                appMap.remove(key)?.let { result += OrderItem.App(it) }
            }
        }

        val newApps = appMap.values.sortedBy { it.label.lowercase() }
        if (result.any { it is OrderItem.PageBreak }) {
            return result + newApps.map { OrderItem.App(it) }
        }

        val apps = appsOf(result).toMutableList()
        for (app in newApps) {
            val label = app.label.lowercase()
            val anchors = longestNonDecreasingIndices(apps)
            val insertIndex = when {
                anchors.isEmpty() -> 0
                else -> {
                    val idx = anchors.indexOfFirst { apps[it].label.lowercase() > label }
                    if (idx < 0) anchors.last() + 1 else anchors[idx]
                }
            }
            apps.add(insertIndex, app)
        }
        return apps.map { OrderItem.App(it) }
    }

    private fun longestNonDecreasingIndices(list: List<AppModel>): List<Int> {
        val n = list.size
        if (n == 0) return emptyList()
        val labels = List(n) { list[it].label.lowercase() }
        val tails = IntArray(n)
        val prev = IntArray(n) { -1 }
        var length = 0
        for (i in 0 until n) {
            val l = labels[i]
            var lo = 0
            var hi = length
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (labels[tails[mid]] > l) hi = mid else lo = mid + 1
            }
            if (lo > 0) prev[i] = tails[lo - 1]
            tails[lo] = i
            if (lo == length) length++
        }
        val out = ArrayDeque<Int>()
        var k = tails[length - 1]
        while (k != -1) {
            out.addFirst(k)
            k = prev[k]
        }
        return out.toList()
    }

    private fun indexOfApp(items: List<OrderItem>, key: String) =
        items.indexOfFirst { it is OrderItem.App && it.app.key == key }

    /** True if [key] is the first app in the list or comes right after a page break. */
    fun startsPage(items: List<OrderItem>, key: String): Boolean {
        val i = indexOfApp(items, key)
        if (i < 0) return false
        return items.subList(0, i).none { it is OrderItem.App } || items[i - 1] is OrderItem.PageBreak
    }

    /** True if the item right before [key] is a page break that can be removed. */
    fun hasBreakBefore(items: List<OrderItem>, key: String): Boolean {
        val i = indexOfApp(items, key)
        return i > 0 && items[i - 1] is OrderItem.PageBreak
    }

    fun insertBreakBefore(items: List<OrderItem>, key: String): List<OrderItem> {
        val i = indexOfApp(items, key)
        if (i < 0 || startsPage(items, key)) return items
        return items.toMutableList().apply { add(i, OrderItem.PageBreak()) }
    }

    fun removeBreakBefore(items: List<OrderItem>, key: String): List<OrderItem> {
        if (!hasBreakBefore(items, key)) return items
        return items.toMutableList().apply { removeAt(indexOfApp(items, key) - 1) }
    }

    fun removeBreakAt(items: List<OrderItem>, index: Int): List<OrderItem> {
        if (items.getOrNull(index) !is OrderItem.PageBreak) return items
        return items.toMutableList().apply { removeAt(index) }
    }

    /** Title of the page group [key] belongs to. */
    fun titleOf(items: List<OrderItem>, key: String): String {
        val i = indexOfApp(items, key)
        if (i < 0) return ""
        return (items.subList(0, i).lastOrNull { it is OrderItem.PageBreak } as? OrderItem.PageBreak)?.title ?: ""
    }

    /**
     * Sets the title of the group [key] belongs to. For apps before the first break, a
     * titled break is added at the very top.
     */
    fun setTitle(items: List<OrderItem>, key: String, title: String): List<OrderItem> {
        val i = indexOfApp(items, key)
        if (i < 0) return items
        val clean = title.trim()
        val breakIndex = items.subList(0, i).indexOfLast { it is OrderItem.PageBreak }
        return items.toMutableList().apply {
            if (breakIndex < 0) {
                if (clean.isNotEmpty()) add(0, OrderItem.PageBreak(clean))
            } else {
                set(breakIndex, OrderItem.PageBreak(clean))
            }
        }
    }

    /**
     * Moves a whole page group (its break, title and apps, including overflow pages) past
     * the neighbouring group: [step] -1 = earlier, +1 = later. [breakIndex] is the group's
     * [AppPage.breakIndex]. Apps before the first break get an untitled break of their own
     * so they can move too. Breaks without apps stay where they are. Returns [items]
     * unchanged when there's nothing to move past.
     */
    fun moveGroup(items: List<OrderItem>, breakIndex: Int, step: Int): List<OrderItem> {
        var list = items
        var start = breakIndex
        if (items.firstOrNull() !is OrderItem.PageBreak) {
            list = listOf(OrderItem.PageBreak()) + items
            start = breakIndex + 1
        }
        val starts = list.indices.filter { list[it] is OrderItem.PageBreak }
        val groups = starts.mapIndexed { i, s -> list.subList(s, starts.getOrElse(i + 1) { list.size }) }
        val withApps = groups.indices.filter { g -> groups[g].any { it is OrderItem.App } }
        val pos = withApps.indexOf(starts.indexOf(start))
        val other = withApps.getOrNull(pos + step)
        if (pos < 0 || other == null) return items
        val me = withApps[pos]
        return groups.toMutableList().apply {
            this[me] = groups[other]
            this[other] = groups[me]
        }.flatten()
    }

    /** Swaps two apps' places. Page breaks stay where they are. */
    fun swap(items: List<OrderItem>, keyA: String, keyB: String): List<OrderItem> {
        val a = indexOfApp(items, keyA)
        val b = indexOfApp(items, keyB)
        if (a < 0 || b < 0 || a == b) return items
        return items.toMutableList().apply {
            val temp = this[a]
            this[a] = this[b]
            this[b] = temp
        }
    }

    /** Moves app [key] to right after the last app of [page] (an empty slot on that page). */
    fun moveToEndOf(items: List<OrderItem>, key: String, page: AppPage): List<OrderItem> {
        val from = indexOfApp(items, key)
        val anchor = page.apps.lastOrNull()?.key ?: return items
        if (from < 0 || anchor == key) return items
        val list = items.toMutableList()
        val moved = list.removeAt(from)
        list.add(indexOfApp(list, anchor) + 1, moved)
        return list
    }
}
