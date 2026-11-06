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
3. **Set anchor (overlay)**: open MLBB, tap **Set anchor (overlay on MLBB)** — allow *Display over other apps*. You get a **small dark menu** (live center coords, **SAVE**, **CLOSE**) and an **orange dot**. Drag the dot to the spell, tap **SAVE**. If public Download is not writable on your device, the app falls back to app storage and remembers that folder for the watch service.
4. Tap **Start watch service** and keep MLBB + ESP Pure running so the state file updates.

**No auto click but state file updates?**

- Taps use **`tap_x` / `tap_y` from `mlbb_retri_config.txt`** (Set anchor / Save). The state file’s tap_* can lag until ESP reloads config — do not rely on state for tap position on this device.
- Use **Test tap** after enabling Accessibility: if that fails, fix a11y / battery restrictions first.
- Watch uses **`trigger` rising edge** (0 → 1): one tap per objective window; if the service starts while the file already shows `trigger=1`, force `trigger=0` once (leave range) or restart watch after ESP writes `trigger=0`.
- **Accessibility** must show the service **On**; check `adb logcat -s MlbbAutoRetriWatch RetriAccessibilityService` for `tap ok` vs warnings.
- **Same folder** as ESP Pure: set optional path in the app if virtual vs host paths differ (config + state must live in the **same** dir the watch reads).

**Debug logs (save / path / watch):**

```text
adb logcat -s MlbbAutoRetriFile MlbbAutoRetriStorage MlbbAutoRetriPrefs MlbbAutoRetriWatch RetriAccessibilityService
```

- `MlbbAutoRetriFile` — config read/write, rename vs copy
- `MlbbAutoRetriStorage` — which folder was writable
- `MlbbAutoRetriPrefs` — `last_write_dir`, missing `mlbb_retri_state.txt` warning
- `MlbbAutoRetriWatch` — no state file path, cooldown, tap ok/fail
