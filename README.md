# YT Auto Shorts — Final Android Project

## What is included
- Native Android app; no Termux runtime required after APK installation.
- Telegram channel photo sync using Bot API `channel_post`.
- Local 1080x1920 H.264 silent MP4 rendering using Android MediaCodec.
- You.com Research metadata generation with local fallback.
- YouTube upload client using resumable `videos.insert` once an OAuth access token is available.
- Daily alarm scheduling + boot rescheduling + WorkManager foreground worker.
- Manual photo selection, Telegram sync, generate, upload, and settings.
- GitHub Actions APK build.

## Important setup
1. Create a Google Cloud project, enable YouTube Data API v3.
2. Configure OAuth credentials for the Android package:
   `com.divya.ytautoshorts`
   with the SHA-1 of the signing key used to build the APK.
3. Add your Telegram bot as admin in the private channel.
4. Put the bot token and channel username/chat ID in Settings.
5. Put your You.com API key in Settings.
6. For direct YouTube API upload, provide a valid OAuth access token in the app's local `yt_access_token` setting. The demo sign-in button proves Google account connection but does not expose a YouTube bearer token through GoogleSignIn's public API.
7. The app creates silent Shorts. Add YouTube's licensed sound using YouTube's own Shorts editor after upload.

## GitHub build
- Upload this project to GitHub.
- Actions -> Build APK.
- Download the `yt-auto-shorts-debug-apk` artifact.

## Local build
Android Studio or Gradle:
`gradle assembleDebug`
APK:
`app/build/outputs/apk/debug/app-debug.apk`

## Automation limits
Android does not guarantee exact background execution. The app uses alarms + WorkManager and reschedules after boot. Battery optimization, permissions, OEM background policies, network failures, and YouTube quota/OAuth can still prevent an unattended run.

## YouTube limits
YouTube `videos.insert` uploads and metadata. New/unverified API projects can be restricted to private uploads until Google audit. The upload quota is 100 units/day for this method according to current documentation.

## Security
Secrets are stored locally in SharedPreferences in this source build. Before public Play Store distribution, replace this with Android Keystore/EncryptedSharedPreferences and avoid shipping a bot token inside the APK.
