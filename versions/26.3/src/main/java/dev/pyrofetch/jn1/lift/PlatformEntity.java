package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Une plateforme en route : ses blocs, ôtés du monde au départ, portés par
 * cette entité, et reposés à l'arrivée avec leur apparence et leur groupe.
 *
 * <p>Sa boîte est l'emprise de la plateforme, un bloc d'épaisseur, solide
 * comme une barque : on y tient debout. Son mouvement accélère, file, puis
 * freine pour s'arrêter pile à l'étage ; c'est une fonction de l'heure du
 * monde ({@link #parcours}) : départ, arrivée et instant du départ sont
 * suivis, client et serveur calculent la même hauteur au même tick. Le
 * joueur debout dessus est porté par son propre client ({@code PlatformRide}),
 * le reste par le serveur.
 */
public class PlatformEntity extends Entity {
	/** Accélération, en blocs par tick² (4 m/s²) ; vitesse de croisière, en blocs par tick (6 m/s). */
	public static final double ACCÉLÉRATION = 0.01;
	public static final double VITESSE_MAX = 0.3;
	private static final int PERSISTANCE = 10;

	/** Le coin nord-ouest de l'emprise (x, z) ; y inutilisé. */
	private static final EntityDataAccessor<BlockPos> ANCRE = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.BLOCK_POS);
	/** La taille de l'emprise : x et z en blocs ; y inutilisé. */
	private static final EntityDataAccessor<BlockPos> TAILLE = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Float> DÉPART = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ARRIVÉE = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Long> DÉBUT = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.LONG);
	/**
	 * 26.3 : le jeu n'a plus de sérialiseur de NBT pour les données suivies ;
	 * celui-ci le remplace, enregistré par {@link #enregistrerSérialiseur()}.
	 */
	private static final EntityDataSerializer<CompoundTag> NBT = new EntityDataSerializer<>() {
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, CompoundTag> codec() {
			return ByteBufCodecs.COMPOUND_TAG;
		}

		@Override
		public CompoundTag copy(CompoundTag valeur) {
			return valeur.copy();
		}
	};
	/** Les blocs portés, pour le dessin : {@code Blocks} = [{x, z, Look}]. */
	private static final EntityDataAccessor<CompoundTag> BLOCS = SynchedEntityData.defineId(PlatformEntity.class, NBT);

	/** À appeler une fois à l'initialisation (client et serveur) : le sérialiseur doit avoir un identifiant. */
	public static void enregistrerSérialiseur() {
		FabricEntityDataRegistry.register(dev.pyrofetch.jn1.JN1.id("compound_tag"), NBT);
	}

	/** Un bloc porté : sa place dans l'emprise, son apparence. */
	public record Bloc(int x, int z, @Nullable BlockState apparence) {
	}

	@Nullable
	private UUID groupe;
	private Object clé = this;
	private boolean enRoute;
	/** Après l'arrivée, les ticks qu'elle reste encore, immobile sur ses blocs reposés. */
	private int adieu = -1;
	@Nullable
	private List<Bloc> blocsLus;

	public PlatformEntity(EntityType<? extends PlatformEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	/** Ôte la plateforme du monde et la fait partir vers ce niveau. */
	public static PlatformEntity lever(ServerLevel monde, Platforms.Plateforme p, int niveau) {
		PlatformEntity entité = new PlatformEntity(ModEntities.LIFT_PLATFORM, monde);
		ListTag liste = new ListTag();
		UUID groupe = null;
		for (BlockPos b : p.blocs()) {
			CompoundTag n = new CompoundTag();
			n.putInt("x", b.getX() - p.x0());
			n.putInt("z", b.getZ() - p.z0());
			if (monde.getBlockEntity(b) instanceof PlatformBlockEntity be) {
				if (be.apparence() != null) {
					n.put("Look", NbtUtils.writeBlockState(be.apparence()));
				}
				groupe = be.groupe();
			}
			liste.add(n);
		}
		for (BlockPos b : p.blocs()) {
			monde.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
		}
		CompoundTag blocs = new CompoundTag();
		blocs.put("Blocks", liste);
		entité.groupe = groupe;
		entité.clé = p.clé();
		entité.entityData.set(BLOCS, blocs);
		entité.entityData.set(ANCRE, new BlockPos(p.x0(), 0, p.z0()));
		entité.entityData.set(TAILLE, new BlockPos(p.x1() - p.x0() + 1, 0, p.z1() - p.z0() + 1));
		entité.entityData.set(DÉPART, (float) p.niveau());
		entité.entityData.set(ARRIVÉE, (float) niveau);
		entité.entityData.set(DÉBUT, monde.getGameTime());
		entité.enRoute = true;
		entité.recentrer(p.niveau());
		monde.addFreshEntity(entité);
		return entité;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(ANCRE, BlockPos.ZERO);
		builder.define(TAILLE, new BlockPos(1, 0, 1));
		builder.define(DÉPART, Float.NaN);
		builder.define(ARRIVÉE, Float.NaN);
		builder.define(DÉBUT, 0L);
		builder.define(BLOCS, new CompoundTag());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (BLOCS.equals(data)) {
			this.blocsLus = null;
		}
		if (ANCRE.equals(data) || TAILLE.equals(data)) {
			this.recentrer(this.getY());
		}
	}

	private void recentrer(double y) {
		BlockPos a = this.entityData.get(ANCRE);
		BlockPos t = this.entityData.get(TAILLE);
		this.setPos(a.getX() + t.getX() / 2.0, y, a.getZ() + t.getZ() / 2.0);
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		if (this.entityData == null) {
			return super.makeBoundingBox(position);
		}
		BlockPos a = this.entityData.get(ANCRE);
		BlockPos t = this.entityData.get(TAILLE);
		return new AABB(a.getX(), position.y, a.getZ(), a.getX() + t.getX(), position.y + 1.0, a.getZ() + t.getZ());
	}

	/** Arrivée, ses blocs reposés : elle ne fait plus qu'attendre de disparaître. */
	public boolean posée() {
		return this.adieu >= 0;
	}

	/** Posée, elle n'est plus qu'une ombre de ses blocs : rechargée, elle les reposerait une seconde fois. */
	@Override
	public boolean shouldBeSaved() {
		return !this.posée() && super.shouldBeSaved();
	}

	public Object clé() {
		return this.clé;
	}

	public BlockPos ancre() {
		return this.entityData.get(ANCRE);
	}

	public int nombreDeBlocs() {
		return this.blocs().size();
	}

	/** Les blocs portés, relus des données suivies (le client les dessine). */
	public List<Bloc> blocs() {
		if (this.blocsLus == null) {
			List<Bloc> blocs = new ArrayList<>();
			ListTag liste = this.entityData.get(BLOCS).getListOrEmpty("Blocks");
			for (int i = 0; i < liste.size(); i++) {
				CompoundTag n = liste.getCompoundOrEmpty(i);
				BlockState apparence = n.contains("Look")
						? NbtUtils.readBlockState(BuiltInRegistries.BLOCK, n.getCompoundOrEmpty("Look")) : null;
				blocs.add(new Bloc(n.getIntOr("x", 0), n.getIntOr("z", 0), apparence));
			}
			this.blocsLus = blocs;
		}
		return this.blocsLus;
	}

	// --- Le mouvement --------------------------------------------------------

	/** La distance parcourue au bout de {@code t} ticks d'un trajet de {@code d} blocs : accélérer, filer, freiner. */
	public static double parcours(double d, double t) {
		if (t <= 0 || d <= 0) {
			return 0;
		}
		double ta = VITESSE_MAX / ACCÉLÉRATION;
		double da = 0.5 * ACCÉLÉRATION * ta * ta;
		if (d < 2 * da) {
			ta = Math.sqrt(d / ACCÉLÉRATION);
			if (t < ta) {
				return 0.5 * ACCÉLÉRATION * t * t;
			}
			double r = Math.max(0, 2 * ta - t);
			return d - 0.5 * ACCÉLÉRATION * r * r;
		}
		double tc = (d - 2 * da) / VITESSE_MAX;
		if (t < ta) {
			return 0.5 * ACCÉLÉRATION * t * t;
		}
		if (t < ta + tc) {
			return da + VITESSE_MAX * (t - ta);
		}
		double r = Math.max(0, 2 * ta + tc - t);
		return d - 0.5 * ACCÉLÉRATION * r * r;
	}

	public static double durée(double d) {
		double ta = VITESSE_MAX / ACCÉLÉRATION;
		double da = 0.5 * ACCÉLÉRATION * ta * ta;
		return d < 2 * da ? 2 * Math.sqrt(d / ACCÉLÉRATION) : 2 * ta + (d - 2 * da) / VITESSE_MAX;
	}

	/** Le bas de la plateforme à cet instant du monde. */
	public double basÀ(double temps) {
		float départ = this.entityData.get(DÉPART);
		float arrivée = this.entityData.get(ARRIVÉE);
		if (Float.isNaN(départ) || Float.isNaN(arrivée)) {
			return this.getY();
		}
		double d = Math.abs(arrivée - départ);
		return départ + Math.signum(arrivée - départ) * parcours(d, temps - this.entityData.get(DÉBUT));
	}

	/** Le dessus de la plateforme à la fin du tick en cours : le joueur porté s'y pose. */
	public double dessusEnFinDeTick() {
		return this.basÀ(this.level().getGameTime()) + 1.0;
	}

	public boolean enMouvement() {
		float départ = this.entityData.get(DÉPART);
		float arrivée = this.entityData.get(ARRIVÉE);
		return !Float.isNaN(arrivée) && this.level().getGameTime() - this.entityData.get(DÉBUT)
				< durée(Math.abs(arrivée - départ));
	}

	/** 1 si elle monte, -1 si elle descend. */
	public int sens() {
		return (int) Math.signum(this.entityData.get(ARRIVÉE) - this.entityData.get(DÉPART));
	}

	public int destination() {
		float a = this.entityData.get(ARRIVÉE);
		return Float.isNaN(a) ? Mth.floor(this.getY()) : Math.round(a);
	}

	// --- Ce qu'elle porte ----------------------------------------------------

	/** Vrai si cette entité se tient sur la plateforme, dans son emprise. */
	public boolean porte(Entity entité) {
		if (entité == this) {
			return false;
		}
		AABB e = this.getBoundingBox();
		if (entité.getX() < e.minX || entité.getX() > e.maxX || entité.getZ() < e.minZ || entité.getZ() > e.maxZ) {
			return false;
		}
		double pieds = entité.getY() - e.maxY;
		return pieds > -0.7 && pieds < 1.0;
	}

	@Nullable
	public static PlatformEntity sous(Entity entité) {
		for (PlatformEntity p : entité.level().getEntities(ModEntities.LIFT_PLATFORM,
				entité.getBoundingBox().inflate(2.0, 3.0, 2.0), Entity::isAlive)) {
			if (p.porte(entité)) {
				return p;
			}
		}
		return null;
	}

	private void porterCeQuiSeTientDessus() {
		AABB e = this.getBoundingBox();
		double dessus = e.maxY;
		for (Entity entité : this.level().getEntities(this,
				new AABB(e.minX, dessus - 0.8, e.minZ, e.maxX, dessus + 0.6, e.maxZ))) {
			if (entité instanceof Player || entité.isPassenger() || entité.noPhysics || entité.getDeltaMovement().y > 0.1) {
				continue;
			}
			entité.setPos(entité.getX(), dessus, entité.getZ());
			entité.setDeltaMovement(entité.getDeltaMovement().x, 0.0, entité.getDeltaMovement().z);
			entité.setOnGround(true);
			entité.fallDistance = 0.0F;
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.setPos(this.getX(), this.basÀ(this.level().getGameTime()), this.getZ());
		boolean roule = this.enMouvement();
		this.setDeltaMovement(0.0, roule ? this.sens() * 0.1 : 0.0, 0.0);
		if (this.level().isClientSide()) {
			return;
		}
		if (roule) {
			this.porterCeQuiSeTientDessus();
		} else if (this.enRoute) {
			this.enRoute = false;
			this.poser();
		} else if (this.adieu > 0 && --this.adieu == 0) {
			this.discard();
		}
	}

	/** L'arrivée : les blocs reprennent leur place, avec leur apparence et leur groupe. */
	private void poser() {
		Level monde = this.level();
		int niveau = this.destination();
		BlockPos a = this.ancre();
		BlockPos premier = null;
		for (Bloc b : this.blocs()) {
			BlockPos p = new BlockPos(a.getX() + b.x(), niveau, a.getZ() + b.z());
			if (!monde.getBlockState(p).canBeReplaced()) {
				// Quelque chose a pris sa place en chemin : le bloc tombe en objet.
				monde.addFreshEntity(new ItemEntity(monde, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5,
						new ItemStack(ModBlocks.LIFT_PLATFORM)));
				continue;
			}
			monde.setBlock(p, ModBlocks.LIFT_PLATFORM.defaultBlockState(), Block.UPDATE_ALL);
			if (monde.getBlockEntity(p) instanceof PlatformBlockEntity be) {
				be.reprendre(b.apparence(), this.groupe);
			}
			if (premier == null) {
				premier = p;
			}
		}
		// Elle reste encore un peu, immobile, sur ses blocs : l'horloge du client a
		// un tick ou deux de retard, et sa plateforme à lui roule encore. Retirée
		// aussitôt, elle laissait le joueur dans les blocs reposés, qu'il
		// traversait jusqu'au fond.
		this.adieu = PERSISTANCE;
		monde.playSound(null, BlockPos.containing(this.getX(), niveau + 1.5, this.getZ()),
				ModSounds.ELEVATOR_DING, SoundSource.BLOCKS, 0.9F, 1.0F);
		if (premier != null) {
			Platforms.alimenter(monde, Platforms.depuisBlocs(monde, premier), true);
		}
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity autre) {
		return true;
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
	public boolean hurtServer(ServerLevel monde, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean allowVehicles) {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passager) {
		return false;
	}

	// 26.3 : la boîte de culling est celle du rendu (EntityRenderer.getBoundingBoxForCulling),
	// qui gonfle déjà la boîte de l'entité de 0,5 : rien à surcharger ici.

	/**
	 * Le client calcule la hauteur lui-même ({@link #basÀ}) : il ne reprend du serveur que x et z.
	 * 26.3 : {@code lerpTo} est remplacé par un gestionnaire d'interpolation ; celui du jeu
	 * pour une entité ordinaire (aucune interpolation, la position posée telle quelle).
	 */
	@Override
	protected InterpolationHandler createInterpolationHandler() {
		return new InterpolationHandler.NoOpInterpolationHandler() {
			@Override
			public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
				if (position != null && !Float.isNaN(PlatformEntity.this.entityData.get(ARRIVÉE))) {
					Vec3 fin = position.endPosition();
					PlatformEntity.this.setPos(fin.x, PlatformEntity.this.getY(), fin.z);
					return true;
				}
				return false;
			}
		};
	}

	@Override
	protected void readAdditionalSaveData(ValueInput nbt) {
		this.entityData.set(ANCRE, nbt.read("Anchor", BlockPos.CODEC).orElse(BlockPos.ZERO));
		this.entityData.set(TAILLE, nbt.read("Size", BlockPos.CODEC).orElse(new BlockPos(1, 0, 1)));
		this.entityData.set(BLOCS, nbt.read("Carried", CompoundTag.CODEC).orElseGet(CompoundTag::new));
		this.groupe = nbt.read("Group", UUIDUtil.CODEC).orElse(null);
		this.clé = this.groupe != null ? this.groupe : this;
		// Rechargée : elle se pose aussitôt à l'étage qu'elle visait.
		float niveau = nbt.getFloatOr("Target", 0.0F);
		this.entityData.set(DÉPART, niveau);
		this.entityData.set(ARRIVÉE, niveau);
		this.enRoute = true;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput nbt) {
		nbt.store("Anchor", BlockPos.CODEC, this.ancre());
		nbt.store("Size", BlockPos.CODEC, this.entityData.get(TAILLE));
		nbt.store("Carried", CompoundTag.CODEC, this.entityData.get(BLOCS));
		nbt.storeNullable("Group", UUIDUtil.CODEC, this.groupe);
		nbt.putFloat("Target", this.destination());
	}
}
