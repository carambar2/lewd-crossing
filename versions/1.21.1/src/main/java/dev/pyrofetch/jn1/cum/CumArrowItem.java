package dev.pyrofetch.jn1.cum;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * La flèche collante : huit flèches trempées dans une fiole de cum. L'arc et
 * l'arbalète la tirent parce qu'elle porte l'étiquette {@code minecraft:arrows} ;
 * le distributeur, parce qu'elle y est inscrite au démarrage.
 */
public class CumArrowItem extends ArrowItem {
	public CumArrowItem(net.minecraft.item.Item.Settings settings) {
		super(settings);
	}

	@Override
	public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter,
	                                              @Nullable ItemStack shotFrom) {
		return new CumArrowEntity(world, shooter, stack.copyWithCount(1), shotFrom);
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		CumArrowEntity flèche = new CumArrowEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1), null);
		flèche.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
		return flèche;
	}
}
