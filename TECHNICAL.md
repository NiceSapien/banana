# Banana Call Recorder - Technical Documentation

## Overview
Banana is a privacy-focused, fully offline Android call recording app built with Jetpack Compose and Material 3 Expressive design theme. It provides comprehensive call recording capabilities with flexible filtering and automatic management features.

## Architecture

### Core Components

#### 1. Data Layer
- **RecordingsRepository**: Manages recording file storage and retrieval
  - Stores recordings in app's external files directory
  - Filename format: `{timestamp}_{number}_{incoming/outgoing}_{name}.m4a`
  - Provides methods for listing, deleting, and auto-cleanup

- **SettingsRepository**: Manages app settings using DataStore
  - Persists user preferences
  - Provides reactive Flow-based settings access
  - Settings include recording mode, schedule, and auto-delete configuration

#### 2. Model Layer
- **Recording**: Data class representing a call recording
  - Contains metadata: phone number, contact name, timestamp, duration, file path
  
- **RecordingSettings**: Data class for app configuration
  - RecordingMode enum: ALL_CALLS, SPECIFIC_CONTACTS, UNKNOWN_CALLERS, ALL_CONTACTS, UNKNOWN_AND_SPECIFIC
  - Schedule settings: days, start/end time
  - Auto-delete settings: enabled flag, retention days

#### 3. Service Layer
- **CallRecordingService**: Foreground service for recording calls
  - Uses MediaRecorder for audio capture
  - Source: VOICE_COMMUNICATION for best call audio quality
  - Format: MPEG_4 container with AAC encoding
  - Shows persistent notification during recording

- **PhoneStateReceiver**: BroadcastReceiver for call events
  - Monitors phone state changes (RINGING, OFFHOOK, IDLE)
  - Monitors outgoing calls
  - Applies filtering logic based on settings
  - Triggers recording service when conditions are met

#### 4. UI Layer
- **MainActivity**: Single activity using Jetpack Compose
  - Handles permission requests
  - Manages navigation between screens
  - Coordinates recording playback

- **BananaTheme**: Material 3 Expressive theme
  - Vibrant banana-inspired color scheme (yellow/orange)
  - Custom color palette with primary, secondary, tertiary colors
  - Expressive typography scale

- **RecordingsScreen**: Main screen showing recorded calls
  - LazyColumn for efficient list rendering
  - Shows contact name, phone number, timestamp
  - Play and delete actions for each recording
  - Empty state when no recordings

- **SettingsScreen**: Configuration screen
  - Recording mode selection (radio buttons)
  - Schedule configuration with day chips and time selection
  - Auto-delete toggle with day counter

#### 5. Utility Classes
- **PermissionUtils**: Handles runtime permission requests
  - Lists all required permissions
  - Adapts permission list based on Android version

- **ContactUtils**: Contact lookup utilities
  - Queries ContactsContract for caller identification
  - Determines if number is saved in contacts

### Background Tasks
- **AutoDeleteWorker**: WorkManager periodic worker
  - Runs daily to clean up old recordings
  - Respects auto-delete settings
  - Low battery constraint to preserve battery

## Features Implementation

### Recording Modes

1. **Record All Calls**
   - Default mode
   - Records every incoming and outgoing call
   - No filtering applied

2. **Record from Specific Contacts**
   - Only records calls from saved contacts
   - Contact identification via ContactsContract
   - Future enhancement: Allow selecting specific contacts

3. **Record from Unknown Callers**
   - Records only calls from numbers not in contacts
   - Useful for screening unknown callers

4. **Record from All Contacts**
   - Records any call from a saved contact
   - Excludes unknown numbers

5. **Record from Unknown & Specific Contacts**
   - Combination mode
   - Records unknown callers and selected contacts
   - Maximum flexibility

### Smart Scheduling

- **Day Selection**: Choose which days recording is active
  - Monday through Sunday selection
  - Visual chip-based UI

- **Time Range**: Set active hours for recording
  - Start and end time selection
  - Only records during specified window
  - 24-hour format

### Auto-Delete

- **Configurable Retention**: Delete recordings after X days
- **Automatic Cleanup**: Daily background job
- **Manual Control**: Enable/disable and adjust retention period
- **Storage Management**: Keeps storage usage under control

## Permissions

### Required Permissions
- `READ_PHONE_STATE`: Detect call state changes
- `READ_CALL_LOG`: Access call information
- `PROCESS_OUTGOING_CALLS`: Detect outgoing calls (deprecated in API 29+)
- `RECORD_AUDIO`: Capture call audio
- `READ_CONTACTS`: Identify callers
- `WRITE_EXTERNAL_STORAGE`: Save recordings (API ≤28)
- `READ_EXTERNAL_STORAGE`: Access recordings (API ≤32)
- `MANAGE_EXTERNAL_STORAGE`: Save recordings (API ≥30)
- `FOREGROUND_SERVICE`: Run recording service
- `FOREGROUND_SERVICE_MICROPHONE`: Microphone foreground service (API ≥34)
- `POST_NOTIFICATIONS`: Show recording notifications (API ≥33)

### Privacy
- **No Internet Permission**: App is completely offline
- **Local Storage Only**: All data stays on device
- **No Analytics**: No usage tracking or data collection
- **Full User Control**: User owns and controls all data

## Technical Specifications

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3 Expressive
- **Storage**: DataStore (preferences), File System (recordings)
- **Background Work**: WorkManager
- **Architecture**: MVVM-like with repository pattern
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

### Build Configuration
- **Gradle**: 7.6.3
- **AGP (Android Gradle Plugin)**: 7.4.2
- **Kotlin**: 1.8.10
- **Compose BOM**: 2023.10.01
- **Material 3**: 1.2.0-beta01

*Note: These versions are used in the current build configuration and are stable. Newer versions may be available at the time you're reading this documentation.*

### File Structure
```
app/src/main/
├── AndroidManifest.xml
├── java/com/banana/recorder/
│   ├── BananaApplication.kt          # Application class + AutoDeleteWorker
│   ├── MainActivity.kt               # Single activity
│   ├── data/
│   │   ├── RecordingsRepository.kt   # Recording file management
│   │   └── SettingsRepository.kt     # Settings persistence
│   ├── model/
│   │   ├── Recording.kt              # Recording data model
│   │   └── RecordingSettings.kt      # Settings data model
│   ├── receiver/
│   │   └── PhoneStateReceiver.kt     # Call event receiver
│   ├── service/
│   │   └── CallRecordingService.kt   # Recording service
│   ├── ui/
│   │   ├── screen/
│   │   │   ├── RecordingsScreen.kt   # Main screen
│   │   │   └── SettingsScreen.kt     # Settings screen
│   │   └── theme/
│   │       ├── Color.kt              # Color definitions
│   │       ├── Theme.kt              # Theme configuration
│   │       └── Type.kt               # Typography
│   └── util/
│       ├── ContactUtils.kt           # Contact helpers
│       └── PermissionUtils.kt        # Permission helpers
└── res/
    ├── values/
    │   ├── strings.xml
    │   └── themes.xml
    └── xml/
        └── file_paths.xml
```

Note: The `AutoDeleteWorker` class is implemented as an inner class in `BananaApplication.kt` for simplicity.

## Known Limitations

1. **Device Compatibility**: Call recording may not work on all devices due to:
   - Manufacturer restrictions
   - Android security policies
   - Hardware limitations

2. **Audio Quality**: Quality varies based on:
   - Device audio routing
   - Call type (cellular vs VoIP)
   - Android version

3. **VoIP Calls**: May not capture audio from:
   - WhatsApp calls
   - Telegram calls
   - Other VoIP apps
   - Due to audio routing restrictions

4. **Legal Compliance**: Users must:
   - Check local laws before using
   - Inform call participants when required
   - Comply with recording consent laws

## Future Enhancements

- Contact selection UI for specific contact mode
- Recording quality settings
- Cloud backup option (optional)
- Recording search and filtering
- Export to different formats
- Transcription support
- Encrypted storage option
- Widget for quick access

## Building and Testing

The app builds through GitHub Actions with full internet access. Local builds require:
- Java 17
- Android SDK
- Internet access to Google Maven repository

GitHub Actions workflow automatically:
- Builds debug and release APKs on every push
- Uploads artifacts for download
- Creates releases for version tags

## Support and Contribution

For issues, feature requests, or contributions, please use the GitHub repository's issue tracker and pull request system.
