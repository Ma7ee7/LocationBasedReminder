# Place Reminder

A personal Android app that reminds you when you enter a fixed **100 meter zone** around saved latitude/longitude coordinates.

## What it does

- Saves reminders with:
  - reminder text
  - latitude
  - longitude
  - fixed 100 meter radius
- Runs a foreground service when you press **Start**.
- The service mostly sleeps.
- It requests location only at 10-minute marks:
  - `:00`
  - `:10`
  - `:20`
  - `:30`
  - `:40`
  - `:50`
- If your current location is within 100 meters of a saved coordinate, it sends a notification.
- A reminder only fires once until you press **Reset**.

## Why it uses a foreground service

Android background timers are not reliable forever. Even with background location permission, Android can delay or kill background work to save battery.

This app uses a foreground service because it is the most reliable personal-app approach. It shows a small persistent notification while the checker is running.

## Build

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Plug in your phone or use an emulator.
4. Press Run.

## Permissions

The app asks for:

- Fine/coarse location, so it can check whether you are near a saved coordinate.
- Notifications on Android 13+, so it can show reminder alerts.
- Foreground service location permission, so the checker can keep running while the app is not open.

## Coordinates

Use decimal coordinates, like:

- Latitude: `40.712776`
- Longitude: `-74.005974`

In Google Maps, you can long-press a spot and copy the coordinates.
