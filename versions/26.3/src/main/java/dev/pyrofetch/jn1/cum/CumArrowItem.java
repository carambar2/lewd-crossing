package dev.pyrofetch.jn1.cum;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * La flèche collante : huit flèches trempées dans une fiole de cum. L'arc et
 * l'arbalète la tirent parce qu'elle porte l'étiquette {@code minecraft:arrows} ;
 * le distributeur, parce qu'elle y est inscrite au démarrage.
 */
public class CumArrowItem extends ArrowItem {
	public CumArrowItem(net.minecraft.world.item.Item.Properties settings) {
		super(settings);
	}

	@Override
	public AbstractArrow createArrow(Level world, ItemStack stack, LivingEntity shooter,
	                                              @Nullable ItemStack shotFrom) {
		return new CumArrowEntity(world, shooter, stack.copyWithCount(1), shotFrom);
	}

	@Override
	public Projectile asProjectile(Level world, Position pos, ItemStack stack, Direction direction) {
		CumArrowEntity flèche = new CumArrowEntity(world, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
		flèche.pickup = AbstractArrow.Pickup.ALLOWED;
		return flèche;
	}
}
