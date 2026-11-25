package dev.pyrofetch.jn1.cum;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

/**
 * La fiole à jeter : une fiole de cum et de la poudre à canon, comme une
 * potion jetable. Elle éclate où elle tombe ({@link CumFlaskEntity}). Le
 * distributeur la lance aussi.
 */
public class SplashCumItem extends Item implements ProjectileItem {
	public SplashCumItem(Item.Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		ItemStack pile = user.getItemInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.SPLASH_POTION_THROW,
				SoundSource.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!world.isClientSide()) {
			CumFlaskEntity fiole = new CumFlaskEntity(world, user);
			fiole.setItem(pile);
			fiole.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
			world.addFreshEntity(fiole);
		}
		user.awardStat(Stats.ITEM_USED.get(this));
		pile.consume(1, user);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Projectile asProjectile(Level world, Position pos, ItemStack stack, Direction direction) {
		CumFlaskEntity fiole = new CumFlaskEntity(world, pos.x(), pos.y(), pos.z());
		fiole.setItem(stack);
		return fiole;
	}
}
