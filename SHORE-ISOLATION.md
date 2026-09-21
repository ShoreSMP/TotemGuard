# Shore integration for TotemGuard 3

The `shore-3.0` branch is based on upstream `main` at
`8705835f` and retains its `3.0.0-SNAPSHOT` status. The previous Shore 2.1.2
implementation remains on `shore-2.1.2`.

## Install

Use `build/TotemGuard-Paper-3.0.0-shore.1-SNAPSHOT.jar` on Paper, Folia or Canvas.
Replace the previous TotemGuard JAR, update Staff to the companion v3 publisher
hook fix, and perform a full restart. Do not install two TotemGuard JARs.
The plugin still requires PacketEvents. This upstream baseline compiles against
PacketEvents 2.12.1 and Paper 1.21.11.

V3 archives v2 configuration and the local `db` directory under `old/`. Back up
the existing plugin directory before first startup. Upstream migrates selected
connection and webhook settings. This fork additionally preserves the existing
default punishment command and changes its `%player%` placeholder to `%tg_player%`.
V3 creates new check definitions. Compare per-check overrides with `old/checks.yml`
before enabling production punishments. Old check names, thresholds, delays and
database history are not promised to migrate into equivalent v3 behavior.

For Staff's strongest origin/revision protection, configure:

```yaml
default-punishment: "[BAN] autoban totemguard %tg_player% 1d [TotemGuard] Unfair Advantage"
```

Keep your intended durations and reasons. `/punish %tg_player% cheating` is also
recognized and blocked for isolated players. Staff handles its existing active-ban
deduplication. Raw console commands cannot carry a revision into Staff's own
asynchronous work, which is why `/autoban` is recommended for delayed punishments.
Staff announces its successful autoban. Do not add a second announcement command.
The unmodified upstream default remains in fresh installs without migrated config.

## Preserved isolation behavior

- Staff installs the same reflection-only `integration.StaffIsolationPolicy` API.
  Without installed callbacks, ordinary upstream behavior remains available.
- Flag events, detector counters, inventory mitigation and other cheat blocking
  remain active. This integration never cancels `TGUserFlagEvent`.
- Restricted subjects' local, focused, Redis and Discord flag output is suppressed.
  Accepted evidence remains in TotemGuard's database.
- Automatic-ban eligibility has a separate counter scoped to Staff's admission
  revision. Restricted-period violations cannot become a ban immediately on release.
- Delayed animations, command dispatch, buffered chat and webhook queues retain or
  recheck the original revision. Paper checks again on its command scheduler.
- Mixed command batches retain kicks and unknown corrective actions. Typed `[BAN]`
  commands, known ban labels and Staff `/punish` are fenced. Notification commands
  such as `/anticheatbc` are also fenced.

Already delivered or in-flight network/HTTP messages cannot be recalled. Remote
Redis messages carry a subject UUID, but not the originating Staff revision, so
receivers can suppress currently restricted subjects rather than reconstruct an
entire remote punishment history. Deploy matching forks on alert-producing servers.
Fabric and loader artifacts have not been validated for this Shore integration.

## Build and verification

Use JDK 25 to run the wrapper. Build the deployable Paper artifact and tests with:

```text
gradlew.bat :common:test :platforms:paper:shadowJar
```

Tests cover counter generations, restricted-period flags, command classification,
delayed notification guards, callback teardown and default-command migration.
These are not live Canvas or anticheat-accuracy tests. Staging must verify cheat
blocking, Staff's publisher connection, `/sus`, all alert channels and release behavior.
Use this repository's artifact for future upgrades so upstream downloads do not
replace the Shore policy.
