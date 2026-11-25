package dev.pyrofetch.jn1.capsule;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * La capsule en vol.
 *
 * <p>Vide, elle capture la créature qu'elle touche ({@link CapsuleCapture}) ;
 * sinon elle retombe au sol, inchangée, plutôt que de se perdre. Pleine, elle
 * relâche sa captive là où elle touche — une créature ou un bloc — et retombe
 * vide (0.56.9). Gaster, lui, ne se capture pas.
 */
public class CapsuleEntity extends ThrowableItemProjectile {
	public CapsuleEntity(EntityType<? extends CapsuleEntity> type, Level world) {
		super(type, world);
	}

	public CapsuleEntity(Level world, LivingEntity owner) {
		// 26.3 : le projectile reçoit son objet à la construction.
		super(ModEntities.CAPSULE, owner, world, new net.minecraft.world.item.ItemStack(ModItems.CAPSULE));
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.CAPSULE;
	}

	private boolean pleine() {
		return CapsuleItem.isFull(this.getItem());
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (this.level().isClientSide()) {
			return;
		}
		ServerPlayer lanceur = this.getOwner() instanceof ServerPlayer player ? player : null;
		if (this.pleine()) {
			this.relâcher(result.getEntity().position(), result.getEntity().position());
			return;
		}
		if (result.getEntity() instanceof dev.pyrofetch.jn1.gaster.GasterEntity gaster) {
			CapsuleCapture.refusGaster(gaster, lanceur, this.getItem());
			this.discard();
			return;
		}
		if (!(result.getEntity() instanceof LivingEntity cible) || !CapsuleCapture.capturable(cible, lanceur)) {
			// Refusée : la capsule retombe, comme manquée (`onCollision`).
			return;
		}
		CapsuleCapture.capture(cible, lanceur);
		this.discard();
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level().isClientSide() || !this.isAlive()) {
			return;
		}
		if (this.pleine()) {
			// Un peu en deçà du point d'impact, du côté d'où elle vient : posée
			// contre un mur, la captive y serait plantée.
			Vec3 recul = this.getDeltaMovement().lengthSqr() > 1.0E-6
					? this.getDeltaMovement().normalize().scale(-0.6) : Vec3.ZERO;
			Vec3 où = result.getLocation().add(recul);
			this.relâcher(où, this.getOwner() != null ? this.getOwner().position() : où);
			return;
		}
		// Manquée : la capsule est un objet, pas un consommable. Elle retombe.
		ServerLevel world = (ServerLevel) this.level();
		world.addFreshEntity(new ItemEntity(world, this.getX(), this.getY(), this.getZ(),
				new ItemStack(ModItems.CAPSULE)));
		world.playSound(null, this.blockPosition(), SoundEvents.METAL_HIT,
				SoundSource.NEUTRAL, 0.5F, 1.2F);
		this.discard();
	}

	/** La captive sort ; la capsule, vide, retombe là. */
	private void relâcher(Vec3 où, Vec3 repli) {
		ServerLevel world = (ServerLevel) this.level();
		if (CapsuleItem.release(this.getItem(), world, où, repli)) {
			world.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE,
					SoundSource.NEUTRAL, 0.6F, 1.4F);
			world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, où.x, où.y + 0.8, où.z,
					30, 0.3, 0.6, 0.3, 0.05);
			world.addFreshEntity(new ItemEntity(world, this.getX(), this.getY(), this.getZ(),
					new ItemStack(ModItems.CAPSULE)));
		} else {
			// Rien n'a pu sortir : la capsule retombe pleine, rien n'est perdu.
			world.addFreshEntity(new ItemEntity(world, this.getX(), this.getY(), this.getZ(), this.getItem().copy()));
		}
		this.discard();
	}

	/** Elle ne doit pas capturer celui qui l'a lancée. */
	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !entity.equals(this.getOwner());
	}
}
