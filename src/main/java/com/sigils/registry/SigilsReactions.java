package com.sigils.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import com.sigils.core.reaction.ReactionRule;

/** Loads all datapack reaction rules into pure-core {@link ReactionRule}s. */
public final class SigilsReactions {

    private SigilsReactions() {}

    public static List<ReactionRule> load(RegistryAccess access) {
        Registry<ReactionRuleDefinition> registry = access.lookupOrThrow(SigilsRegistries.REACTION);
        List<ReactionRule> rules = new ArrayList<>();
        for (Identifier id : registry.keySet()) {
            ReactionRuleDefinition def = registry.getValue(id);
            if (def != null) {
                rules.add(def.toCore(id));
            }
        }
        return rules;
    }
}