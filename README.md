# 🍌 Banana - Call Recorder

A full-featured, privacy-focused call recording app for Android built with Jetpack Compose and Material 3 Expressive design theme.

## ✨ Features

### Recording Modes
- **Record All Calls**: Automatically record every incoming and outgoing call
- **Record from Specific Contacts**: Only record calls from selected contacts
- **Record from Unknown Callers**: Record calls from numbers not in your contacts
- **Record from All Contacts**: Record calls from any saved contact
- **Record from Unknown & Specific Contacts**: Combination mode for maximum flexibility

### Smart Scheduling
- Schedule recordings for specific days of the week
- Set time ranges for when recordings should be active
- Perfect for work hours or personal time management

### Auto-Delete
- Automatically delete recordings older than a specified number of days
- Keep your storage clean and organized
- Configurable retention period

### Privacy First
- **No internet permission required** - completely offline
- All recordings stored locally on your device
- No data collection or analytics
- Full control over your data

## 🎨 Design

Built with **Material 3 Expressive** design theme featuring:
- Vibrant banana-inspired color scheme (yellow and orange)
- Modern, fluid animations
- Intuitive and accessible interface
- Adaptive layouts for different screen sizes

## 🚀 Getting Started

### Prerequisites
- Android device running Android 8.0 (API 26) or higher
- Microphone, phone, contacts, and storage permissions

### Installation

1. Download the latest APK from the [Releases](../../releases) page
2. Install the APK on your Android device
3. Grant the required permissions when prompted
4. Configure your preferred recording mode in Settings

### Building from Source

```bash
# Clone the repository
git clone https://github.com/NiceSapien/banana.git
cd banana

# Build the APK
./gradlew assembleRelease

# The APK will be generated at:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 📱 Permissions Required

- **READ_PHONE_STATE**: Detect incoming and outgoing calls
- **READ_CALL_LOG**: Access call information
- **RECORD_AUDIO**: Record call audio
- **READ_CONTACTS**: Identify callers and match with recording preferences
- **WRITE_EXTERNAL_STORAGE** (Android 9 and below): Save recordings
- **MANAGE_EXTERNAL_STORAGE** (Android 11+): Save recordings
- **POST_NOTIFICATIONS** (Android 13+): Show recording notifications
- **FOREGROUND_SERVICE**: Keep recording service running

## 🛠️ Technology Stack

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern declarative UI framework
- **Material 3**: Latest Material Design components with Expressive theme
- **DataStore**: Persistent settings storage
- **WorkManager**: Background task scheduling for auto-delete
- **MediaRecorder**: High-quality audio recording

## 📋 Usage

### Recording Calls

1. Open the app and grant all required permissions
2. Configure your recording mode in Settings
3. Make or receive a call - it will be recorded automatically based on your settings
4. Access your recordings from the main screen

### Managing Recordings

- **Play**: Tap the play button to listen to a recording
- **Delete**: Tap the delete button to remove a recording
- **Share**: Long-press a recording to share it with other apps

### Configuring Settings

Navigate to Settings to configure:
- Recording mode
- Recording schedule (days and times)
- Auto-delete settings

## ⚠️ Important Notes

- Call recording laws vary by country and region. Ensure you comply with local laws before using this app.
- Some devices may not support call recording due to hardware or software limitations.
- Recording quality may vary depending on your device and call type (VoIP, cellular, etc.).

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Android Jetpack](https://developer.android.com/jetpack)
- Inspired by privacy-focused design principles
- Material 3 Expressive design guidelines

## 📞 Support

For issues, questions, or suggestions, please open an issue on GitHub.
