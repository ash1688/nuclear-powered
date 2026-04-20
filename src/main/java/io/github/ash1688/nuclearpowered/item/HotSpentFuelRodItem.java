package io.github.ash1688.nuclearpowered.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Freshly spent fuel rods from a pile are dangerously hot. Holding one in the
// selected hotbar slot burns the player — once a second while selected, they
// take a point of damage and catch fire for ~1 second. Stashing the rod in a
// non-selected inventory slot or dropping it into a Cooling Pond stops the
// damage; once cooled the pond yields a normal depleted_uranium_fuel_rod.
public class HotSpentFuelRodItem extends Item {
    private static final int DAMAGE_EVERY_TICKS = 20;
    private static final int FIRE_DURATION_TICKS = 40;
    private static final float DAMAGE_PER_HIT = 1.0f;

    public HotSpentFuelRodItem(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!isSelected) return;
        if (!(entity instanceof Player player)) return;
        if (level.getGameTime() % DAMAGE_EVERY_TICKS != 0L) return;
        player.setRemainingFireTicks(FIRE_DURATION_TICKS);
        player.hurt(level.damageSources().inFire(), DAMAGE_PER_HIT);
    }
}
