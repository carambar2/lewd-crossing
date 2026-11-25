package dev.pyrofetch.jn1.entity;

import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.notebook.FieldNotebook;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

	private static final EntityDataAccessor<Boolean> DORMANT =
			SynchedEntityData.defineId(AuraEntity.class, EntityDataSerializers.BOOLEAN);
	/**
	 * Dessinée en hologramme (translucide, lumineuse, qui scintille) ; faux :
	 * pleine, comme une autre compagne. L'œuf « opaque » la fait paraître ainsi,
	 * pour juger sa planche telle qu'elle est.
	 */
	private static final EntityDataAccessor<Boolean> HOLOGRAM =
			SynchedEntityData.defineId(AuraEntity.class, EntityDataSerializers.BOOLEAN);

	/**
	 * Sa place : au-dessus de son projecteur (0.84.0). Une FapGPT sans projecteur (œuf,
	 * commande) prend pour place l'endroit où elle paraît.
	 */
	@org.jetbrains.annotations.Nullable
	private net.minecraft.world.phys.Vec3 place;

	public AuraEntity(EntityType<? extends AuraEntity> type, Level world) {
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
	private net.minecraft.core.BlockPos projecteur() {
		net.minecraft.core.BlockPos ici = this.blockPosition();
		net.minecraft.core.BlockPos meilleur = null;
		double mieux = Double.MAX_VALUE;
		for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(ici.offset(-16, -6, -16), ici.offset(16, 6, 16))) {
			if (this.level().getBlockState(p).getBlock() instanceof dev.pyrofetch.jn1.rift.AuraProjectorBlock) {
				double d = p.distSqr(ici);
				if (d < mieux) {
					mieux = d;
					meilleur = p.immutable();
				}
			}
		}
		return meilleur;
	}

	/** Tenue à sa place, sauf le temps d'un câlin, qui la rend ensuite d'un coup. */
	private void tenirSaPlace() {
		if (this.place == null) {
			net.minecraft.core.BlockPos p = this.projecteur();
			this.place = p != null
					? new net.minecraft.world.phys.Vec3(p.getX() + 0.5, p.getY() + dev.pyrofetch.jn1.rift.AuraProjectorBlock.HAUTEUR, p.getZ() + 0.5)
					: this.position();
		}
		if (this.getGesture() == CompanionGesture.HUG) {
			return;
		}
		if (this.getMode() == dev.pyrofetch.jn1.entity.JennyMode.FOLLOW) {
			this.setMode(dev.pyrofetch.jn1.entity.JennyMode.IDLE);
		}
		this.getNavigation().stop();
		if (this.distanceToSqr(this.place) > 0.01) {
			this.snapTo(this.place.x, this.place.y, this.place.z, this.getYRot(), this.getXRot());
		}
		this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
	}

	@Override
	public CompanionKind kind() {
		return CompanionKind.AURA;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DORMANT, true);
		builder.define(HOLOGRAM, true);
	}

	/** En veille : à peine visible, elle attend qu'on la trouve. */
	@Override
	public boolean estEnVeille() {
		return this.isDormant();
	}

	public boolean isDormant() {
		return this.entityData.get(DORMANT);
	}

	public void setDormant(boolean dormant) {
		this.entityData.set(DORMANT, dormant);
	}

	public boolean isHologram() {
		return this.entityData.get(HOLOGRAM);
	}

	/** Son portrait est un hologramme aussi, éveillé ou en veille comme elle. */
	@Override
	public void copierApparence(CompanionEntity copie) {
		super.copierApparence(copie);
		if (copie instanceof AuraEntity aura) {
			aura.entityData.set(HOLOGRAM, this.isHologram());
			aura.entityData.set(DORMANT, this.isDormant());
		}
	}

	private void éveiller(ServerPlayer joueur) {
		if (!this.isDormant()) {
			return;
		}
		this.setDormant(false);
		this.setCompanion(joueur);
		this.getLookControl().setLookAt(joueur);
		this.playGesture(CompanionGesture.WAVE);
		this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE,
				SoundSource.NEUTRAL, 0.8F, 1.4F);
		for (ServerPlayer témoin : ((ServerLevel) this.level()).getPlayers(
				j -> j.distanceToSqr(this) < 16 * 16)) {
			témoin.sendSystemMessage(Component.translatable("message.jn1.aura.wake"));
			DialogueEngine.setFlag(témoin, FieldNotebook.FLAG_AURA);
		}
	}

	@Override
	public void tick() {
		super.tick();
		Level monde = this.level();
		if (!monde.isClientSide()) {
			this.tenirSaPlace();
		}
		if (!monde.isClientSide() && this.isDormant() && this.tickCount % 10 == 0) {
			Player proche = monde.getNearestPlayer(this, ÉVEIL);
			if (proche instanceof ServerPlayer joueur && !joueur.isSpectator()) {
				this.éveiller(joueur);
			}
		}
		// Quelques grains de lumière s'échappent de la projection.
		if (monde.isClientSide() && this.isHologram() && this.random.nextInt(this.isDormant() ? 14 : 6) == 0) {
			monde.addParticle(new DustParticleOptions(0x73E6FF, 0.6F) /* 26.3 : couleur en entier, (0,45 ; 0,9 ; 1) */,
					this.getX() + (this.random.nextDouble() - 0.5) * 0.7,
					this.getY() + this.random.nextDouble() * 1.9,
					this.getZ() + (this.random.nextDouble() - 0.5) * 0.7, 0.0, 0.02, 0.0);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		// En veille, un toucher l'éveille, et rien d'autre.
		if (this.isDormant()) {
			if (player instanceof ServerPlayer joueur) {
				this.éveiller(joueur);
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel monde, DamageSource source) {
		return !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)
				&& !source.is(dev.pyrofetch.jn1.cum.DildoBlock.DÉCHIRURE);
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putBoolean("Dormant", this.isDormant());
		nbt.putBoolean("Hologram", this.isHologram());
		if (this.place != null) {
			nbt.putDouble("PlaceX", this.place.x);
			nbt.putDouble("PlaceY", this.place.y);
			nbt.putDouble("PlaceZ", this.place.z);
		}
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
		super.readAdditionalSaveData(nbt);
		this.setDormant(nbt.getBooleanOr("Dormant", true));
		this.entityData.set(HOLOGRAM, nbt.getBooleanOr("Hologram", true));
		java.util.Optional<Double> placeX = nbt.read("PlaceX", com.mojang.serialization.Codec.DOUBLE);
		if (placeX.isPresent()) {
			this.place = new net.minecraft.world.phys.Vec3(placeX.get(), nbt.getDoubleOr("PlaceY", 0.0), nbt.getDoubleOr("PlaceZ", 0.0));
		}
	}
}
