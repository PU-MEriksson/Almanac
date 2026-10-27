# Almanac

Almanac is a launcher for the Mudita Kompakt e-ink phone, based on [Katapult](https://github.com/gezimos/Katapult) by gezimos, a simple grid launcher. It's my personal version and keeps everything from the original. It adds two things: sky info under the clock, and more control over how apps are arranged into pages.

For everything Almanac has from Katapult (the app grid, music widget, icon tinting, notification badges, screensaver and more), see the [original Katapult README](https://github.com/gezimos/Katapult#readme).

<div align="center">
    <table>
        <tr>
            <td><img src='docs/img/screenshots/fork_home_sky.png' height='300' alt='Home screen with sky info'></td>
            <td><img src='docs/img/screenshots/fork_pages2.png' height='300' alt='App page with a title'></td>
            <td><img src='docs/img/screenshots/fork_pages1.png' height='300' alt='App page with a title'></td>
            <td><img src='docs/img/screenshots/fork_sky_settings.png' height='300' alt='Sky info settings'></td>
        </tr>
    </table>
</div>

## What Almanac adds

### Sky info under the clock

Three short lines under the clock, on both the home screen and the screensaver:

```
(moon)  62% waning
(sun)   07:58 – 17:32
Samhain in 9 days
```

- **Moon phase:** a small moon drawn in solid black and white (no grey, so it stays crisp on e-ink), with how much of the moon is lit and whether it's waxing or waning.
- **Sunrise and sunset** for today, in the phone's time zone. On days when the sun never sets or never rises, the line says "Midnight sun" or "Polar night".
- **The next point in the wheel of the year:** the solstices and equinoxes, plus the Celtic cross-quarter days Imbolc, Beltane, Lughnasadh and Samhain. It says "today" or "tomorrow" when it's that close.

Settings (long-press an empty area of the home screen):

- **Show Sky Info:** off by default.
- **Sky Location:** type the latitude and longitude (both `59.33` and `59,33` work), or tap "Use my current location". The phone then takes a single position fix and saves it rounded to about 1 km. There's no background location tracking and no Google services are needed.
- **Cross-Quarter Days:** _Traditional_ (1 Feb, 1 May, 1 Aug, 1 Nov), _Astronomical_ (the day the sun is exactly halfway between a solstice and an equinox, a few days later), or _Hidden_.
- **Sky Info Card:** optionally show the lines inside a framed card.

Everything is calculated on the phone, offline, once a day. The calculations live in their own module, [`astro/`](astro/), written in plain Kotlin with no Android code. They use the NOAA sunrise/sunset algorithm and Jean Meeus' _Astronomical Algorithms_, and unit tests check the results against timeanddate.com. Sky info is designed for the northern hemisphere.

### App pages with titles

Apps can be arranged into pages of your own. Choose _Custom_ under Settings → Order Apps. The other choices (_Alphabetical_, _Most Recent_, _Most Used_) sort the apps automatically, and your custom layout is kept for when you switch back.

- **Reorder** now also lets you move an app to an empty slot, not just swap two apps.
- **Start new page here** (long-press an app) starts a new page with that app.
- **Page titles:** give a page a name, for example "Travel" or "Tools".
- **Move page earlier / later:** long-press a page title to move the whole page.
- "Reset app order to alphabetical" also removes all page breaks.

### Update checker

Settings → Check for Updates only _tells_ you when the original Katapult has a new release. It doesn't download anything, since installing the official APK would replace this version.

## Installing

There are no ready-made downloads. Build it yourself with JDK 21 and the Android SDK:

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/almanac-v1.6-debug.apk
```

It installs as "Almanac Debug", a separate app from the original Katapult, so both can be installed side by side. Choose it as the Home app in the phone's settings.

> [!TIP]
> If you use any app with fraud protection, leave "Almanac Action Service" turned off in Settings → Accessibility. While any accessibility service is on, some apps refuse to start. This applies to the original Katapult too. The service is only needed for the lock screen widgets, hiding the status bar clock, and locking the phone from the screensaver.

## How Almanac was made

Almanac's additions were built with Claude Code. My background is in web development, not Kotlin or Android.

The requirements I gave it are in [CLAUDE.md](CLAUDE.md), the instructions file Claude Code reads in this project. The sky calculations are checked by unit tests against published values from timeanddate.com, with the expected and actual numbers compared within set tolerances (for example ±2 minutes for sunrise and sunset).

Katapult itself, everything Almanac inherits, is gezimos's own work.

## Permissions

Almanac asks for the same permissions as Katapult, plus location for the sky info. Each one is only used for the feature named here.

| Permission                                       | Why                                                                                                                                                                                  |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `QUERY_ALL_PACKAGES`                             | List all installed apps                                                                                                                                                              |
| `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | "Use my current location" for the sky info. Asked for only when you tap the button, used for a single position fix, never in the background. Not needed if you type the coordinates. |
| `PACKAGE_USAGE_STATS`                            | Sorting apps by Most Recent or Most Used                                                                                                                                             |
| `WRITE_SETTINGS`                                 | Double-tap brightness toggle                                                                                                                                                         |
| `REQUEST_DELETE_PACKAGES`                        | Uninstall apps from the context menu                                                                                                                                                 |
| `BIND_NOTIFICATION_LISTENER_SERVICE`             | Read notifications for badge counts                                                                                                                                                  |
| `READ_CALL_LOG`, `READ_SMS`                      | Missed call and unread SMS badges for Mudita Phone and Messages (Mudita Kompakt only)                                                                                                |
| `INTERNET`                                       | Check for Updates: asks GitHub whether the original Katapult has a new release                                                                                                       |
| `BIND_ACCESSIBILITY_SERVICE`                     | "Almanac Action Service", for the optional lock screen widgets. It reads only the lock screen itself, never your apps. Keep it off if you use BankID or similar apps (see above).    |

## Credits and license

Almanac is built on [Katapult](https://github.com/gezimos/Katapult) by [gezimos](https://github.com/gezimos). The launcher itself, its design and almost all of its code are their work.

**Support the original developer.** If you enjoy Almanac, most of what you enjoy is Katapult. Buy Gezimos a coffee here:

<a href="https://buymeacoffee.com/gezimos"><img src="docs/img/bmc-button.png" alt="Buy gezimos a coffee" height="40"></a>
<br>
(the link goes to gezimos)

Like Katapult, Almanac is licensed under the [GPL-3.0](LICENSE).
