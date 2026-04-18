# Gemini CLI Mobile (Android)

Application Android qui expose le backend Gemini CLI via un bridge HTTP, avec authentification token, exécution de commandes CLI, exécution shell (`/shell`, `/exec`) et support du `cwd` pour manipuler les fichiers comme dans un terminal.

## Fonctionnalités implémentées

- UI Compose type assistant code.
- Menu slash commands (40 commandes).
- Endpoints bridge :
  - `POST /v1/account/login`
  - `POST /v1/account/status`
  - `POST /v1/cli/commands`
  - `POST /v1/cli/execute`
- Exécution Gemini CLI réelle via `npm run gemini -- <input>`.
- Exécution shell explicite via `/shell ...` ou `/exec ...`.

## Démarrage local

```bash
./tools/fetch_gemini_cli.sh
python3 tools/gemini_cli_bridge.py
```

Puis démarrer l’app Android (émulateur) et pointer l’URL bridge `http://10.0.2.2:8765`.

## Build APK signé local

```bash
./tools/build_signed_apk.sh
```

Variables supportées :
- `GEMINI_KEYSTORE_PASSWORD`
- `GEMINI_KEY_ALIAS`
- `GEMINI_KEY_PASSWORD`

## GitHub Actions (build APK)

Workflow: `.github/workflows/android-build.yml`.

- Build release APK à chaque push/PR.
- Génère un keystore de CI (fallback si secrets absents).
- Upload l’APK (`app-release-apk`) en artifact.

Secrets optionnels recommandés :
- `GEMINI_KEYSTORE_PASSWORD`
- `GEMINI_KEY_ALIAS`
- `GEMINI_KEY_PASSWORD`

## Sécurité

⚠️ Ne jamais publier un token/API key en clair dans un message, commit ou workflow. Si un token a été exposé, il faut le révoquer immédiatement et en générer un nouveau.
