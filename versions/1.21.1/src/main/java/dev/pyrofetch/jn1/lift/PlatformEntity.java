package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
	private static final TrackedData<BlockPos> ANCRE = DataTracker.registerData(PlatformEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
	/** La taille de l'emprise : x et z en blocs ; y inutilisé. */
	private static final TrackedData<BlockPos> TAILLE = DataTracker.registerData(PlatformEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
	private static final TrackedData<Float> DÉPART = DataTracker.registerData(PlatformEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> ARRIVÉE = DataTracker.registerData(PlatformEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Long> DÉBUT = DataTracker.registerData(PlatformEntity.class, TrackedDataHandlerRegistry.LONG);
	/** Les blocs portés, pour le dessin : {@code Blocks} = [{x, z, Look}]. */
	private static final TrackedData<NbtCompound> BLOCS = DataTracker.registerData(PlatformEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);

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

	public PlatformEntity(EntityType<? extends PlatformEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	/** Ôte la plateforme du monde et la fait partir vers ce niveau. */
	public static PlatformEntity lever(ServerWorld monde, Platforms.Plateforme p, int niveau) {
		PlatformEntity entité = new PlatformEntity(ModEntities.LIFT_PLATFORM, monde);
		NbtList liste = new NbtList();
		UUID groupe = null;
		for (BlockPos b : p.blocs()) {
			NbtCompound n = new NbtCompound();
			n.putInt("x", b.getX() - p.x0());
			n.putInt("z", b.getZ() - p.z0());
			if (monde.getBlockEntity(b) instanceof PlatformBlockEntity be) {
				if (be.apparence() != null) {
					n.put("Look", NbtHelper.fromBlockState(be.apparence()));
				}
				groupe = be.groupe();
			}
			liste.add(n);
		}
		for (BlockPos b : p.blocs()) {
			monde.setBlockState(b, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.FORCE_STATE);
		}
		NbtCompound blocs = new NbtCompound();
		blocs.put("Blocks", liste);
		entité.groupe = groupe;
		entité.clé = p.clé();
		entité.dataTracker.set(BLOCS, blocs);
		entité.dataTracker.set(ANCRE, new BlockPos(p.x0(), 0, p.z0()));
		entité.dataTracker.set(TAILLE, new BlockPos(p.x1() - p.x0() + 1, 0, p.z1() - p.z0() + 1));
		entité.dataTracker.set(DÉPART, (float) p.niveau());
		entité.dataTracker.set(ARRIVÉE, (float) niveau);
		entité.dataTracker.set(DÉBUT, monde.getTime());
		entité.enRoute = true;
		entité.recentrer(p.niveau());
		monde.spawnEntity(entité);
		return entité;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ANCRE, BlockPos.ORIGIN);
		builder.add(TAILLE, new BlockPos(1, 0, 1));
		builder.add(DÉPART, Float.NaN);
		builder.add(ARRIVÉE, Float.NaN);
		builder.add(DÉBUT, 0L);
		builder.add(BLOCS, new NbtCompound());
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (BLOCS.equals(data)) {
			this.blocsLus = null;
		}
		if (ANCRE.equals(data) || TAILLE.equals(data)) {
			this.recentrer(this.getY());
		}
	}

	private void recentrer(double y) {
		BlockPos a = this.dataTracker.get(ANCRE);
		BlockPos t = this.dataTracker.get(TAILLE);
		this.setPosition(a.getX() + t.getX() / 2.0, y, a.getZ() + t.getZ() / 2.0);
	}

	@Override
	protected Box calculateBoundingBox() {
		if (this.dataTracker == null) {
			return super.calculateBoundingBox();
		}
		BlockPos a = this.dataTracker.get(ANCRE);
		BlockPos t = this.dataTracker.get(TAILLE);
		return new Box(a.getX(), this.getY(), a.getZ(), a.getX() + t.getX(), this.getY() + 1.0, a.getZ() + t.getZ());
	}

	/** Arrivée, ses blocs reposés : elle ne fait plus qu'attendre de disparaître. */
	public boolean posée() {
		return this.adieu >= 0;
	}

	/** Posée, elle n'est plus qu'une ombre de ses blocs : rechargée, elle les reposerait une seconde fois. */
	@Override
	public boolean shouldSave() {
		return !this.posée() && super.shouldSave();
	}

	public Object clé() {
		return this.clé;
	}

	public BlockPos ancre() {
		return this.dataTracker.get(ANCRE);
	}

	public int nombreDeBlocs() {
		return this.blocs().size();
	}

	/** Les blocs portés, relus des données suivies (le client les dessine). */
	public List<Bloc> blocs() {
		if (this.blocsLus == null) {
			List<Bloc> blocs = new ArrayList<>();
			NbtList liste = this.dataTracker.get(BLOCS).getList("Blocks", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < liste.size(); i++) {
				NbtCompound n = liste.getCompound(i);
				BlockState apparence = n.contains("Look")
						? NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), n.getCompound("Look")) : null;
				blocs.add(new Bloc(n.getInt("x"), n.getInt("z"), apparence));
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
		float départ = this.dataTracker.get(DÉPART);
		float arrivée = this.dataTracker.get(ARRIVÉE);
		if (Float.isNaN(départ) || Float.isNaN(arrivée)) {
			return this.getY();
		}
		double d = Math.abs(arrivée - départ);
		return départ + Math.signum(arrivée - départ) * parcours(d, temps - this.dataTracker.get(DÉBUT));
	}

	/** Le dessus de la plateforme à la fin du tick en cours : le joueur porté s'y pose. */
	public double dessusEnFinDeTick() {
		return this.basÀ(this.getWorld().getTime()) + 1.0;
	}

	public boolean enMouvement() {
		float départ = this.dataTracker.get(DÉPART);
		float arrivée = this.dataTracker.get(ARRIVÉE);
		return !Float.isNaN(arrivée) && this.getWorld().getTime() - this.dataTracker.get(DÉBUT)
				< durée(Math.abs(arrivée - départ));
	}

	/** 1 si elle monte, -1 si elle descend. */
	public int sens() {
		return (int) Math.signum(this.dataTracker.get(ARRIVÉE) - this.dataTracker.get(DÉPART));
	}

	public int destination() {
		float a = this.dataTracker.get(ARRIVÉE);
		return Float.isNaN(a) ? MathHelper.floor(this.getY()) : Math.round(a);
	}

	// --- Ce qu'elle porte ----------------------------------------------------

	/** Vrai si cette entité se tient sur la plateforme, dans son emprise. */
	public boolean porte(Entity entité) {
		if (entité == this) {
			return false;
		}
		Box e = this.getBoundingBox();
		if (entité.getX() < e.minX || entité.getX() > e.maxX || entité.getZ() < e.minZ || entité.getZ() > e.maxZ) {
			return false;
		}
		double pieds = entité.getY() - e.maxY;
		return pieds > -0.7 && pieds < 1.0;
	}

	@Nullable
	public static PlatformEntity sous(Entity entité) {
		for (PlatformEntity p : entité.getWorld().getEntitiesByType(ModEntities.LIFT_PLATFORM,
				entité.getBoundingBox().expand(2.0, 3.0, 2.0), Entity::isAlive)) {
			if (p.porte(entité)) {
				return p;
			}
		}
		return null;
	}

	private void porterCeQuiSeTientDessus() {
		Box e = this.getBoundingBox();
		double dessus = e.maxY;
		for (Entity entité : this.getWorld().getOtherEntities(this,
				new Box(e.minX, dessus - 0.8, e.minZ, e.maxX, dessus + 0.6, e.maxZ))) {
			if (entité instanceof PlayerEntity || entité.hasVehicle() || entité.noClip || entité.getVelocity().y > 0.1) {
				continue;
			}
			entité.setPosition(entité.getX(), dessus, entité.getZ());
			entité.setVelocity(entité.getVelocity().x, 0.0, entité.getVelocity().z);
			entité.setOnGround(true);
			entité.fallDistance = 0.0F;
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.setPosition(this.getX(), this.basÀ(this.getWorld().getTime()), this.getZ());
		boolean roule = this.enMouvement();
		this.setVelocity(0.0, roule ? this.sens() * 0.1 : 0.0, 0.0);
		if (this.getWorld().isClient()) {
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
		World monde = this.getWorld();
		int niveau = this.destination();
		BlockPos a = this.ancre();
		BlockPos premier = null;
		for (Bloc b : this.blocs()) {
			BlockPos p = new BlockPos(a.getX() + b.x(), niveau, a.getZ() + b.z());
			if (!monde.getBlockState(p).isReplaceable()) {
				// Quelque chose a pris sa place en chemin : le bloc tombe en objet.
				monde.spawnEntity(new ItemEntity(monde, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5,
						new ItemStack(ModBlocks.LIFT_PLATFORM)));
				continue;
			}
			monde.setBlockState(p, ModBlocks.LIFT_PLATFORM.getDefaultState(), Block.NOTIFY_ALL);
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
		monde.playSound(null, BlockPos.ofFloored(this.getX(), niveau + 1.5, this.getZ()),
				ModSounds.ELEVATOR_DING, SoundCategory.BLOCKS, 0.9F, 1.0F);
		if (premier != null) {
			Platforms.alimenter(monde, Platforms.depuisBlocs(monde, premier), true);
		}
	}

	@Override
	public boolean isCollidable() {
		return true;
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
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passager) {
		return false;
	}

	@Override
	public Box getVisibilityBoundingBox() {
		return this.getBoundingBox().expand(0.5);
	}

	/** Le client calcule la hauteur lui-même ({@link #basÀ}) : il ne reprend du serveur que x et z. */
	@Override
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch,
	                                           int interpolationSteps) {
		if (!Float.isNaN(this.dataTracker.get(ARRIVÉE))) {
			this.setPosition(x, this.getY(), z);
		} else {
			super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps);
		}
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		this.dataTracker.set(ANCRE, NbtHelper.toBlockPos(nbt, "Anchor").orElse(BlockPos.ORIGIN));
		this.dataTracker.set(TAILLE, NbtHelper.toBlockPos(nbt, "Size").orElse(new BlockPos(1, 0, 1)));
		this.dataTracker.set(BLOCS, nbt.getCompound("Carried"));
		this.groupe = nbt.containsUuid("Group") ? nbt.getUuid("Group") : null;
		this.clé = this.groupe != null ? this.groupe : this;
		// Rechargée : elle se pose aussitôt à l'étage qu'elle visait.
		float niveau = nbt.getFloat("Target");
		this.dataTracker.set(DÉPART, niveau);
		this.dataTracker.set(ARRIVÉE, niveau);
		this.enRoute = true;
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.put("Anchor", NbtHelper.fromBlockPos(this.ancre()));
		nbt.put("Size", NbtHelper.fromBlockPos(this.dataTracker.get(TAILLE)));
		nbt.put("Carried", this.dataTracker.get(BLOCS));
		if (this.groupe != null) {
			nbt.putUuid("Group", this.groupe);
		}
		nbt.putFloat("Target", this.destination());
	}
}
