package com.sigils.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.sigils.Sigils;

/** Menu types. */
public final class SigilsMenus {

    private SigilsMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Sigils.MOD_ID);

    public static final Supplier<MenuType<DraftingTableMenu>> DRAFTING_TABLE =
            MENUS.register("drafting_table", () ->
                    new MenuType<>(DraftingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}