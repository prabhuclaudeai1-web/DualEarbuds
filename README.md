# Dual Earbuds Connect — Android App

## What this app does
- Saves 2 ENC earbuds (any brand) by name & MAC address
- One tap to connect both simultaneously
- Enables Dual Bluetooth Audio on your Moto G62 5G
- Shows live connection status for each earbud
- Guides you to Developer Options for Dual Audio setup

---

## Build APK (5 minutes)

1. Install **Android Studio** → https://developer.android.com/studio
2. Open this `DualEarbuds` folder in Android Studio
3. Let Gradle sync (~2 min)
4. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. APK is at: `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer to phone → tap to install

---

## How to use the app on your phone

### Step 1 — Pair both earbuds first
- Go to Android **Settings → Bluetooth**
- Put **Earbud 1** in pairing mode → pair it
- Put **Earbud 2** in pairing mode → pair it
- Both must be paired before the app can connect them

### Step 2 — Enable Dual Audio
- **Settings → About Phone** → tap **Build Number** 7 times
- Go to **Settings → Developer Options**
- Enable **"Disable Bluetooth A2DP hardware offload"**
- OR look for **"Dual Audio"** toggle
- Restart Bluetooth

### Step 3 — Use the app
- Open Dual Earbuds app
- Tap **Scan & Save** for slot 1 → select your first earbud
- Tap **Scan & Save** for slot 2 → select your second earbud
- Tap **⚡ CONNECT BOTH EARBUDS**
- Both earbuds will connect and play audio simultaneously!

---

## Compatibility
- Moto G62 5G (Android 12+) ✓
- Any Android phone with Bluetooth 5.0+ ✓
- Any brand of earbuds: Boat, JBL, Sony, Zebronics, Xiaomi, etc. ✓
