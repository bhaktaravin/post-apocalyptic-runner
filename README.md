# Post-Apocalyptic Runner Game

A JavaFX-based 2D side-scrolling runner game with parallax backgrounds, enemies, and Firebase leaderboard integration.

## Features

- 🎮 Smooth parallax scrolling backgrounds
- 🏃 Player movement with physics-based jumping
- 👾 Enemy spawning system with collision detection
- ❤️ Health system with invulnerability frames
- 📊 Score tracking and Firebase leaderboard
- 🎨 Stylish main menu
- ⚙️ Settings and controls

## Controls

- **A/D** or **Arrow Keys**: Move left/right
- **SPACE/W/↑**: Jump
- **SHIFT**: Toggle auto-scroll
- **ESC**: Return to menu
- **R**: Restart (when game over)

## Requirements

- Java 21 LTS
- Maven 3.x
- JavaFX 21
- Firebase account (optional for leaderboard)

## Setup

1. Clone the repository
2. Install dependencies:
   ```bash
   mvn clean install
   ```

3. (Optional) Configure Firebase:
   - Follow instructions in `FIREBASE_SETUP.md`
   - Add your `serviceAccountKey.json` to `src/main/resources/`

4. Run the game:
   ```bash
   mvn clean javafx:run
   ```

## Game Objective

Survive as long as possible by dodging enemies in the post-apocalyptic wasteland. Each enemy avoided increases your score. Getting hit reduces your health by 20 HP. The game ends when your health reaches zero.

## Technologies

- **JavaFX 21** - UI and game rendering
- **Firebase Realtime Database** - Score tracking and leaderboards
- **Maven** - Build and dependency management

## Assets

Background art from CraftPix (included in resources)

## License

MIT License - Feel free to use and modify!

## Development

Built with Java 21 and JavaFX. The game uses a canvas-based rendering system with an AnimationTimer for the game loop running at ~60 FPS.

---

Made with ❤️ using GitHub Copilot
# post-apocalyptic-runner
