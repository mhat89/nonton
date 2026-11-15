# Nonton - Indonesian Streaming App

A modern Android streaming application built with Kotlin, Jetpack Compose, and ExoPlayer. Supports phones, tablets, and Android TV.

## Features

- 🎬 **Multi-platform**: Phone, Tablet, Android TV
- 📱 **Modern UI**: Jetpack Compose with Material3
- 🎥 **Video Playback**: ExoPlayer with HLS/DASH support
- 🏗️ **Architecture**: MVVM + Clean Architecture + Hilt DI
- 🎨 **Theming**: Dark mode, TV 10ft UI
- 📦 **Offline**: Download support with WorkManager
- 🔍 **Search**: Multi-type search (movies, series, anime, drakor)
- 🌐 **Localization**: Indonesian primary, English support

## Tech Stack

- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Repository + UseCase
- **DI**: Hilt
- **Database**: Room
- **Networking**: OkHttp + Jsoup provider scrapers
- **Video**: ExoPlayer (Media3)
- **Images**: Coil
- **Async**: Coroutines + Flow
- **Serialization**: Kotlinx Serialization
- **Build**: Gradle 8.7 + Kotlin DSL

## Project Structure

```
nonton/
├── app/
│   ├── src/
│   │   ├── main/                 # Shared code
│   │   ├── phone/                # Phone-specific
│   │   ├── tablet/               # Tablet-specific
│   │   └── tv/                   # Android TV-specific
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Getting Started

### 1. Clone the repository
```bash
git clone <repository-url>
cd nonton
```

### 2. Open in Android Studio
- File > Open > Select the `nonton` folder
- Wait for Gradle sync to complete

### 3. Configure a provider
Nonton does not use a private backend. A fresh installation includes an active **Internet Archive Public Domain** provider, so the home screen can load real metadata and progressive MP4 playback without an API key.

You can add another provider from **Settings → Provider Management** by entering either:

- a provider site `Base URL` that matches the bundled HTML scraper selectors, or
- a URL to a JSON provider configuration.

Use the sync button after adding a JSON link. The app reports HTTP/JSON errors per source and reloads both the active provider and visible content after synchronization. If no URL is configured, sync is reported as an informational state rather than a network failure.

Provider configurations are bundled in `app/src/main/assets/providers/index.json`. The JSON provider format supports:

- `responseType`: `HTML` or `JSON`
- `selectors` and `endpoints` for HTML scrapers
- `json` dot-path mappings for list, search, detail, poster, and playable-file fields
- `videoFileUrlTemplate` templates supporting `{id}` and `{filename}`

The disabled template in `app/src/main/assets/providers/index.json` documents the complete schema.

### 4. Run the app
- Select `app` configuration
- Choose device: Phone, Tablet, or TV emulator
- Click Run ▶️

## Build Variants

| Variant | Description |
|---------|-------------|
| `phoneDebug` | Phone debug build |
| `phoneRelease` | Phone release build |
| `tabletDebug` | Tablet debug build |
| `tabletRelease` | Tablet release build |
| `tvDebug` | Android TV debug build |
| `tvRelease` | Android TV release build |

## Project Structure Details

### Core Modules

| Module | Purpose |
|--------|---------|
| `data` | Repository implementations, API services, models |
| `domain` | Use cases, business logic |
| `ui` | Compose screens, ViewModels, navigation |
| `di` | Hilt modules for dependency injection |
| `util` | Constants, extensions, utilities |

### Key Classes

| Class | Purpose |
|-------|---------|
| `NontonApplication` | Hilt application entry point |
| `MainViewModel` | Home screen state management |
| `MediaRepository` | Data layer abstraction |
| `ProviderDownloadManager` | Provider JSON loading and synchronization |
| `MediaRepositoryImpl` | Repository implementation |
| `PlayerScreen` / Media3 | Video playback |

### Build Flavors

```kotlin
// app/build.gradle.kts
flavorDimensions "device"
productFlavors {
    phone { dimension "device" }
    tablet { dimension "device" }
    tv { dimension "device" }
}
```

## TV Development

### Testing on Android TV Emulator

1. Create TV emulator in AVD Manager:
   - Device: Android TV (1080p)
   - System Image: API 34 (Android TV)

2. Run TV variant:
   ```bash
   ./gradlew installTvDebug
   ```

### TV UI Guidelines

- **10ft UI**: Minimum 32sp text, 48dp touch targets
- **Navigation**: D-pad focus navigation
- **Focus**: Visible focus indicators
- **Overscan**: 48dp safe margins
- **Text**: Minimum 20sp for body, 32sp for headlines

## Dependencies

### Core
- AndroidX Core, Activity, Lifecycle
- Hilt (DI)
- Room (Database)
- WorkManager (Background tasks)

### UI
- Jetpack Compose (Material3)
- Navigation Compose
- Coil (Images)
- AndroidX Activity & Lifecycle

### Media
- ExoPlayer (Media3)
- Media3 Session (Notifications, Cast)
- Leanback (Legacy TV)

### Network
- OkHttp
- Jsoup
- Kotlinx Serialization

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## TV Permissions

```xml
<uses-feature android:name="android.software.leanback" android:required="true" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />
```

## ProGuard

Release builds use ProGuard with rules in `app/proguard-rules.pro`:
- Keeps model classes for serialization
- Preserves ExoPlayer, Coil, Retrofit classes
- Keeps Hilt generated code

## CI/CD

### GitHub Actions Example

```yaml
# .github/workflows/android.yml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build Debug
        run: ./gradlew assembleDebug
      - name: Run Tests
        run: ./gradlew test
```

## License

```
MIT License

Copyright (c) 2024 Nonton App

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
2. Commit changes (`git commit -m 'Add amazing feature'`)
3. Push to branch (`git push origin feature/amazing-feature`)
4. Open Pull Request

## Support

- Email: support@nonton.app
- Issues: GitHub Issues
- Documentation: [Wiki](https://github.com/your-repo/wiki)

---

**Nonton** - Streaming favoritmu, kapan saja, di mana saja 🍿