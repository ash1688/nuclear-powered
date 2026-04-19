package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.BatteryMenu;
import io.github.ash1688.nuclearpowered.menu.CoalBoilerMenu;
import io.github.ash1688.nuclearpowered.menu.CrusherMenu;
import io.github.ash1688.nuclearpowered.menu.DissolverMenu;
import io.github.ash1688.nuclearpowered.menu.ElectricFurnaceMenu;
import io.github.ash1688.nuclearpowered.menu.FuelFabricatorMenu;
import io.github.ash1688.nuclearpowered.menu.PileMenu;
import io.github.ash1688.nuclearpowered.menu.ShearerMenu;
import io.github.ash1688.nuclearpowered.menu.SteamEngineMenu;
import io.github.ash1688.nuclearpowered.menu.ThermocoupleMenu;
import io.github.ash1688.nuclearpowered.menu.WasherMenu;
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

    public static final RegistryObject<MenuType<WasherMenu>> WASHER =
            MENU_TYPES.register("washer", () -> IForgeMenuType.create(WasherMenu::new));

    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
            MENU_TYPES.register("electric_furnace", () -> IForgeMenuType.create(ElectricFurnaceMenu::new));

    public static final RegistryObject<MenuType<PileMenu>> GRAPHITE_PILE =
            MENU_TYPES.register("graphite_pile", () -> IForgeMenuType.create(PileMenu::new));

    public static final RegistryObject<MenuType<ThermocoupleMenu>> THERMOCOUPLE =
            MENU_TYPES.register("thermocouple", () -> IForgeMenuType.create(ThermocoupleMenu::new));

    public static final RegistryObject<MenuType<BatteryMenu>> BATTERY =
            MENU_TYPES.register("battery", () -> IForgeMenuType.create(BatteryMenu::new));

    public static final RegistryObject<MenuType<FuelFabricatorMenu>> FUEL_FABRICATOR =
            MENU_TYPES.register("fuel_fabricator", () -> IForgeMenuType.create(FuelFabricatorMenu::new));

    public static final RegistryObject<MenuType<CoalBoilerMenu>> COAL_BOILER =
            MENU_TYPES.register("coal_boiler", () -> IForgeMenuType.create(CoalBoilerMenu::new));

    public static final RegistryObject<MenuType<SteamEngineMenu>> STEAM_ENGINE =
            MENU_TYPES.register("steam_engine", () -> IForgeMenuType.create(SteamEngineMenu::new));

    public static final RegistryObject<MenuType<ShearerMenu>> SHEARER =
            MENU_TYPES.register("shearer", () -> IForgeMenuType.create(ShearerMenu::new));

    public static final RegistryObject<MenuType<DissolverMenu>> DISSOLVER =
            MENU_TYPES.register("dissolver", () -> IForgeMenuType.create(DissolverMenu::new));

    private ModMenuTypes() {}

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
