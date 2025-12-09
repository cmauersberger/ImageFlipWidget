# ImageFlipWidget
Simple Widget for Android to show one flippable image

## Project Structure

This is a minimal native Android Studio project built with:
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Features

- Home-screen widget that displays an image
- Tap to toggle between two images (green and blue)
- State persistence using SharedPreferences
- Built with RemoteViews for widget rendering

## Project Files

### Core Widget Files
- `app/src/main/java/com/cmauersberger/imageflipwidget/ImageFlipWidgetProvider.kt` - Main widget provider
- `app/src/main/res/layout/image_flip_widget.xml` - Widget layout
- `app/src/main/res/xml/image_flip_widget_info.xml` - AppWidget provider configuration
- `app/src/main/res/drawable/image_one.xml` - First image (green)
- `app/src/main/res/drawable/image_two.xml` - Second image (blue)

### Configuration Files
- `build.gradle.kts` - Root build configuration
- `app/build.gradle.kts` - App module build configuration
- `settings.gradle.kts` - Project settings
- `gradle.properties` - Gradle properties
- `app/src/main/AndroidManifest.xml` - App manifest with widget declaration

### CI/CD
- `.github/workflows/android-ci.yml` - GitHub Actions workflow for building and testing

## Building

To build the project locally:

```bash
./gradlew assembleDebug
```

To run tests:

```bash
./gradlew test
```

## Widget Installation

1. Build and install the APK on an Android device/emulator
2. Long-press on the home screen
3. Select "Widgets"
4. Find and add "Image Flip Widget"
5. Tap the widget to toggle between images

## How It Works

The widget uses:
- **SharedPreferences** to store the current image state per widget instance
- **RemoteViews** to update the widget UI
- **PendingIntent** to handle tap events on the widget
- **BroadcastReceiver** pattern to receive widget events
