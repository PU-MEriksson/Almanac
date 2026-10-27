I've forked Katapult, an Android launcher (Kotlin + Jetpack Compose) for the Mudita Kompakt e-ink phone. I don't know Kotlin, so please explain what you change and why in plain terms as we go. I'm new to Kotlin/Java/Android builds (my background is web development and a React Native Supernote plugin), so explain tooling steps too.

## Goal

Add "sky info" under the clock, on both the home screen and the screensaver, as three lines:

```
(moon) 62% waning
(sunrise) 07:58  (sunset) 17:32
Samhain in 9 days
```

- Moon phase: a small drawn moon disc + illumination % + waxing/waning
- Today's sunrise and sunset
- Days until the next point in the wheel of the year
  - Solstices/equinoxes in plain English: Winter solstice, Spring equinox, Summer solstice, Autumn equinox
  - Cross-quarter days with Celtic names: Imbolc, Beltane, Lughnasadh, Samhain
  - Say "today" / "tomorrow" instead of "in 0 days" / "in 1 day". Days are counted as calendar days in the phone's time zone.

## Build and install

- Build the debug APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/katapult-v1.6-debug.apk`
- The debug build installs as a separate app ("Katapult Debug", `com.gezimos.katapult.debug`) next to the original Katapult. I install it via Mudita Center (or `adb install` over USB) and pick it as the Home app.
- Tools already on my Mac: JDK 21 (Homebrew, `JAVA_HOME` set), Android SDK in `~/Library/Android/sdk`, Android Studio. The clean build was verified on 2026-10-23.
- The built-in update checker (Settings → Check for Updates) only tells me when the original Katapult has a new release. It doesn't download or install anything, because the official APK would replace my fork's changes.
- Keep "Katapult Action Service" (the accessibility service) turned off in Settings → Accessibility. While it's on, some apps refuses to run. Right now I don't need the features it provides.

## Syncing with the original repo

`origin` is my fork (PU-MEriksson/Katapult), `upstream` is the original (gezimos/Katapult). When the update checker reports a new release:

```
git fetch upstream            # download their new commits (changes nothing locally)
git log main..upstream/main   # see what's new
git checkout main
git checkout -b sync-upstream
git merge upstream/main       # bring their changes into my code
./gradlew assembleDebug       # check it still builds, then test on the phone
```

- Merge, don't rebase (my commits are already pushed to GitHub).
- If git reports conflicts, resolve them with me before building.
- To keep conflicts rare, change upstream's files as little as possible and keep my own features in their own files.

## Design requirements

1. **Separate astro module.** Put all astronomy calculations in a separate Gradle module (`:astro`) of plain Kotlin with no Android dependencies, so it can be unit tested. Direct code reuse isn't important (my possible future projects are a Supernote plugin in React Native/TypeScript and a Pebble watch app in C, so they'd be ports). Write it so it's easy to port: small, clearly commented functions that name the source (e.g. Meeus chapter), and test values in a plain table that can be reused.
2. **Calculations, all offline:**
   - Sunrise/sunset: NOAA algorithm (written ourselves, so it is portable and has no dependencies).
   - Moon phase and illumination: algorithm accurate to within a few hours is fine.
   - Solstices/equinoxes: Jean Meeus' algorithms.
   - Cross-quarter days: setting to choose between traditional fixed dates (Feb 1, May 1, Aug 1, Nov 1) and astronomical midpoints (sun at ecliptic longitude 315°, 45°, 135°, 225°). Default: traditional.
   - Setting to hide the cross-quarter days entirely (only show solstices/equinoxes).
   - Northern hemisphere only.
3. **Midnight sun / polar night:** on days with no sunrise or sunset, the sun line shows "Midnight sun" (with a sun icon) or "Polar night" (with the drawn moon).
4. **Location:**
   - Latitude/longitude entered manually in settings. Accept both `59.33` and `59,33` (decimal comma). Reject values outside ±90 / ±180.
   - A "Use my current location" button that asks for location permission when tapped (only then), gets one position fix, saves it rounded to ~2 decimals, and stops. No background location, no continuous GPS. The Kompakt has no Google services, so use Android's built-in `LocationManager`.
   - Times are shown in the phone's time zone. That handles summer/winter time, and the coordinates decide the actual sunrise/sunset for where I am.
5. **Performance on e-ink:** compute sky values once per day, not every 15 seconds like the clock. Reuse the existing clock tick: on each tick, recompute only if the date has changed since the last computation. Also recompute when the sky settings or the location change.
6. **Moon icon:** draw it in Compose: an outline circle with a solid-ink lit or dark part, the shadow edge drawn as a proper half-ellipse. No grey shading or gradients (e-ink). Don't use emoji or Unicode moon glyphs, which may not render in the device font.
7. **Sun icons:** use Material icons (already used for the battery/alarm), not Unicode ☀.
8. **Settings toggle** to show/hide the sky info. It is off by default, and the lines only show when the toggle is on AND valid coordinates are set.
9. Match the existing visual style (fonts, ink colour, spacing).

## Verification

Use the unit tests to check results against known values from timeanddate.com for a few dates (e.g. the 2026 winter solstice, a known full moon, sunrise for a fixed location in Sweden, including a summer/winter time change day and a midnight sun / polar night case). Show me the expected vs actual numbers. Tolerances: sunrise/sunset ±2 min, solstices/equinoxes ±2 min, new/full moon ±1 h, illumination ±2 percentage points.

## Workflow

- Work on a new branch, let me know when it is time to commit and I will do so myself.
- Start by showing me a short plan and wait for my OK before writing code.
- The project is GPL-3; keep the license intact.

## Ideas for later

- Swedish holidays (midsommar, valborg, allhelgona, påsk) as an alternative or addition to the wheel of the year.
