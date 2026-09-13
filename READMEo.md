# BloomScrolling 🌱

**A pause between the tap and the scroll.**

BloomScrolling is an Android app built for **HackWesTX '26** ("Beyond the Feed") that interrupts doomscrolling in the moment it starts. Instead of blocking apps outright, it detects when you open a habit app like Instagram or TikTok, asks you a quick question about *why* you're opening it, and — if you keep scrolling past your own limit — applies a gentle, escalating friction: a countdown delay, then a warm color filter that deepens the longer you stay. A community leaderboard and a weekly "time saved" ring turn cutting back into something you can see progress on, instead of just a rule you broke.

---

## Table of contents

- [What it does](#what-it-does)
- [How it works](#how-it-works)
- [Tech stack](#tech-stack)
- [Run it yourself](#run-it-yourself)
- [Repository structure](#repository-structure)
- [Team](#team)
- [License](#license)

## What it does

- **Survey on open** — the moment you open a monitored app, a quick "why are you opening this?" prompt appears (Bored / Habit / Specific task / Messaging someone), logged with a timestamp so you can see your own patterns later.
- **Escalating delay** — cross your daily time limit for that app and a full-screen countdown appears before you can continue; each subsequent trigger extends the wait.
- **Warm color filter** — stay in the app past the delay and a subtle amber overlay activates, growing warmer the longer you keep scrolling.
- **Home dashboard** — a progress ring showing % of time saved this week, plus your day streak and hours saved this week/year.
- **Community** — a leaderboard comparing time saved against friends, to make cutting back a little bit social.
- **App selection** — choose exactly which apps BloomScrolling watches; everything else on your phone is left alone.

## How it works

```
AccessibilityService detects foreground app
        │
        ▼
   On the blocklist? ──no──▶ (nothing happens)
        │ yes
        ▼
   Survey due? ──yes──▶ Survey overlay ──▶ log response
        │
        ▼
  Usage timer starts / updates
        │
        ▼
  Daily limit crossed? ──yes──▶ Delay screen (countdown escalates each time)
        │                              │
        no                              ▼
        │                     Extended use ──▶ color filter ramps
        ▼
      (loop)
```

An `AccessibilityService` is the single trigger point: it watches for foreground-app changes, checks the open app against a user-configured blocklist stored in Room, and kicks off the survey → timer → delay → color-filter chain from there. `UsageStatsManager` cross-checks elapsed time, since its own reporting has a few minutes of lag on some OEMs.

## Tech stack

- **Kotlin** + **Jetpack Compose** (UI, no XML layouts) + **Compose Navigation**
- **Room** — local persistence (survey responses, per-app usage logs, per-app settings)
- **Hilt** — dependency injection
- **DataStore** — user preferences (limits, survey frequency, filter intensity)
- **WorkManager** — scheduled/background work (weekly summaries, streak checks)
- **AccessibilityService** + **UsageStatsManager** + **WindowManager** overlays — the detection/delay/color-filter core
- **Vico** — the weekly trend chart
- minSdk 26, target/compile SDK 34+

## Run it yourself

1. **Clone the repo**
   ```bash
   git clone https://github.com/Pxwer8/bloom-scrolling.git
   cd bloom-scrolling
   ```
2. **Open in Android Studio** (current stable release) and let Gradle sync — Room/Hilt/KSP dependencies resolve automatically.
3. **Run** on a device or emulator running **API 26+** (a Pixel device/emulator is recommended — some OEM skins, e.g. Xiaomi/Samsung, apply aggressive background-app killing that can interfere with the accessibility service).
4. **Grant permissions** the first time the app opens — it walks you through three, each of which requires leaving the app once: Usage Access, Display over other apps, and Accessibility.
5. **Pick apps to monitor** from the app-selection screen (Instagram/TikTok are good demo choices).
6. Open a monitored app to see the full flow: survey → (keep scrolling) → delay countdown → color filter.

> For a live demo/judging pass, daily limits and survey frequency can be turned down (e.g. a 30-second limit, survey on every open) in Settings so the flow is visible in under a minute instead of over a real day.

## Repository structure

```
bloom-scrolling/
├── app/
│   └── src/main/java/.../
│       ├── service/     # AccessibilityService, usage-timer foreground service
│       ├── ui/          # Compose screens: onboarding, survey, delay, home, community, settings
│       ├── data/        # Room entities/DAOs, AppDatabase
│       ├── overlay/     # WindowManager color-filter view
│       └── di/          # Hilt modules
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## Team

Built in 24 hours at HackWesTX '26 (Texas Tech Innovation Hub, Lubbock, TX) by Sam, Hugo, Amaral, and Calebe.

## License

MIT — see [LICENSE](LICENSE).
