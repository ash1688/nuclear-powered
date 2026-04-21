package io.github.ash1688.nuclearpowered.integration.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

// Display-only POJO for the hardcoded reprocessing-chain recipes. JEI only
// needs to show these to the player — the actual processing logic lives in
// each machine's block entity.
public class ReprocessingRecipe {
    public final List<ItemStack> inputs;
    public final FluidStack fluidInput;   // may be empty
    public final List<ItemStack> outputs;
    public final int feCost;
    public final int processTicks;

    public ReprocessingRecipe(List<ItemStack> inputs, FluidStack fluidInput,
                              List<ItemStack> outputs, int feCost, int processTicks) {
        this.inputs = inputs;
        this.fluidInput = fluidInput == null ? FluidStack.EMPTY : fluidInput;
        this.outputs = outputs;
        this.feCost = feCost;
        this.processTicks = processTicks;
    }

    public static ReprocessingRecipe of(int feCost, int processTicks,
                                        List<ItemStack> inputs, List<ItemStack> outputs) {
        return new ReprocessingRecipe(inputs, FluidStack.EMPTY, outputs, feCost, processTicks);
    }

    public static ReprocessingRecipe of(int feCost, int processTicks, FluidStack fluid,
                                        List<ItemStack> inputs, List<ItemStack> outputs) {
        return new ReprocessingRecipe(inputs, fluid, outputs, feCost, processTicks);
    }
}
