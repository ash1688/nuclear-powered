package io.github.ash1688.nuclearpowered.block.engine;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.client.ui.NPTabs;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.energy.EnergyMode;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class SteamEngineBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int STEAM_CAPACITY_MB = 4000;
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_OUTPUT_FE_PER_TICK = 512;

    // Conversion: each "batch" consumes STEAM_PER_BATCH mB of steam and
    // produces FE_PER_BATCH FE. The number of batches the engine runs per
    // tick scales with the steam tank's fill level — full tank runs the
    // maximum batches, empty tank runs none. This way more boilers feeding
    // into the engine raises its sustained tank level, which in turn raises
    // the engine's FE/tick output.
    //   1 boiler  = 2 mB/tick supply  -> ~5 % fill equilibrium  -> ~25 FE/tick
    //   5 boilers = 10 mB/tick supply -> ~25 % fill equilibrium -> ~125 FE/tick
    //   20 boilers = 40 mB/tick supply -> ~100 % fill equilibrium -> ~500 FE/tick
    private static final int STEAM_PER_BATCH = 2;
    private static final int FE_PER_BATCH = 25;
    /** Maximum batches per tick. At max (full tank) the engine consumes
     *  20 × 2 = 40 mB and produces 20 × 25 = 500 FE — close to the
     *  {@link #MAX_OUTPUT_FE_PER_TICK} output cap. */
    private static final int MAX_BATCHES_PER_TICK = 20;

    private final FluidTank steamTank = new io.github.ash1688.nuclearpowered.compat.gtceu.SteamTank(STEAM_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int storedFE = 0;

    // Dual-energy mode flag. Engine produces FE → in EU mode it switches to
    // producing/exposing EU. The EU-side cap and push behaviour ship in a
    // follow-up commit; for now EU mode just hoards generated FE.
    private EnergyMode energyMode = EnergyMode.FE;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) { return 0; }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int take = Math.min(storedFE, Math.min(maxExtract, MAX_OUTPUT_FE_PER_TICK));
            if (take <= 0) return 0;
            if (!simulate) {
                storedFE -= take;
                setChanged();
            }
            return take;
        }

        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    private int lastFEGenerated = 0;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public EnergyMode getEnergyMode() {
        return energyMode;
    }

    public boolean canToggleEnergyMode() {
        if (energyMode == EnergyMode.FE && !GTCompat.isLoaded()) return false;
        return true;
    }

    public void toggleEnergyMode() {
        if (!canToggleEnergyMode()) return;
        energyMode = energyMode.opposite();
        lazyEnergy.invalidate();
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(NPMachineUI.UI_W, NPMachineUI.UI_H, this, player);
        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.steam_engine");

        ui.mainGroup.addWidget(NPMachineUI.tankBar(60, 17, steamTank));
        ui.mainGroup.addWidget(NPMachineUI.feBar(104, 17,
                () -> storedFE, ENERGY_CAPACITY,
                () -> energyMode.displayUnit()));

        // Energy-mode tag above the FE bar — green for FE, light blue for EU.
        ui.mainGroup.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 104, 8,
                () -> energyMode == EnergyMode.FE ? "§aFE" : "§bEU")
                .setDropShadow(true));

        // Status line: green "Running (+N FE/t)" while steam is being
        // consumed, grey "Idle" otherwise. Minecraft format codes (§a, §7)
        // colour the text inline since LabelWidget only takes a single
        // base colour. The number is the FE generated last tick — scales
        // with steam tank fill so players can see exactly what their
        // boiler setup is producing.
        ui.mainGroup.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 58,
                () -> lastFEGenerated > 0
                        ? "§aRunning §f(+" + lastFEGenerated + " FE/t)"
                        : "§7Idle")
                .setDropShadow(true));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);
        ui.mainGroup.addWidget(new NPTabs()
                .energyTab(this::getEnergyMode, this::toggleEnergyMode,
                        this::canToggleEnergyMode)
                .build());
        return ui;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && energyMode == EnergyMode.FE) {
            return lazyEnergy.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
        lazyFluidHandler = LazyOptional.of(() -> steamTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag tankTag = new CompoundTag();
        steamTank.writeToNBT(tankTag);
        tag.put("steam", tankTag);
        tag.putInt("fe", storedFE);
        tag.putString("energyMode", energyMode.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag.getCompound("steam"));
        storedFE = tag.getInt("fe");
        if (tag.contains("energyMode")) {
            try { energyMode = EnergyMode.valueOf(tag.getString("energyMode")); }
            catch (IllegalArgumentException ignored) { energyMode = EnergyMode.FE; }
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean changed = false;
        lastFEGenerated = 0;

        // Output scales with steam fill level: empty -> 0 batches, full ->
        // MAX_BATCHES_PER_TICK. Math.ceil keeps the engine producing at least
        // 1 batch any time there's steam in the tank (so a single boiler in
        // equilibrium at low fill still delivers its 25 FE/tick).
        int steamHeld = steamTank.getFluidAmount();
        int steamCap = steamTank.getCapacity();
        if (steamHeld >= STEAM_PER_BATCH && steamCap > 0) {
            double fillFrac = (double) steamHeld / steamCap;
            int batchesByFill = Math.max(1, (int) Math.ceil(fillFrac * MAX_BATCHES_PER_TICK));
            int batchesBySteam = steamHeld / STEAM_PER_BATCH;
            int batchesByCap = (ENERGY_CAPACITY - storedFE) / FE_PER_BATCH;
            int batches = Math.min(batchesByFill, Math.min(batchesBySteam, batchesByCap));
            if (batches > 0) {
                int steamConsumed = batches * STEAM_PER_BATCH;
                int feProduced = batches * FE_PER_BATCH;
                steamTank.drain(steamConsumed, IFluidHandler.FluidAction.EXECUTE);
                storedFE += feProduced;
                lastFEGenerated = feProduced;
                changed = true;
            }
        }

        // Push FE to adjacent consumers. Skip GT-aware neighbours — their
        // Forge ENERGY shim silently voids FE when the EU side is full or
        // missing storage. The dedicated FE↔EU converter is the bridge for
        // GT integration. EU mode skips this push entirely; producer-side
        // EU push lands with the GT producer adapter in a follow-up commit.
        if (storedFE > 0 && energyMode == EnergyMode.FE) {
            for (Direction dir : Direction.values()) {
                if (storedFE <= 0) break;
                BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
                if (neighbour == null) continue;
                if (GTCompat.isLoaded()
                        && GTEnergyCompat.isExternalGTSink(neighbour, dir.getOpposite())) continue;
                int delta = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(sink -> {
                    if (!sink.canReceive()) return 0;
                    int offered = Math.min(storedFE, MAX_OUTPUT_FE_PER_TICK);
                    return sink.receiveEnergy(offered, false);
                }).orElse(0);
                if (delta > 0) {
                    storedFE -= delta;
                    changed = true;
                }
            }
        }

        if (changed) setChanged(level, pos, state);
    }

    public FluidTank getSteamTank() { return steamTank; }
}
