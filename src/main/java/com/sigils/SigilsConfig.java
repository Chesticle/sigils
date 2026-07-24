package com.sigils;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Server-owner-facing configuration.
 *
 * <p>Added in Phase 0 on purpose: several later systems (world-modifying
 * effects, sigil scan radius, particle budget) need admin toggles, and
 * retrofitting a config system is more annoying than starting with one.
 */
public final class SigilsConfig {

    public static final ModConfigSpec SPEC;
    public static final SigilsConfig INSTANCE;

    /** Master switch for spells that place or break blocks. */
    public final ModConfigSpec.BooleanValue allowBlockModification;

    /** Hard ceiling on the radius, in blocks, any single spell effect may reach. */
    public final ModConfigSpec.IntValue maxEffectRadius;

    /** Verbose logging of spell compilation and resolution. Noisy; dev use. */
    public final ModConfigSpec.BooleanValue debugLogging;

    private SigilsConfig(ModConfigSpec.Builder builder) {
        builder.comment("Sigils configuration").push("general");

        allowBlockModification = builder
                .comment("If false, spells cannot place or break blocks. Effects that would",
                        "modify terrain are skipped; the rest of the spell still resolves.")
                .define("allowBlockModification", true);

        maxEffectRadius = builder
                .comment("Maximum radius in blocks that any single spell effect may affect.",
                        "Lower this on public servers to bound worst-case griefing and lag.")
                .defineInRange("maxEffectRadius", 16, 1, 128);

        debugLogging = builder
                .comment("Log every spell compilation and elemental resolution. Very noisy.")
                .define("debugLogging", false);

        builder.pop();
    }

    static {
        Pair<SigilsConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(SigilsConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }
}