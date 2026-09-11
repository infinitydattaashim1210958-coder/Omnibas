# OmniRoute Android

Unlimited AI chat for Android, inspired by [OmniRoute](https://github.com/diegosouzapw/OmniRoute).

One app, four routes — **Auto**, **Code**, **Write**, **Think**. Chats stay on the phone. No account required.

## What you get

- Native Jetpack Compose chat client (Material 3, dark teal)
- **Free unlimited** mode (no API key)
- **Custom gateway** mode for a self-hosted OmniRoute, xAI, OpenRouter, or any OpenAI-compatible `/v1` endpoint
- Streaming replies, markdown, local history, search
- GitHub Actions workflow that builds a debug APK on every push

## Install the APK (GitHub build)

1. Create a GitHub repo and upload this folder (or unzip `repository.zip` and push).
2. Open **Actions → Build APK**.
3. Download the `OmniRoute-debug` artifact and install it on your phone (allow unknown sources).

You can also open the folder in **Android Studio** (Hedgehog / Koala or newer), wait for Gradle sync, and press Run.

## Chat without a key

Leave **Provider** on **Free unlimited**. Messages go through a public OpenAI-compatible text route. Good for everyday chat.

## Plug in OmniRoute

1. Run OmniRoute on a computer or server (`npx -y omniroute`).
2. In the app: **Settings → Custom gateway**.
3. Base URL examples:
   - Local OmniRoute: `http://192.168.x.x:4141/v1`
   - xAI: `https://api.x.ai/v1` + your key, model `grok-4.5`
   - OpenRouter: `https://openrouter.ai/api/v1`
4. Model: `auto` (OmniRoute) or a specific model id.
5. Save, then send a message.

## Project layout

```
app/src/main/java/online/omniroute/chat/
  MainActivity.kt
  data/          models, local store, OpenAI-compatible client
  ui/            Compose screens + theme
.github/workflows/android.yml   GitHub Actions APK build
```

Package: `online.omniroute.chat` · minSdk 26 · targetSdk 35 · Java 17

Inspired by OmniRoute (MIT). This client is a separate mobile app, not a fork of the full gateway.
