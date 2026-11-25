package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * La fiole à jeter en vol. Elle éclate au premier contact, comme une potion
 * jetable : la face touchée garde une trace, et le sol alentour en reçoit
 * quelques-unes (rayon d'un bloc et demi environ). Ce qui est à moins de
 * {@link #RAYON} blocs est éclaboussé : une fille prend un niveau de traces,
 * les autres sont aveuglés et ralentis trois secondes.
 */
public class CumFlaskEntity extends ThrowableItemProjectile {
	public static final double RAYON = 2.5;
	/** La couleur des particules de l'éclatement, celles d'une potion. */
	private static final int BLANC = 0xF0ECE0;

	public CumFlaskEntity(EntityType<? extends CumFlaskEntity> type, Level world) {
		super(type, world);
	}

	public CumFlaskEntity(Level world, LivingEntity owner) {
		// 26.3 : le projectile reçoit son objet à la construction.
		super(ModEntities.SPLASH_CUM, owner, world, new net.minecraft.world.item.ItemStack(ModItems.SPLASH_CUM));
	}

	public CumFlaskEntity(Level world, double x, double y, double z) {
		super(ModEntities.SPLASH_CUM, x, y, z, world, new net.minecraft.world.item.ItemStack(ModItems.SPLASH_CUM));
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.SPLASH_CUM;
	}

	/** Celle d'une potion jetable : plus lourde qu'une boule de neige. */
	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (this.level().isClientSide()) {
			return;
		}
		Level monde = this.level();
		this.éclabousser(monde, hit);
		monde.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, this.blockPosition(), BLANC);
		Cum.bruit(monde, this.blockPosition());
		this.discard();
	}

	private void éclabousser(Level monde, HitResult hit) {
		if (hit instanceof BlockHitResult bloc && hit.getType() == HitResult.Type.BLOCK) {
			Cum.poserTrace(monde, bloc.getBlockPos().relative(bloc.getDirection()), bloc.getDirection().getOpposite());
		}
		// Le sol alentour : des gouttes jusqu'à un cran au-dessus et deux en dessous.
		Vec3 centre = hit.getLocation();
		RandomSource hasard = this.getRandom();
		int voulues = 3 + hasard.nextInt(4);
		for (int essai = 0, posées = 0; essai < 14 && posées < voulues; essai++) {
			double x = centre.x + hasard.nextGaussian() * 1.0;
			double z = centre.z + hasard.nextGaussian() * 1.0;
			for (int dy = 1; dy >= -2; dy--) {
				if (Cum.poserTrace(monde, BlockPos.containing(x, centre.y + dy, z), Direction.DOWN)) {
					posées++;
					break;
				}
			}
		}
		for (LivingEntity cible : monde.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(RAYON, RAYON * 0.6, RAYON))) {
			if (cible.distanceToSqr(centre) > RAYON * RAYON) {
				continue;
			}
			if (cible instanceof CompanionEntity fille) {
				fille.addTraces(1);
			} else {
				cible.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this.getOwner());
				cible.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), this.getOwner());
			}
		}
	}
}
