package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Une faille de flux : une déchirure dans le monde, qui donne sur le vide.
 *
 * <p>Celles qui comptent sont tenues par une brèche, au cœur d'un site de
 * confinement (voir {@link BreachBlock}) : elles ne se referment jamais. Une
 * faille invoquée à la main, sans brèche, vit huit minutes et se referme à
 * l'aube. Celui qui la traverse est emporté dans le vide, à l'aplomb de la
 * faille, en plein vide (voir {@link RiftTravel}).
 *
 * <p>Elle n'a pas de corps : on ne la frappe pas, on ne la pousse pas, on ne
 * s'y cogne pas. On y entre, c'est tout.
 */
public class RiftEntity extends Entity {
	/** Le temps de s'ouvrir, puis de se refermer, en ticks. */
	public static final int OUVERTURE = 30;
	public static final int FERMETURE = 40;
	/** Durée de vie par défaut d'une faille invoquée à la main : huit minutes. */
	private static final int VIE_PAR_DÉFAUT = 20 * 60 * 8;

	/**
	 * Depuis combien de ticks elle se referme ; zéro tant qu'elle est ouverte.
	 *
	 * <p>Suivi pour le client, qui la rétrécit à mesure : sans cela elle
	 * disparaîtrait d'un coup.
	 */
	private static final EntityDataAccessor<Integer> FERMETURE_EN_COURS =
			SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
	/**
	 * Sa hauteur, en blocs (0.83.8). 2,6 pour une faille de
	 * passage ; celle d'une brèche prend la taille réglée sur la brèche. La
	 * largeur suit : une fente, un peu plus d'un quart de la hauteur.
	 */
	private static final EntityDataAccessor<Float> TAILLE =
			SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.FLOAT);
	public static final float TAILLE_DÉFAUT = 2.6F;

	private int vie = VIE_PAR_DÉFAUT;
	/** La brèche qui la tient ouverte ; {@code null} pour une faille de passage. */
	@Nullable
	private BlockPos brèche;

	public RiftEntity(EntityType<? extends RiftEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void setVie(int ticks) {
		this.vie = ticks;
	}

	/** La rattache à sa brèche : elle ne se refermera plus tant qu'elle existe. */
	public void ancrer(BlockPos brèche) {
		this.brèche = brèche.immutable();
	}

	public boolean ancrée() {
		return this.brèche != null;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(FERMETURE_EN_COURS, 0);
		builder.define(TAILLE, TAILLE_DÉFAUT);
	}

	public float taille() {
		return this.entityData.get(TAILLE);
	}

	public void setTaille(float hauteur) {
		this.entityData.set(TAILLE, Math.max(1.0F, hauteur));
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (TAILLE.equals(data)) {
			this.refreshDimensions();
		}
	}

	@Override
	public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
		float h = this.taille();
		return EntityDimensions.scalable(Math.max(1.0F, h * 0.28F), h);
	}

	/**
	 * Son ouverture, de 0 à 1 : c'est ce qui en règle la largeur à l'écran.
	 *
	 * <p>Elle s'élargit à l'ouverture et se pince à la fermeture. L'âge de
	 * l'entité suffit à la première : le client le compte aussi.
	 */
	public float ouverture(float tickDelta) {
		float ouvre = Math.min(1.0F, (this.tickCount + tickDelta) / OUVERTURE);
		int ferme = this.entityData.get(FERMETURE_EN_COURS);
		if (ferme > 0) {
			ouvre *= Math.max(0.0F, 1.0F - (ferme + tickDelta) / FERMETURE);
		}
		return ouvre;
	}

	public boolean seReferme() {
		return this.entityData.get(FERMETURE_EN_COURS) > 0;
	}

	public void refermer() {
		if (!this.seReferme()) {
			this.entityData.set(FERMETURE_EN_COURS, 1);
		}
	}

	@Override
	public void tick() {
		super.tick();
		Level monde = this.level();
		if (monde.isClientSide()) {
			// Quelques poussières qui s'échappent : de près, on la voit respirer.
			if (this.random.nextInt(3) == 0 && this.ouverture(0.0F) > 0.5F) {
				monde.addParticle(ModParticles.GLYPH_MOTE,
						this.getX() + (this.random.nextDouble() - 0.5) * 0.6 * this.getBbWidth(),
						this.getY() + this.random.nextDouble() * this.getBbHeight(),
						this.getZ() + (this.random.nextDouble() - 0.5) * 0.6 * this.getBbWidth(),
						(this.random.nextDouble() - 0.5) * 0.02, 0.01, (this.random.nextDouble() - 0.5) * 0.02);
			}
			return;
		}

		int ferme = this.entityData.get(FERMETURE_EN_COURS);
		if (ferme > 0) {
			if (ferme >= FERMETURE) {
				this.discard();
			} else {
				this.entityData.set(FERMETURE_EN_COURS, ferme + 1);
			}
			return;
		}

		if (this.brèche != null) {
			// On ne referme pas une brèche : la faille ne tombe qu'avec son socle,
			// ce qui n'arrive qu'à la main, en créatif ou par commande.
			if (!(monde.getBlockState(this.brèche).getBlock() instanceof BreachBlock)) {
				this.refermer();
				return;
			}
		} else if (--this.vie <= 0 || (monde.dimensionType().hasSkyLight() && monde.isBrightOutside())) {
			// Sans brèche, elle se referme à l'aube : c'est une chose de la nuit.
			this.refermer();
			return;
		}

		// On n'y entre qu'une fois grande ouverte : une faille naissante ne
		// happe personne au passage.
		if (this.tickCount < OUVERTURE) {
			return;
		}
		for (ServerPlayer joueur : monde.getEntitiesOfClass(ServerPlayer.class,
				this.getBoundingBox().deflate(0.3, 0.2, 0.3),
				joueur -> !joueur.isSpectator() && !joueur.isPassenger() && ModAttachments.riftReady(joueur))) {
			RiftTravel.traverser(joueur, this);
		}
	}

	@Override
	public boolean hurtServer(net.minecraft.server.level.ServerLevel monde, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean allowVehicles) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput nbt) {
		nbt.getInt("life").ifPresent(vie -> this.vie = vie);
		if (nbt.getBooleanOr("closing", false)) {
			this.refermer();
		}
		this.brèche = nbt.read("breach", BlockPos.CODEC).orElse(null);
		nbt.read("size", com.mojang.serialization.Codec.FLOAT).ifPresent(this::setTaille);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput nbt) {
		nbt.putInt("life", this.vie);
		nbt.putBoolean("closing", this.seReferme());
		nbt.putFloat("size", this.taille());
		nbt.storeNullable("breach", BlockPos.CODEC, this.brèche);
	}
}
