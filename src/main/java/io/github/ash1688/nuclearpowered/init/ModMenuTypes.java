package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.CrusherMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, NuclearPowered.MODID);

    public static final RegistryObject<MenuType<CrusherMenu>> CRUSHER =
            MENU_TYPES.register("crusher", () -> IForgeMenuType.create(CrusherMenu::new));

    private ModMenuTypes() {}

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
