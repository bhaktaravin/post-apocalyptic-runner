# Firebase Setup Guide

## Overview
Your game now has Firebase integration for tracking scores and leaderboards. Currently running in **mock mode** (offline).

## To Enable Firebase:

### 1. Create a Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter a project name (e.g., "PostApocalypticGame")
4. Follow the setup wizard

### 2. Enable Realtime Database
1. In Firebase Console, go to "Build" → "Realtime Database"
2. Click "Create Database"
3. Choose a location (e.g., us-central1)
4. Start in **test mode** (for development)

### 3. Get Service Account Key
1. Go to Project Settings (gear icon) → "Service accounts"
2. Click "Generate new private key"
3. Download the JSON file
4. Save it as `serviceAccountKey.json` in `src/main/resources/` folder
5. **IMPORTANT**: Add to `.gitignore` to keep it secure!

### 4. Update Firebase Configuration

The Firebase URL has already been configured:
```java
.setDatabaseUrl("https://gamescorees-default-rtdb.firebaseio.com/")
```

The app is now configured to automatically load `serviceAccountKey.json` from the resources folder.

### 5. Security Rules (Production)
In Firebase Console → Realtime Database → Rules, update to:
```json
{
  "rules": {
    "scores": {
      ".read": true,
      ".write": true
    },
    "players": {
      ".read": true,
      ".write": true
    }
  }
}
```

## Current Features (Offline Mode)
- ✓ Main menu with player name input
- ✓ Play game functionality
- ✓ Settings dialog
- ✓ Leaderboard UI (shows "offline" message)

## Features After Firebase Setup
- ✓ Save scores to cloud database
- ✓ Track player statistics (games played, high score)
- ✓ Real-time leaderboard (top 10 scores)
- ✓ Persistent data across sessions
- ✓ Multi-player score comparison

## Data Structure in Firebase
```
your-project/
├── scores/
│   └── {playerId}/
│       └── {scoreId}
│           ├── playerName: "PlayerName"
│           ├── score: 1234
│           ├── survivalTime: 45
│           └── timestamp: 1234567890
└── players/
    └── {playerId}
        ├── name: "PlayerName"
        ├── gamesPlayed: 5
        ├── highScore: 2000
        └── lastPlayed: 1234567890
```

## Testing
Run the game:
```bash
mvn clean javafx:run
```

The game will work in offline mode until you configure Firebase!
