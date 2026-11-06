# MLBB Auto Retri (standalone)

Standalone Android APK: reads `mlbb_retri_state.txt` written by **MLBB ESP Pure**, writes `mlbb_retri_config.txt` (tap position + target flags), and performs the spell tap via **Accessibility** when `trigger=1`.

## Shared folder

Default: `Download/MlbbEspRetri/` (same as ESP Pure). If ESP Pure runs in a VM and paths differ, open this app and set **optional shared folder** to match ESP Pure’s “Auto Retri data dir”.

Files:

- `mlbb_retri_state.txt` — produced by ESP Pure
- `mlbb_retri_config.txt` — produced by this app (Save in UI)

## Build

### On GitHub (Actions)

Push to `main` → **Actions** tab → latest **Build APK** run → download artifact **`mlbb-auto-retri-debug`** (contains the APK).

### Local

Requirements: **JDK 17+**, Android SDK (AGP will download components if missing).

```bash
cd mlbb-auto-retri
chmod +x gradlew
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Release:

```bash
./gradlew :app:assembleRelease
```

## New GitHub repo

```bash
cd mlbb-auto-retri
git init
git add .
git commit -m "Initial MLBB Auto Retri standalone"
git branch -M main
git remote add origin https://github.com/YOUR_USER/mlbb-auto-retri.git
git push -u origin main
```

## Usage

1. Install ESP Pure on the reader device; install **this APK** on the device where you can enable Accessibility (often the host phone).
2. Enable **MLBB Auto Retri (tap)** in Settings → Accessibility.
3. Set `tap_x` / `tap_y` and targets; tap **Save config**.
4. Tap **Start watch service** and keep MLBB + ESP Pure running so the state file updates.
