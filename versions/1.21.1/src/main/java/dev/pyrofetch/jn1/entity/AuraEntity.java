package dev.pyrofetch.jn1.entity;

import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.notebook.FieldNotebook;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Aura, l'IA holographique du laboratoire.
 *
 * <p>Elle attend au laboratoire de la faille, en veille — à peine visible,
 * immobile —, projetée par un projecteur holographique
 * ({@link dev.pyrofetch.jn1.rift.AuraProjectorBlock}) qu'on pose dans le
 * bâtiment. Un joueur qui s'approche ou la touche l'éveille.
 *
 * <p>Elle ne quitte pas son projecteur. Son câlin ne demande aucune
 * affection. La <b>liaison</b> qu'il établissait (drapeau {@code aura_link},
 * resté dans les sauvegardes) n'est plus exigée par rien depuis 0.84.0 :
 * la progression ne passe plus par elle.
 *
 * <p>Une projection ne se blesse pas : rien ne l'atteint, sinon le vide sous le
 * monde et la commande qui tue.
 */
public class AuraEntity extends CompanionEntity {
	/** À cette distance, un joueur l'éveille. */
	private static final double ÉVEIL = 5.0;

	private static final TrackedData<Boolean> DORMANT =
			DataTracker.registerData(AuraEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/**
	 * Dessinée en hologramme (translucide, lumineuse, qui scintille) ; faux :
	 * pleine, comme une autre compagne. L'œuf « opaque » la fait paraître ainsi,
	 * pour juger sa planche telle qu'elle est.
	 */
	private static final TrackedData<Boolean> HOLOGRAM =
			DataTracker.registerData(AuraEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/**
	 * Sa place : au-dessus de son projecteur (0.84.0). Une FapGPT sans projecteur (œuf,
	 * commande) prend pour place l'endroit où elle paraît.
	 */
	@org.jetbrains.annotations.Nullable
	private net.minecraft.util.math.Vec3d place;

	public AuraEntity(EntityType<? extends AuraEntity> type, World world) {
		super(type, world);
	}

	@Override
	public boolean peutQuitterSaPlace() {
		return false;
	}

	/**
	 * Le projecteur le plus proche, à 16 blocs (6 en hauteur) : celle d'un
	 * monde existant, qui s'en était éloignée, y retourne. Cherché une fois.
	 */
	@org.jetbrains.annotations.Nullable
	private net.minecraft.util.math.BlockPos projecteur() {
		net.minecraft.util.math.BlockPos ici = this.getBlockPos();
		net.minecraft.util.math.BlockPos meilleur = null;
		double mieux = Double.MAX_VALUE;
		for (net.minecraft.util.math.BlockPos p : net.minecraft.util.math.BlockPos.iterate(ici.add(-16, -6, -16), ici.add(16, 6, 16))) {
			if (this.getWorld().getBlockState(p).getBlock() instanceof dev.pyrofetch.jn1.rift.AuraProjectorBlock) {
				double d = p.getSquaredDistance(ici);
				if (d < mieux) {
					mieux = d;
					meilleur = p.toImmutable();
				}
			}
		}
		return meilleur;
	}

	/** Tenue à sa place, sauf le temps d'un câlin, qui la rend ensuite d'un coup. */
	private void tenirSaPlace() {
		if (this.place == null) {
			net.minecraft.util.math.BlockPos p = this.projecteur();
			this.place = p != null
					? new net.minecraft.util.math.Vec3d(p.getX() + 0.5, p.getY() + dev.pyrofetch.jn1.rift.AuraProjectorBlock.HAUTEUR, p.getZ() + 0.5)
					: this.getPos();
		}
		if (this.getGesture() == CompanionGesture.HUG) {
			return;
		}
		if (this.getMode() == dev.pyrofetch.jn1.entity.JennyMode.FOLLOW) {
			this.setMode(dev.pyrofetch.jn1.entity.JennyMode.IDLE);
		}
		this.getNavigation().stop();
		if (this.squaredDistanceTo(this.place) > 0.01) {
			this.refreshPositionAndAngles(this.place.x, this.place.y, this.place.z, this.getYaw(), this.getPitch());
		}
		this.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
	}

	@Override
	public CompanionKind kind() {
		return CompanionKind.AURA;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(DORMANT, true);
		builder.add(HOLOGRAM, true);
	}

	/** En veille : à peine visible, elle attend qu'on la trouve. */
	@Override
	public boolean estEnVeille() {
		return this.isDormant();
	}

	public boolean isDormant() {
		return this.dataTracker.get(DORMANT);
	}

	public void setDormant(boolean dormant) {
		this.dataTracker.set(DORMANT, dormant);
	}

	public boolean isHologram() {
		return this.dataTracker.get(HOLOGRAM);
	}

	/** Son portrait est un hologramme aussi, éveillé ou en veille comme elle. */
	@Override
	public void copierApparence(CompanionEntity copie) {
		super.copierApparence(copie);
		if (copie instanceof AuraEntity aura) {
			aura.dataTracker.set(HOLOGRAM, this.isHologram());
			aura.dataTracker.set(DORMANT, this.isDormant());
		}
	}

	private void éveiller(ServerPlayerEntity joueur) {
		if (!this.isDormant()) {
			return;
		}
		this.setDormant(false);
		this.setCompanion(joueur);
		this.getLookControl().lookAt(joueur);
		this.playGesture(CompanionGesture.WAVE);
		this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
				SoundCategory.NEUTRAL, 0.8F, 1.4F);
		for (ServerPlayerEntity témoin : ((ServerWorld) this.getWorld()).getPlayers(
				j -> j.squaredDistanceTo(this) < 16 * 16)) {
			témoin.sendMessage(Text.translatable("message.jn1.aura.wake"), false);
			DialogueEngine.setFlag(témoin, FieldNotebook.FLAG_AURA);
		}
	}

	@Override
	public void tick() {
		super.tick();
		World monde = this.getWorld();
		if (!monde.isClient()) {
			this.tenirSaPlace();
		}
		if (!monde.isClient() && this.isDormant() && this.age % 10 == 0) {
			PlayerEntity proche = monde.getClosestPlayer(this, ÉVEIL);
			if (proche instanceof ServerPlayerEntity joueur && !joueur.isSpectator()) {
				this.éveiller(joueur);
			}
		}
		// Quelques grains de lumière s'échappent de la projection.
		if (monde.isClient() && this.isHologram() && this.random.nextInt(this.isDormant() ? 14 : 6) == 0) {
			monde.addParticle(new DustParticleEffect(new Vector3f(0.45F, 0.9F, 1.0F), 0.6F),
					this.getX() + (this.random.nextDouble() - 0.5) * 0.7,
					this.getY() + this.random.nextDouble() * 1.9,
					this.getZ() + (this.random.nextDouble() - 0.5) * 0.7, 0.0, 0.02, 0.0);
		}
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		// En veille, un toucher l'éveille, et rien d'autre.
		if (this.isDormant()) {
			if (player instanceof ServerPlayerEntity joueur) {
				this.éveiller(joueur);
			}
			return ActionResult.SUCCESS;
		}
		return super.interactMob(player, hand);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.GENERIC_KILL)
				&& !source.isOf(dev.pyrofetch.jn1.cum.DildoBlock.DÉCHIRURE);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Dormant", this.isDormant());
		nbt.putBoolean("Hologram", this.isHologram());
		if (this.place != null) {
			nbt.putDouble("PlaceX", this.place.x);
			nbt.putDouble("PlaceY", this.place.y);
			nbt.putDouble("PlaceZ", this.place.z);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.setDormant(!nbt.contains("Dormant") || nbt.getBoolean("Dormant"));
		this.dataTracker.set(HOLOGRAM, !nbt.contains("Hologram") || nbt.getBoolean("Hologram"));
		if (nbt.contains("PlaceX")) {
			this.place = new net.minecraft.util.math.Vec3d(nbt.getDouble("PlaceX"), nbt.getDouble("PlaceY"), nbt.getDouble("PlaceZ"));
		}
	}
}
