package io.github.ash1688.nuclearpowered.compat.gtceu;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * FluidTank that treats every {@link SteamCompat#isSteam(net.minecraft.world.level.material.Fluid)
 * registered steam variant} as fungible. Volume is preserved; the stored
 * fluid variant is coerced to whatever the latest fill request brought.
 *
 * <p>Why: vanilla {@link FluidTank#fill} hard-rejects any incoming fluid that
 * isn't {@code isFluidEqual} to the current contents. With NP's Coal Boiler
 * dynamically switching between {@code nuclearpowered:steam} and
 * {@code gtceu:steam} based on whether GT is loaded, an Engine tank that
 * still holds the previous variant would refuse the boiler's new offers and
 * the steam line silently breaks. By coercing the stored variant to the
 * incoming one, NP's steam infrastructure stays portable across mod
 * configurations and across saves where GT was added mid-game.</p>
 */
public class SteamTank extends FluidTank {
    public SteamTank(int capacity) {
        super(capacity, stack -> SteamCompat.isSteam(stack.getFluid()));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) return 0;

        // Tank holds a different but-still-valid steam variant: swap the
        // stored variant in-place (volume unchanged) so the upstream
        // FluidTank.fill check (`fluid.isFluidEqual(resource)`) matches.
        boolean coerce = !fluid.isEmpty()
                && fluid.getFluid() != resource.getFluid()
                && SteamCompat.isSteam(fluid.getFluid());
        if (coerce) {
            if (action.simulate()) {
                // Predict the post-coerce fill outcome without mutating.
                return Math.min(capacity - fluid.getAmount(), resource.getAmount());
            }
            fluid = new FluidStack(resource.getFluid(), fluid.getAmount());
            onContentsChanged();
        }
        return super.fill(resource, action);
    }
}
