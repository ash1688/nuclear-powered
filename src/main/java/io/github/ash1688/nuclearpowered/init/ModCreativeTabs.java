package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NuclearPowered.MODID);

    public static final RegistryObject<CreativeModeTab> NUCLEAR_TAB = CREATIVE_MODE_TABS.register("nuclear_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + NuclearPowered.MODID))
                    .icon(() -> ModItems.RAW_URANIUM.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        out.accept(ModBlocks.URANIUM_ORE.get());
                        out.accept(ModItems.RAW_URANIUM.get());
                        out.accept(ModBlocks.THORIUM_ORE.get());
                        out.accept(ModItems.RAW_THORIUM.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
