package com.sigils.circuit;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sigils.Sigils;

/**
 * Every trigger the game knows about, by id.
 *
 * <p>Code and not a datapack registry, for the one reason that matters: a trigger
 * is behaviour, and behaviour is not JSON. What <em>is</em> open is the set — any
 * mod can call {@link #register} and its trigger appears in the cycle, the
 * command and every sigil's tooltip with no change here.
 *
 * <p>If this ever needs to be a real registry (for tags, or for a datapack to
 * restrict which triggers a pen tier may use), it becomes one behind these same
 * three methods and no caller changes.
 */
public final class Circuits {

    public static final Identifier REDSTONE = Sigils.id("redstone");
    public static final Identifier TREAD = Sigils.id("tread");
    public static final Identifier TIMER = Sigils.id("timer");
    public static final Identifier CLOSURE = Sigils.id("closure");

    /** What a freshly placed sigil uses until someone changes it. */
    public static final Identifier DEFAULT = REDSTONE;

    private static final Map<Identifier, CircuitCompletion> BY_ID = new HashMap<>();
    private static List<Identifier> sorted = List.of();

    private Circuits() {}

    /** Call once from the mod constructor. */
    public static void bootstrap() {
        register(REDSTONE, new RedstoneCompletion());
        register(TREAD, new TreadCompletion());
        register(TIMER, new TimerCompletion(80));
        register(CLOSURE, new ClosureCompletion());
        Sigils.LOGGER.info("Sigils: {} circuit trigger(s) registered", BY_ID.size());
    }

    public static synchronized void register(Identifier id, CircuitCompletion completion) {
        if (BY_ID.putIfAbsent(id, completion) != null) {
            Sigils.LOGGER.warn("Circuit trigger {} is already registered — ignoring the second", id);
            return;
        }
        // Sorted, so the cycle order in Part B is stable across launches and does
        // not depend on which mod registered first.
        List<Identifier> ids = new ArrayList<>(BY_ID.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));
        sorted = List.copyOf(ids);
    }

    /**
     * The trigger for an id, or {@link CircuitCompletion#NEVER} for one nobody
     * registered.
     *
     * <p>Never null and never throws. A sigil placed with a trigger from a mod
     * that has since been uninstalled goes quiet; it does not crash a chunk load,
     * and re-installing the mod brings it back exactly as it was.
     */
    public static CircuitCompletion get(Identifier id) {
        return BY_ID.getOrDefault(id, CircuitCompletion.NEVER);
    }

    public static boolean isKnown(Identifier id) {
        return BY_ID.containsKey(id);
    }

    /** Every registered id, in a stable order. */
    public static List<Identifier> ids() {
        return sorted;
    }

    /** The next trigger in the cycle. Part B's right-click. */
    public static Identifier next(Identifier current) {
        if (sorted.isEmpty()) {
            return DEFAULT;
        }
        int index = sorted.indexOf(current);
        return sorted.get(Math.floorMod(index + 1, sorted.size()));
    }

    /**
     * Lang key for a trigger's name: {@code sigils:redstone} becomes
     * {@code circuit.sigils.redstone}. The {@code .hint} suffix is the sentence
     * explaining what closes it.
     */
    public static String descriptionKey(Identifier id) {
        return "circuit." + id.getNamespace() + "." + id.getPath();
    }
}