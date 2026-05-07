# HabboP Kepler Server Context

Read this before changing server behavior for HabboP production.

Key points:

- Production runs this repo as Docker image `kepler-server:dc4543c` in service `test_kepler-server` on `easy.convo.chat`.
- Upstream is `https://github.com/Quackster/Kepler`; origin is `https://github.com/RenatoAscencio/Kepler.git`.
- Do not revert HabboP-specific server changes when pulling upstream. Compare carefully and keep local behavior.
- Production services: `test_kepler-server`, `test_kepler-db`, `test_kepler-www`.
- Game ports: game `12321`, RCON `12309`, MUS camera `12322`.
- Launcher/CMS source of truth is Laravel in `../kepler-laravel`; launcher source/builds are in `../kepler-cms/launcher`.

Recent HabboP-specific behavior:

- Avatar and mission editing must work while launcher SSO stays active. Keep the server-side handling in `Kepler-Server/src/main/java/org/alexdev/kepler/messages/incoming/user/UPDATE.java`; do not reintroduce early returns that discard already parsed figure/motto changes.
- The launcher keeps `pIsSsoLogin = TRUE` and injects a temporary `user_password` from the current SSO ticket. The server accepts the active in-memory SSO token for account/profile flows without persisting it again.
- Camera photos require MUS frame/body/prop limits of at least 32 MB. v14 camera `BINDATA` uploads can exceed the old 1 MB MUS limit.
- Production database must keep `items_photos.photo_data` as `LONGBLOB` and MariaDB service `test_kepler-db` must keep `--max-allowed-packet=64M`; real camera payloads exceeded the default 16 MB packet limit.
- Navigator room counts are refreshed from live in-memory room entities, not only the `rooms.visitors_now` DB snapshot.
- RCON game settings can be reloaded without full service restarts; keep that path available for Laravel housekeeping.

Verification:

- Local Mac may not have Java installed. If `./gradlew test` cannot run locally, verify with Docker build: `docker build -t kepler-server:<tag> .`.
- After production changes, verify service convergence with `docker service ps test_kepler-server` and logs with `docker service logs --since 5m test_kepler-server`.
- For camera regressions, check logs for discarded MUS frames or SQL packet errors immediately after taking a photo.
