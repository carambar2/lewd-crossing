package dev.pyrofetch.jn1.capsule;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * La capsule en vol.
 *
 * <p>Vide, elle capture la créature qu'elle touche ({@link CapsuleCapture}) ;
 * sinon elle retombe au sol, inchangée, plutôt que de se perdre. Pleine, elle
 * relâche sa captive là où elle touche — une créature ou un bloc — et retombe
 * vide (0.56.9). Gaster, lui, ne se capture pas.
 */
public class CapsuleEntity extends ThrownItemEntity {
	public CapsuleEntity(EntityType<? extends CapsuleEntity> type, World world) {
		super(type, world);
	}

	public CapsuleEntity(World world, LivingEntity owner) {
		super(ModEntities.CAPSULE, owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.CAPSULE;
	}

	private boolean pleine() {
		return CapsuleItem.isFull(this.getStack());
	}

	@Override
	protected void onEntityHit(EntityHitResult result) {
		super.onEntityHit(result);
		if (this.getWorld().isClient()) {
			return;
		}
		ServerPlayerEntity lanceur = this.getOwner() instanceof ServerPlayerEntity player ? player : null;
		if (this.pleine()) {
			this.relâcher(result.getEntity().getPos(), result.getEntity().getPos());
			return;
		}
		if (result.getEntity() instanceof dev.pyrofetch.jn1.gaster.GasterEntity gaster) {
			CapsuleCapture.refusGaster(gaster, lanceur, this.getStack());
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
	protected void onCollision(HitResult result) {
		super.onCollision(result);
		if (this.getWorld().isClient() || !this.isAlive()) {
			return;
		}
		if (this.pleine()) {
			// Un peu en deçà du point d'impact, du côté d'où elle vient : posée
			// contre un mur, la captive y serait plantée.
			Vec3d recul = this.getVelocity().lengthSquared() > 1.0E-6
					? this.getVelocity().normalize().multiply(-0.6) : Vec3d.ZERO;
			Vec3d où = result.getPos().add(recul);
			this.relâcher(où, this.getOwner() != null ? this.getOwner().getPos() : où);
			return;
		}
		// Manquée : la capsule est un objet, pas un consommable. Elle retombe.
		ServerWorld world = (ServerWorld) this.getWorld();
		world.spawnEntity(new ItemEntity(world, this.getX(), this.getY(), this.getZ(),
				new ItemStack(ModItems.CAPSULE)));
		world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_METAL_HIT,
				SoundCategory.NEUTRAL, 0.5F, 1.2F);
		this.discard();
	}

	/** La captive sort ; la capsule, vide, retombe là. */
	private void relâcher(Vec3d où, Vec3d repli) {
		ServerWorld world = (ServerWorld) this.getWorld();
		if (CapsuleItem.release(this.getStack(), world, où, repli)) {
			world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
					SoundCategory.NEUTRAL, 0.6F, 1.4F);
			world.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, où.x, où.y + 0.8, où.z,
					30, 0.3, 0.6, 0.3, 0.05);
			world.spawnEntity(new ItemEntity(world, this.getX(), this.getY(), this.getZ(),
					new ItemStack(ModItems.CAPSULE)));
		} else {
			// Rien n'a pu sortir : la capsule retombe pleine, rien n'est perdu.
			world.spawnEntity(new ItemEntity(world, this.getX(), this.getY(), this.getZ(), this.getStack().copy()));
		}
		this.discard();
	}

	/** Elle ne doit pas capturer celui qui l'a lancée. */
	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !entity.equals(this.getOwner());
	}
}
