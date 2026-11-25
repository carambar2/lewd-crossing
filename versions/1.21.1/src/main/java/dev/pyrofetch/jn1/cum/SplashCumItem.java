package dev.pyrofetch.jn1.cum;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * La fiole à jeter : une fiole de cum et de la poudre à canon, comme une
 * potion jetable. Elle éclate où elle tombe ({@link CumFlaskEntity}). Le
 * distributeur la lance aussi.
 */
public class SplashCumItem extends Item implements ProjectileItem {
	public SplashCumItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack pile = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW,
				SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!world.isClient()) {
			CumFlaskEntity fiole = new CumFlaskEntity(world, user);
			fiole.setItem(pile);
			fiole.setVelocity(user, user.getPitch(), user.getYaw(), -20.0F, 0.5F, 1.0F);
			world.spawnEntity(fiole);
		}
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		pile.decrementUnlessCreative(1, user);
		return TypedActionResult.success(pile, world.isClient());
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		CumFlaskEntity fiole = new CumFlaskEntity(world, pos.getX(), pos.getY(), pos.getZ());
		fiole.setItem(stack);
		return fiole;
	}
}
