package com.deathmotion.totemguard.integration;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Optional public output-stage extension. It never changes detector or setback state. */
public final class StaffIsolationPolicy {
    private static volatile Hooks hooks;
    private record Hooks(Object owner, Predicate<UUID> suppress, ToLongFunction<UUID> revision,
                         BiPredicate<UUID, Long> allowBan, Function<Object[], Boolean> dispatch) { }
    private StaffIsolationPolicy() { }

    public static synchronized void install(Object owner, Predicate<UUID> suppress,
                                            ToLongFunction<UUID> revision, BiPredicate<UUID, Long> allowBan,
                                            Function<Object[], Boolean> dispatch) {
        hooks = new Hooks(owner, suppress, revision, allowBan, dispatch);
    }

    public static synchronized void clear(Object owner) {
        if (hooks != null && hooks.owner == owner) hooks = null;
    }

    public static boolean suppressed(UUID uuid) {
        Hooks current = hooks;
        if (current == null) return false;
        try { return current.suppress.test(uuid); } catch (RuntimeException failure) { return true; }
    }

    public static long revision(UUID uuid) {
        Hooks current = hooks;
        if (current == null) return 0L;
        try { return current.revision.applyAsLong(uuid); } catch (RuntimeException failure) { return Long.MIN_VALUE; }
    }

    public static boolean allowsBan(UUID uuid, long revision) {
        Hooks current = hooks;
        if (current == null) return true;
        try { return current.allowBan.test(uuid, revision); } catch (RuntimeException failure) { return false; }
    }

    public static boolean dispatchAutoban(UUID uuid, String command, long revision) {
        if (!label(command).equals("autoban")) return false;
        Hooks current = hooks;
        if (current == null) return false;
        try { return current.dispatch.apply(new Object[]{uuid, command, revision}); }
        catch (RuntimeException failure) { return true; } // Never fall back to a fresh unversioned request.
    }

    public static boolean isBan(String command) {
        return Set.of("autoban", "ban", "tempban", "banip", "ipban", "ban-ip", "tempipban").contains(label(command));
    }

    public static boolean isNotification(String command) {
        return Set.of("[alert]", "[proxy]", "[log]", "[webhook]").contains(command.trim().toLowerCase(Locale.ROOT))
                || label(command).equals("anticheatbc");
    }

    public static boolean allowsCommand(UUID uuid, String command, long revision, boolean banEligible) {
        if (isBan(command)) return banEligible && allowsBan(uuid, revision);
        return !isNotification(command) || !suppressed(uuid);
    }

    private static String label(String command) {
        String trimmed = command.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        String label = trimmed.split("\\s+", 2)[0];
        return label.substring(label.indexOf(':') + 1);
    }
}
