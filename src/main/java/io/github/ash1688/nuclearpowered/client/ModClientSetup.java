package io.github.ash1688.nuclearpowered.client;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.client.screen.CrusherScreen;
import io.github.ash1688.nuclearpowered.client.screen.ElectricFurnaceScreen;
import io.github.ash1688.nuclearpowered.client.screen.PileScreen;
import io.github.ash1688.nuclearpowered.client.screen.ThermocoupleScreen;
import io.github.ash1688.nuclearpowered.client.screen.WasherScreen;
import io.github.ash1688.nuclearpowered.init.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = NuclearPowered.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientSetup {
    private ModClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.CRUSHER.get(), CrusherScreen::new);
            MenuScreens.register(ModMenuTypes.WASHER.get(), WasherScreen::new);
            MenuScreens.register(ModMenuTypes.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.GRAPHITE_PILE.get(), PileScreen::new);
            MenuScreens.register(ModMenuTypes.THERMOCOUPLE.get(), ThermocoupleScreen::new);
        });
    }
}
