# Shore integration for TotemGuard 2.1.2

This fork is based on upstream tag `v2.1.2` (`225d0050030f80431f7f7730422505f7d776cb14`) and reports `2.1.2-shore.1` to distinguish its artifact. It adds an optional Staff output policy without changing checks, bypass permissions, accepted detector flags or existing prevention.

With compatible Staff isolation enabled, restricted subjects' new local player/console/Discord/Redis alert publication is suppressed. Their accepted alert evidence still goes to TotemGuard's alert repository. The detector violation counter still increments. A separate automatic-ban counter ignores restricted-period flags and starts fresh for each admission/session revision. Flags capture their revision before entering TotemGuard's asynchronous task, so a queued flag from isolation cannot become new ban eligibility after release.

Automatic punishment checks the original revision before scheduling, after a configured delay, and immediately before each command. Mixed command batches retain kicks, corrections and unknown actions. Existing manual staff punishment paths are unaffected. Recognized `/autoban` commands go directly to Staff's origin-aware service with the captured revision; this prevents delayed work from receiving a fresh token at console arrival.

Staff installs the public `integration.StaffIsolationPolicy` callbacks using cached reflection. This plugin has no compile-time Staff dependency. Absent Staff preserves ordinary TotemGuard behavior. Staff's restricted-state and revision reads perform no database work in TotemGuard callbacks. Do not cancel `FlagEvent` or grant `TotemGuard.Bypass` to implement output suppression.

In the existing deployed `checks.yml`, set the default automatic ban command to:

```yaml
default-punishment: "autoban totemguard %player% 1d [TotemGuard] Unfair Advantage"
```

Update per-check raw ban overrides as well, keeping their durations/reasons and existing thresholds. Do not change correction or kick actions. Staff's `/autoban` already announces the first successful punishment, so remove a duplicate `anticheatbc` action from that same automatic chain. Keep the old script unloaded. No check upgrades or live configuration changes are performed by this fork.

The suppression point prevents new alert publication. Already delivered or in-flight legacy proxy messages cannot be retracted; their payload has no subject UUID. Use matching publisher behavior on all alert-producing backends before enabling cross-server alert forwarding. Existing two-component Redis packet format is preserved.

Build with `gradlew.bat build`. The deployable artifact is `build/libs/TotemGuard-2.1.2-shore.1.jar`. Automated tests cover restricted-period counter behavior, release/reconnect generations, queued flags, delayed command guards, mixed corrective commands, callback teardown/failure and direct token forwarding. Live Folia, anticheat prevention and all alert channels still need staging verification against compatible Staff. Upstream's update command downloads upstream artifacts and will not preserve this fork; deploy updates from this repository's build instead.

Production Grim is a separate private/injected v3 product. This TotemGuard integration does not establish Grim v3 compatibility or suppress Grim's private publishers.
