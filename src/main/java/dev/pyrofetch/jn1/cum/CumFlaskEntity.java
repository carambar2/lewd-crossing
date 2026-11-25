package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

/**
 * La fiole à jeter en vol. Elle éclate au premier contact, comme une potion
 * jetable : la face touchée garde une trace, et le sol alentour en reçoit
 * quelques-unes (rayon d'un bloc et demi environ). Ce qui est à moins de
 * {@link #RAYON} blocs est éclaboussé : une fille prend un niveau de traces,
 * les autres sont aveuglés et ralentis trois secondes.
 */
public class CumFlaskEntity extends ThrownItemEntity {
	public static final double RAYON = 2.5;
	/** La couleur des particules de l'éclatement, celles d'une potion. */
	private static final int BLANC = 0xF0ECE0;

	public CumFlaskEntity(EntityType<? extends CumFlaskEntity> type, World world) {
		super(type, world);
	}

	public CumFlaskEntity(World world, LivingEntity owner) {
		super(ModEntities.SPLASH_CUM, owner, world);
	}

	public CumFlaskEntity(World world, double x, double y, double z) {
		super(ModEntities.SPLASH_CUM, x, y, z, world);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.SPLASH_CUM;
	}

	/** Celle d'une potion jetable : plus lourde qu'une boule de neige. */
	@Override
	protected double getGravity() {
		return 0.05;
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (this.getWorld().isClient()) {
			return;
		}
		World monde = this.getWorld();
		this.éclabousser(monde, hit);
		monde.syncWorldEvent(WorldEvents.SPLASH_POTION_SPLASHED, this.getBlockPos(), BLANC);
		Cum.bruit(monde, this.getBlockPos());
		this.discard();
	}

	private void éclabousser(World monde, HitResult hit) {
		if (hit instanceof BlockHitResult bloc && hit.getType() == HitResult.Type.BLOCK) {
			Cum.poserTrace(monde, bloc.getBlockPos().offset(bloc.getSide()), bloc.getSide().getOpposite());
		}
		// Le sol alentour : des gouttes jusqu'à un cran au-dessus et deux en dessous.
		Vec3d centre = hit.getPos();
		Random hasard = this.getRandom();
		int voulues = 3 + hasard.nextInt(4);
		for (int essai = 0, posées = 0; essai < 14 && posées < voulues; essai++) {
			double x = centre.x + hasard.nextGaussian() * 1.0;
			double z = centre.z + hasard.nextGaussian() * 1.0;
			for (int dy = 1; dy >= -2; dy--) {
				if (Cum.poserTrace(monde, BlockPos.ofFloored(x, centre.y + dy, z), Direction.DOWN)) {
					posées++;
					break;
				}
			}
		}
		for (LivingEntity cible : monde.getNonSpectatingEntities(LivingEntity.class,
				this.getBoundingBox().expand(RAYON, RAYON * 0.6, RAYON))) {
			if (cible.squaredDistanceTo(centre) > RAYON * RAYON) {
				continue;
			}
			if (cible instanceof CompanionEntity fille) {
				fille.addTraces(1);
			} else {
				cible.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0), this.getOwner());
				cible.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0), this.getOwner());
			}
		}
	}
}
