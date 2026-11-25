package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * La flèche collante en vol. Elle colle à ce qu'elle touche : une créature
 * est ralentie (Lenteur II, cinq secondes) — une fille prend en plus un
 * niveau de traces —, un bloc garde une trace sur la face touchée.
 */
public class CumArrowEntity extends AbstractArrow {
	/** La trace n'est posée qu'une fois, au premier contact. */
	private boolean collée;

	public CumArrowEntity(EntityType<? extends CumArrowEntity> type, Level world) {
		super(type, world);
	}

	public CumArrowEntity(Level world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(ModEntities.CUM_ARROW, owner, world, stack, shotFrom);
	}

	public CumArrowEntity(Level world, double x, double y, double z, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(ModEntities.CUM_ARROW, x, y, z, world, stack, shotFrom);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(ModItems.CUM_ARROW);
	}

	@Override
	protected void doPostHurtEffects(LivingEntity target) {
		super.doPostHurtEffects(target);
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), this.getOwner());
		if (target instanceof CompanionEntity fille) {
			fille.addTraces(1);
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!this.level().isClientSide() && !this.collée) {
			this.collée = true;
			Cum.poserTrace(this.level(), hit.getBlockPos().relative(hit.getDirection()), hit.getDirection().getOpposite());
		}
	}
}
