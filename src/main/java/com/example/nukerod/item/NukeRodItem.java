package com.example.nukerod.item;

import com.example.nukerod.config.NukeConfig;
import com.example.nukerod.entity.ModEntities;
import com.example.nukerod.entity.NukeWarheadEntity;
import com.example.nukerod.registry.ModItems;
import com.example.nukerod.sound.ModSounds;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * The Nuke Rod item, used like a sniper rifle:
 * <ul>
 *   <li><b>Right-click</b> toggles the scope (handled client-side in
 *       {@code NukeZoom}); it does not fire.</li>
 *   <li><b>Left-click</b> fires. The client sends a {@code FireRodPayload} and
 *       the server runs {@link #serverFire(ServerPlayerEntity)}: it detonates an
 *       airborne warhead if one exists, otherwise consumes a core and launches a
 *       new one straight down the player's aim.</li>
 * </ul>
 */
public class NukeRodItem extends FishingRodItem {

    /** Launch speed of the warhead — high for a fast, long-range strike. */
    private static final float LAUNCH_SPEED = 4.5f;

    public NukeRodItem(Settings settings) {
        super(settings);
    }

    // Right-click no longer fires or casts a bobber; the scope is toggled
    // client-side by watching the use key, so this is intentionally a no-op.
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return ActionResult.PASS;
    }

    /**
     * Server-authoritative fire. Detonate an owned airborne warhead if present,
     * otherwise consume a Warhead Core (unless creative/not required) and launch
     * a new warhead straight down the player's crosshair.
     */
    public static void serverFire(ServerPlayerEntity user) {
        World world = user.getEntityWorld();
        if (world.isClient()) {
            return;
        }

        NukeWarheadEntity existing = findOwnedWarhead(world, user);
        if (existing != null) {
            existing.detonate();
            world.playSound(null, user.getBlockPos(), ModSounds.WARHEAD_LAUNCH,
                    SoundCategory.PLAYERS, 0.6f, 1.4f);
            return;
        }

        if (NukeConfig.get().requiresWarheadCore && !user.getAbilities().creativeMode
                && !consumeWarheadCore(user)) {
            user.sendMessage(Text.translatable("item.nukerod.nuke_rod.no_ammo")
                    .formatted(Formatting.RED), true);
            return;
        }

        NukeWarheadEntity warhead = new NukeWarheadEntity(ModEntities.NUKE_WARHEAD, world);
        warhead.setOwner(user);
        // Spawn just in front of the eyes along the look vector so it fires
        // straight down the crosshair without hitting the player.
        Vec3d look = user.getRotationVec(1.0f);
        warhead.setPosition(user.getX() + look.x * 0.8,
                user.getEyeY() - 0.1 + look.y * 0.8,
                user.getZ() + look.z * 0.8);
        warhead.launch(user.getPitch(), user.getYaw(), LAUNCH_SPEED);
        world.spawnEntity(warhead);

        world.playSound(null, user.getBlockPos(), ModSounds.WARHEAD_LAUNCH,
                SoundCategory.PLAYERS, 1.2f, 1.0f);

        ItemStack stack = user.getMainHandStack().isOf(ModItems.NUKE_ROD)
                ? user.getMainHandStack() : user.getOffHandStack();
        stack.damage(NukeConfig.get().durabilityPerUse, user, EquipmentSlot.MAINHAND);
        user.incrementStat(Stats.USED.getOrCreateStat(ModItems.NUKE_ROD));
    }

    /** Locate a live warhead whose owner is {@code user}, if any. */
    private static NukeWarheadEntity findOwnedWarhead(World world, PlayerEntity user) {
        for (NukeWarheadEntity e : world.getEntitiesByClass(NukeWarheadEntity.class,
                user.getBoundingBox().expand(512.0),
                w -> w.getOwner() == user && !w.isRemoved())) {
            return e;
        }
        return null;
    }

    /** Remove one warhead core from the player's inventory; true if consumed. */
    private static boolean consumeWarheadCore(PlayerEntity user) {
        for (int i = 0; i < user.getInventory().size(); i++) {
            ItemStack s = user.getInventory().getStack(i);
            if (s.isOf(ModItems.WARHEAD_CORE)) {
                s.decrement(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable("item.nukerod.nuke_rod.tooltip1")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
        textConsumer.accept(Text.translatable("item.nukerod.nuke_rod.tooltip2")
                .formatted(Formatting.DARK_RED));
        if (NukeConfig.get().requiresWarheadCore) {
            textConsumer.accept(Text.translatable("item.nukerod.nuke_rod.tooltip_ammo")
                    .formatted(Formatting.DARK_GRAY));
        }
    }
}
