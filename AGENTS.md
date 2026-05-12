<shared-agent-memory>

# READ FIRST — Shared agent memory

This repository is one of three sibling repos (`kepler-laravel`, `kepler-launcher`, `kepler-server`) sharing a single project-context store. Any agent — Claude Code, Codex (CLI or ChatGPT), Gemini CLI, Cursor — should read the shared memory before responding so you inherit decisions, fixes, and runbooks captured by previous sessions of any tool.

```bash
# Run this once at session start:
ls   ~/.config/habbop/kepler-memory/
cat  ~/.config/habbop/kepler-memory/MEMORY.md         # one-line index of every topic file
# Then read the topic files relevant to the task at hand.
```

`MEMORY.md` is the index. Topic files use markdown with simple frontmatter:

```yaml
---
name: short-kebab-slug
description: one-line summary used to decide relevance
type: project | feedback | reference | user
---
body…
```

When you learn something durable (a project decision, a recurring failure mode, a workflow pattern, a credential location, an API quirk), append a new topic file in the same directory and add a one-line link in `MEMORY.md`. Future sessions of every agent will see it.

The canonical store lives at `~/.config/habbop/kepler-memory/` (a symlink into Claude Code's auto-memory dir under `~/.claude/projects/.../memory/` — the path is the same files, just two names for it). Do **not** commit those files to Git: they intentionally hold operational details that should stay local. If multi-machine sync is needed later, point that directory at a private Git clone.

CLAUDE.md and GEMINI.md in this repo are symlinks to AGENTS.md so all three tool conventions read the same content. Edit AGENTS.md; the others follow automatically.

</shared-agent-memory>

# HabboP Kepler Server Context

Read this before changing server behavior for HabboP production.

Key points:

- Production runs this repo as Docker image `kepler-server:dc4543c` in service `test_kepler-server` on `easy.convo.chat`.
- Upstream is `https://github.com/Quackster/Kepler`; origin is `https://github.com/RenatoAscencio/Kepler.git`.
- Do not revert HabboP-specific server changes when pulling upstream. Compare carefully and keep local behavior.
- Production services: `test_kepler-server`, `test_kepler-db`, `test_kepler-www`.
- Game ports: game `12321`, RCON `12309`, MUS camera `12322`.
- Launcher/CMS source of truth is Laravel in `../kepler-laravel`; launcher source/builds are in `../kepler-launcher/launcher/` (renamed from `kepler-cms` on May 12 2026).

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
