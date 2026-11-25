package dev.pyrofetch.jn1.rift;

import net.minecraft.entity.EntityDimensions;

import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
	private static final TrackedData<Integer> FERMETURE_EN_COURS =
			DataTracker.registerData(RiftEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/**
	 * Sa hauteur, en blocs (0.83.8). 2,6 pour une faille de
	 * passage ; celle d'une brèche prend la taille réglée sur la brèche. La
	 * largeur suit : une fente, un peu plus d'un quart de la hauteur.
	 */
	private static final TrackedData<Float> TAILLE =
			DataTracker.registerData(RiftEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public static final float TAILLE_DÉFAUT = 2.6F;

	private int vie = VIE_PAR_DÉFAUT;
	/** La brèche qui la tient ouverte ; {@code null} pour une faille de passage. */
	@Nullable
	private BlockPos brèche;

	public RiftEntity(EntityType<? extends RiftEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public void setVie(int ticks) {
		this.vie = ticks;
	}

	/** La rattache à sa brèche : elle ne se refermera plus tant qu'elle existe. */
	public void ancrer(BlockPos brèche) {
		this.brèche = brèche.toImmutable();
	}

	public boolean ancrée() {
		return this.brèche != null;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(FERMETURE_EN_COURS, 0);
		builder.add(TAILLE, TAILLE_DÉFAUT);
	}

	public float taille() {
		return this.dataTracker.get(TAILLE);
	}

	public void setTaille(float hauteur) {
		this.dataTracker.set(TAILLE, Math.max(1.0F, hauteur));
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (TAILLE.equals(data)) {
			this.calculateDimensions();
		}
	}

	@Override
	public EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose) {
		float h = this.taille();
		return EntityDimensions.changing(Math.max(1.0F, h * 0.28F), h);
	}

	/**
	 * Son ouverture, de 0 à 1 : c'est ce qui en règle la largeur à l'écran.
	 *
	 * <p>Elle s'élargit à l'ouverture et se pince à la fermeture. L'âge de
	 * l'entité suffit à la première : le client le compte aussi.
	 */
	public float ouverture(float tickDelta) {
		float ouvre = Math.min(1.0F, (this.age + tickDelta) / OUVERTURE);
		int ferme = this.dataTracker.get(FERMETURE_EN_COURS);
		if (ferme > 0) {
			ouvre *= Math.max(0.0F, 1.0F - (ferme + tickDelta) / FERMETURE);
		}
		return ouvre;
	}

	public boolean seReferme() {
		return this.dataTracker.get(FERMETURE_EN_COURS) > 0;
	}

	public void refermer() {
		if (!this.seReferme()) {
			this.dataTracker.set(FERMETURE_EN_COURS, 1);
		}
	}

	@Override
	public void tick() {
		super.tick();
		World monde = this.getWorld();
		if (monde.isClient()) {
			// Quelques poussières qui s'échappent : de près, on la voit respirer.
			if (this.random.nextInt(3) == 0 && this.ouverture(0.0F) > 0.5F) {
				monde.addParticle(ModParticles.GLYPH_MOTE,
						this.getX() + (this.random.nextDouble() - 0.5) * 0.6 * this.getWidth(),
						this.getY() + this.random.nextDouble() * this.getHeight(),
						this.getZ() + (this.random.nextDouble() - 0.5) * 0.6 * this.getWidth(),
						(this.random.nextDouble() - 0.5) * 0.02, 0.01, (this.random.nextDouble() - 0.5) * 0.02);
			}
			return;
		}

		int ferme = this.dataTracker.get(FERMETURE_EN_COURS);
		if (ferme > 0) {
			if (ferme >= FERMETURE) {
				this.discard();
			} else {
				this.dataTracker.set(FERMETURE_EN_COURS, ferme + 1);
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
		} else if (--this.vie <= 0 || (monde.getDimension().hasSkyLight() && monde.isDay())) {
			// Sans brèche, elle se referme à l'aube : c'est une chose de la nuit.
			this.refermer();
			return;
		}

		// On n'y entre qu'une fois grande ouverte : une faille naissante ne
		// happe personne au passage.
		if (this.age < OUVERTURE) {
			return;
		}
		for (ServerPlayerEntity joueur : monde.getEntitiesByClass(ServerPlayerEntity.class,
				this.getBoundingBox().contract(0.3, 0.2, 0.3),
				joueur -> !joueur.isSpectator() && !joueur.hasVehicle() && ModAttachments.riftReady(joueur))) {
			RiftTravel.traverser(joueur, this);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		if (nbt.contains("life")) {
			this.vie = nbt.getInt("life");
		}
		if (nbt.getBoolean("closing")) {
			this.refermer();
		}
		this.brèche = NbtHelper.toBlockPos(nbt, "breach").orElse(null);
		if (nbt.contains("size")) {
			this.setTaille(nbt.getFloat("size"));
		}
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("life", this.vie);
		nbt.putBoolean("closing", this.seReferme());
		nbt.putFloat("size", this.taille());
		if (this.brèche != null) {
			nbt.put("breach", NbtHelper.fromBlockPos(this.brèche));
		}
	}
}
