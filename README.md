# Gemini CLI Mobile (Android)

Application Android qui expose **tout le backend Gemini CLI** via un bridge HTTP, avec authentification account/API, exécution de commandes CLI, et exécution shell explicite (`/shell`, `/exec`).

## Fonctionnalités implémentées

- Interface mobile type assistant code (Compose).
- Menu slash commands (40 commandes) + entrée libre.
- Login account `/api` côté bridge (`/v1/account/login`).
- Status session (`/v1/account/status`), récupération des commandes (`/v1/cli/commands`).
- Exécution Gemini CLI réelle (`npm run gemini -- <input>`).
- Exécution shell réelle via `/shell ...` ou `/exec ...`.
- Gestion du `cwd` (working directory) pour agir sur les fichiers comme dans un terminal.

## Backend bridge

```bash
./tools/fetch_gemini_cli.sh
python3 tools/gemini_cli_bridge.py
```

Bridge par défaut : `http://0.0.0.0:8765`.

## Build APK signé local

```bash
./tools/build_signed_apk.sh
```

Variables supportées :
- `GEMINI_KEYSTORE_PASSWORD`
- `GEMINI_KEY_ALIAS`
- `GEMINI_KEY_PASSWORD`

## GitHub Actions

Le workflow `.github/workflows/android-build.yml` build un APK release signé et l’upload en artifact.
Pour une signature personnalisée, configure ces secrets repo :
- `GEMINI_KEYSTORE_PASSWORD`
- `GEMINI_KEY_ALIAS`
- `GEMINI_KEY_PASSWORD`
