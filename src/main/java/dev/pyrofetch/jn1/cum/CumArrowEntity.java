package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * La flèche collante en vol. Elle colle à ce qu'elle touche : une créature
 * est ralentie (Lenteur II, cinq secondes) — une fille prend en plus un
 * niveau de traces —, un bloc garde une trace sur la face touchée.
 */
public class CumArrowEntity extends PersistentProjectileEntity {
	/** La trace n'est posée qu'une fois, au premier contact. */
	private boolean collée;

	public CumArrowEntity(EntityType<? extends CumArrowEntity> type, World world) {
		super(type, world);
	}

	public CumArrowEntity(World world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(ModEntities.CUM_ARROW, owner, world, stack, shotFrom);
	}

	public CumArrowEntity(World world, double x, double y, double z, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(ModEntities.CUM_ARROW, x, y, z, world, stack, shotFrom);
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(ModItems.CUM_ARROW);
	}

	@Override
	protected void onHit(LivingEntity target) {
		super.onHit(target);
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), this.getOwner());
		if (target instanceof CompanionEntity fille) {
			fille.addTraces(1);
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		if (!this.getWorld().isClient() && !this.collée) {
			this.collée = true;
			Cum.poserTrace(this.getWorld(), hit.getBlockPos().offset(hit.getSide()), hit.getSide().getOpposite());
		}
	}
}
