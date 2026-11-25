package dev.pyrofetch.jn1.gaster;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.lab.VoidLabs;
import dev.pyrofetch.jn1.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Gaster, qui attend derrière l'arbre du vide.
 *
 * <p>Il apparaît derrière l'arbre — celui qu'on regarde en arrivant dans le
 * vide par une porte — une fois sur trois ; jamais deux fois tant qu'il n'a pas
 * disparu ; interagir avec lui fait sauter un œuf, le plus ordinaire du jeu, et
 * il disparaît.
 *
 * <p>Son vrai modèle, lisse, dessiné par {@code GasterRenderer} depuis le
 * maillage exporté de Blender (la version en cubes a été abandonnée en 0.56.1).
 * Le modèle est celui de Skyguy555, à créditer.
 *
 * <p>Rien ne le blesse, sauf le vide sous le monde et {@code /kill} : il n'est
 * pas là pour être combattu.
 *
 * <p><strong>L'épie</strong> : paru derrière l'arbre, il se tient
 * dans le coin d'une racine, tourné vers le chemin, et s'en penche — la tête
 * qui dépasse, inclinée, les doigts agrippés à l'arête ({@code peek}, pose
 * calculée contre les blocs par {@code tools/gen_gaster.py}). Le corps ne
 * tourne plus : seule la tête suit le joueur ({@code GasterModel}). Sa boîte
 * suit la pose, pour qu'on puisse cliquer sur la tête qui dépasse.
 *
 * <p><strong>Il se cache</strong> : un
 * joueur debout à moins de {@link #APPROCHE} blocs de sa place, et il passe
 * derrière le tronc, du côté opposé au joueur, en pose de repos, le dos au
 * bois, et il y reste : on peut venir lui parler. Il ne revient épier que
 * quand plus personne n'est à {@link #RETOUR} blocs **et** que personne ne
 * regarde vers sa place. Accroupi, on ne l'effraie pas : c'est ainsi qu'on
 * l'approche assez pour le toucher (il faut être à trois blocs).
 *
 * <p><strong>L'adieu</strong> (0.56.1) : en sortant de sa boutique, il est posé juste devant le joueur, les
 * yeux à hauteur des siens ({@link #seMontrer}), et s'en va {@link #DURÉE_ADIEU}
 * ticks plus tard. Le client le pose là sans glisser depuis l'arbre
 * ({@link #updateTrackedPositionAndAngles}).
 */
public class GasterEntity extends PathAwareEntity implements GeoEntity {
	/** Il nous fixe, sorti de sa boutique, juste avant de disparaître. */
	private static final TrackedData<Boolean> ADIEU =
			DataTracker.registerData(GasterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Les ticks de danse qui restent (« six seven », voir {@link SixSeven}). */
	private static final TrackedData<Integer> DANSE =
			DataTracker.registerData(GasterEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final RawAnimation REPOS = RawAnimation.begin().thenLoop("animation.gaster.idle");
	private static final RawAnimation SIX_SEVEN = RawAnimation.begin().thenLoop("animation.gaster.six_seven");
	private static final RawAnimation ÉPIE = RawAnimation.begin().thenLoop("animation.gaster.peek");
	private static final RawAnimation BOUTIQUE = RawAnimation.begin().thenLoop("animation.gaster.shop");
	/** Il épie derrière l'arbre : corps immobile, tourné vers le chemin. */
	private static final TrackedData<Boolean> ÉPIANT =
			DataTracker.registerData(GasterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Caché derrière le tronc, en pose de repos : il épiait, quelqu'un s'est approché. */
	private static final TrackedData<Boolean> CACHÉ =
			DataTracker.registerData(GasterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Vers l'est, d'où l'on arrive par le chemin. */
	private static final float LACET_ÉPIE = -90.0F;
	/** Un joueur debout plus près que cela de sa place, et il se cache. */
	private static final double APPROCHE = 5.0;
	/** Il ne ressort que quand plus personne n'est à cette distance. */
	private static final double RETOUR = 8.0;
	/** Ni quand quelqu'un regarde vers sa place, à moins de ce demi-angle (le champ de vision du jeu). */
	private static final double COS_REGARD = Math.cos(Math.toRadians(55.0));
	/** Ceux à qui il se cache : les joueurs à cette distance du tronc. */
	private static final double TÉMOINS = 32.0;
	/**
	 * Ce qui dépasse de lui en pose de repos, mesuré sur le maillage animé : le
	 * bas de la robe s'évase à 0,85 bloc de l'axe, les mains à 0,85 devant lui,
	 * la tête à 0,4. Des points tout autour, à ces hauteurs et ces rayons : il
	 * est vu si l'un d'eux l'est.
	 */
	private static final double[][] SILHOUETTE = {
			{0.1, 0.85}, {0.4, 0.85}, {0.9, 0.4}, {1.4, 0.85}, {1.85, 0.8}, {2.2, 0.4}, {2.5, 0.35}};
	/** Il ne s'approche pas plus que cela du bois : sa robe y entrerait. */
	private static final double ROBE = 0.85;
	/** Trois allers-retours et demi de la balance : la danse dure ce qu'on met à le dire deux fois. */
	private static final int DURÉE_DANSE = 72;

	/** Ce qu'il nous fixe avant de s'en aller : moins d'une seconde. */
	private static final int DURÉE_ADIEU = 18;
	/**
	 * De nos yeux aux siens, pendant l'adieu : son masque, une main plus en
	 * avant, est alors à deux dixièmes de bloc de l'œil et remplit l'écran.
	 */
	private static final double PRÈS = 0.32;
	/**
	 * Le milieu de ses yeux, au-dessus de ses pieds, en avant de son axe et sur
	 * sa gauche (sa tête penche de ce côté au repos) : réglé en jeu pour
	 * que son regard tombe au milieu de l'écran.
	 */
	private static final double HAUTEUR_YEUX = 2.35;
	/**
	 * En avant de son axe d'après les os (0,3) ; mais mesuré en jeu, son
	 * visage était 0,4 bloc plus loin que voulu (la pose de repos recule la
	 * tête) : d'où ce chiffre.
	 */
	private static final double AVANCE_YEUX = -0.1;
	private static final double CÔTÉ_YEUX = 0.17;

	/** Une chance sur trois à chaque arrivée par une porte. */
	private static final int CHANCE = 3;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	/** Sa boutique : l'étal tiré à la première ouverture, et ce qui en est vendu. */
	@Nullable
	private List<GasterShop.Article> étal;
	private int vendus;
	/** Vrai pour la copie dessinée dans la boutique : elle joue {@code shop}. */
	private boolean boutique;
	/** Les ticks d'adieu qui restent, au serveur. */
	private int adieu;

	/** L'ancre du laboratoire dont il hante l'arbre ; nulle s'il a été invoqué à la main. */
	@Nullable
	private BlockPos ancre;
	/** Un Gaster d'avant l'épie, qui attend encore à l'ancienne place : à replacer au premier tick. */
	private boolean àReplacer;

	public GasterEntity(EntityType<? extends GasterEntity> type, World world) {
		super(type, world);
		this.setNoGravity(true);
		this.setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	/**
	 * Le fait paraître derrière l'arbre du laboratoire de cette ancre.
	 *
	 * @param forcé sans tirage au sort (commande d'épreuve)
	 * @return lui, ou rien s'il n'est pas venu — tirage perdu, ou déjà là
	 */
	@Nullable
	public static GasterEntity apparaître(ServerWorld vide, BlockPos ancre, boolean forcé) {
		GasterState état = GasterState.of(vide);
		if (état.attend(ancre) || (!forcé && vide.getRandom().nextInt(CHANCE) != 0)) {
			return null;
		}
		GasterEntity gaster = ModEntities.GASTER.create(vide);
		if (gaster == null) {
			return null;
		}
		gaster.ancre = ancre.toImmutable();
		gaster.épier();
		vide.spawnEntity(gaster);
		état.marquer(ancre);
		JN1.LOGGER.info("Gaster attend derrière l'arbre ({}, {}).", ancre.getX(), ancre.getZ());
		return gaster;
	}

	/** À sa place derrière l'arbre, tourné vers le chemin, et il épie. */
	private void épier() {
		Vec3d place = VoidLabs.derrièreLArbre(this.ancre);
		this.dataTracker.set(ÉPIANT, true);
		this.dataTracker.set(CACHÉ, false);
		this.refreshPositionAndAngles(place.x, place.y, place.z, LACET_ÉPIE, 0.0F);
		this.setHeadYaw(LACET_ÉPIE);
		this.setBodyYaw(LACET_ÉPIE);
	}

	/** Il épie, penché hors de la racine (et non caché derrière le tronc). */
	public boolean épie() {
		return this.dataTracker.get(ÉPIANT) && !this.dataTracker.get(CACHÉ);
	}

	public boolean caché() {
		return this.dataTracker.get(ÉPIANT) && this.dataTracker.get(CACHÉ);
	}

	/** À l'arbre — qu'il épie ou s'y cache —, et ne danse pas : son corps ne suit pas sa tête. */
	private boolean corpsFixe() {
		return (this.dataTracker.get(ÉPIANT) && this.dataTracker.get(DANSE) <= 0) || this.enAdieu();
	}

	/** Le cap de son corps à l'arbre : vers le chemin quand il épie, le dos au tronc caché. */
	private float lacetFixe() {
		return this.caché() || this.enAdieu() ? this.getYaw() : LACET_ÉPIE;
	}

	/** Un joueur qui compte : vivant, pas spectateur. */
	private static boolean compte(PlayerEntity joueur) {
		return joueur.isAlive() && !joueur.isSpectator();
	}

	private static double distanceAuSol(Vec3d a, Vec3d b) {
		return Math.hypot(a.x - b.x, a.z - b.z);
	}

	/**
	 * Épie-t-il, ou se cache-t-il ? Décidé au serveur, à chaque tick : un joueur
	 * debout trop près le fait passer derrière le tronc ; il n'en ressort que
	 * quand tous sont loin et que personne ne regarde vers sa place.
	 */
	private void guetter(ServerWorld monde) {
		Vec3d place = VoidLabs.derrièreLArbre(this.ancre);
		PlayerEntity proche = null;
		double meilleure = Double.MAX_VALUE;
		boolean tropPrès = false, regardé = false;
		for (PlayerEntity joueur : monde.getPlayers()) {
			if (!compte(joueur)) {
				continue;
			}
			double d = distanceAuSol(joueur.getPos(), place);
			if (d < meilleure) {
				meilleure = d;
				proche = joueur;
			}
			tropPrès |= d < RETOUR;
			Vec3d vers = place.add(0, 1.2, 0).subtract(joueur.getEyePos());
			regardé |= vers.lengthSquared() < 48 * 48
					&& joueur.getRotationVec(1.0F).dotProduct(vers.normalize()) > COS_REGARD;
		}
		if (!this.caché()) {
			if (proche != null && meilleure < APPROCHE && !proche.isSneaking()) {
				this.seCacher(monde, proche, true);
			}
			return;
		}
		// Caché, il reste où il est : on peut venir lui parler. Il ne ressort épier que
		// quand plus personne n'est près ni ne regarde vers sa place.
		if (!tropPrès && !regardé) {
			this.épier();
		}
	}

	/**
	 * Derrière le tronc, là où aucun joueur proche ne le voit, le dos au bois.
	 * Déjà caché, il ne bouge que si quelqu'un l'aperçoit — le joueur a tourné
	 * autour de l'arbre — : le trajet d'une cachette à l'autre passe derrière
	 * le tronc. Les places essayées tournent autour du tronc, en partant du
	 * côté opposé au joueur le plus proche ; « vu » se juge par des rayons des
	 * yeux de chaque joueur à sa silhouette, contre les vrais blocs. Faute de
	 * place où personne ne le voit, il prend la moins vue.
	 */
	private void seCacher(ServerWorld monde, PlayerEntity joueur, boolean dePlace) {
		Vec3d centre = VoidLabs.centreDuTronc(this.ancre);
		List<PlayerEntity> témoins = new ArrayList<>();
		for (PlayerEntity p : monde.getPlayers()) {
			if (compte(p) && distanceAuSol(p.getPos(), centre) < TÉMOINS) {
				témoins.add(p);
			}
		}
		if (!dePlace && (this.age % 2 != 0 || vus(monde, this.getPos(), témoins) == 0)) {
			return;
		}
		double idéal = Math.atan2(centre.z - joueur.getZ(), centre.x - joueur.getX());
		Vec3d meilleure = null;
		int moinsVu = Integer.MAX_VALUE;
		for (int pas = 0; pas <= 12 && moinsVu > 0; pas++) {
			// 0, +15°, -15°, +30°, -30°… jusqu'à ±90° du côté opposé au joueur.
			double angle = idéal + Math.toRadians(15.0 * ((pas + 1) / 2) * (pas % 2 == 0 ? -1 : 1));
			for (double rayon : new double[] {2.4, 2.8, 3.2, 3.6}) {
				double x = centre.x + Math.cos(angle) * rayon, z = centre.z + Math.sin(angle) * rayon;
				Vec3d pied = new Vec3d(x, centre.y, z);
				Box robe = new Box(x - ROBE, centre.y, z - ROBE, x + ROBE, centre.y + 2.5, z + ROBE);
				if (!monde.isSpaceEmpty(robe) || monde.getBlockState(BlockPos.ofFloored(x, centre.y - 1, z)).isAir()) {
					continue;
				}
				int vus = vus(monde, pied, témoins);
				if (vus < moinsVu) {
					moinsVu = vus;
					meilleure = pied;
					if (vus == 0) {
						break;
					}
				}
			}
		}
		if (meilleure == null) {
			return;
		}
		// Dos au tronc : il regarde en s'en éloignant.
		float lacet = (float) Math.toDegrees(Math.atan2(-(meilleure.x - centre.x), meilleure.z - centre.z));
		this.dataTracker.set(CACHÉ, true);
		this.refreshPositionAndAngles(meilleure.x, meilleure.y, meilleure.z, lacet, 0.0F);
		this.setHeadYaw(lacet);
		this.setBodyYaw(lacet);
	}

	/** Combien de points de sa silhouette, posé là, les témoins voient-ils ? */
	private int vus(ServerWorld monde, Vec3d pied, List<PlayerEntity> témoins) {
		int vus = 0;
		for (PlayerEntity témoin : témoins) {
			Vec3d œil = témoin.getEyePos();
			for (double[] couche : SILHOUETTE) {
				for (int k = 0; k < 8; k++) {
					double a = k * Math.PI / 4;
					Vec3d point = pied.add(Math.cos(a) * couche[1], couche[0], Math.sin(a) * couche[1]);
					BlockHitResult choc = monde.raycast(new RaycastContext(œil, point,
							RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, témoin));
					if (choc.getType() == HitResult.Type.MISS) {
						vus++;
					}
				}
			}
		}
		return vus;
	}

	/** Il fait la danse du « six seven », tourné vers qui l'a dit. */
	public void danser(net.minecraft.server.network.ServerPlayerEntity joueur) {
		this.getLookControl().lookAt(joueur);
		this.dataTracker.set(DANSE, DURÉE_DANSE);
	}

	/** Il nous fixe avant de partir : pupilles allumées, la tête qui suit. */
	public boolean enAdieu() {
		return this.dataTracker.get(ADIEU);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ADIEU, false);
		builder.add(DANSE, 0);
		builder.add(ÉPIANT, false);
		builder.add(CACHÉ, false);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (ÉPIANT.equals(data) || CACHÉ.equals(data)) {
			this.setBoundingBox(this.calculateBoundingBox());
		}
		// Le paquet de position arrive d'ordinaire juste avant : le glissement
		// depuis l'arbre est déjà entamé, on le termine d'un coup.
		if (ADIEU.equals(data) && this.getWorld().isClient() && this.enAdieu() && this.bodyTrackingIncrements > 0) {
			this.poserIci(this.serverX, this.serverY, this.serverZ, (float) this.serverYaw, (float) this.serverPitch);
		}
	}

	/**
	 * Pendant l'adieu, le client le pose où le serveur le dit, sans les trois
	 * ticks de glissement du jeu : il ne vient pas de l'arbre, il est là.
	 */
	@Override
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch,
			int interpolationSteps) {
		if (this.enAdieu()) {
			this.poserIci(x, y, z, yaw, pitch);
			return;
		}
		super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps);
	}

	@Override
	public void updateTrackedHeadRotation(float yaw, int interpolationSteps) {
		if (this.enAdieu()) {
			this.setHeadYaw(yaw);
			this.prevHeadYaw = yaw;
			this.headTrackingIncrements = 0;
			return;
		}
		super.updateTrackedHeadRotation(yaw, interpolationSteps);
	}

	private void poserIci(double x, double y, double z, float yaw, float pitch) {
		this.setPosition(x, y, z);
		this.setYaw(yaw);
		this.setPitch(pitch);
		this.setBodyYaw(yaw);
		this.setHeadYaw(yaw);
		this.resetPosition();
		this.prevBodyYaw = yaw;
		this.prevHeadYaw = yaw;
		this.bodyTrackingIncrements = 0;
		this.headTrackingIncrements = 0;
	}

	/**
	 * Penché hors de la racine, sa tête et ses mains sont à plus d'un bloc de
	 * ses pieds, du côté droit : la boîte s'étend jusqu'à elles (en avant
	 * -0,4..0,75, à droite -0,4..1,35), sans quoi la tête qu'on voit ne se
	 * clique pas — le reste de la boîte est caché derrière le bois.
	 */
	@Override
	protected net.minecraft.util.math.Box calculateBoundingBox() {
		if (!this.épie()) {
			return super.calculateBoundingBox();
		}
		double rad = Math.toRadians(LACET_ÉPIE);
		double ax = -Math.sin(rad), az = Math.cos(rad);   // en avant
		double dx = -Math.cos(rad), dz = -Math.sin(rad);  // à droite
		double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
		for (double a : new double[] {-0.4, 0.75}) {
			for (double d : new double[] {-0.4, 1.35}) {
				double x = this.getX() + a * ax + d * dx, z = this.getZ() + a * az + d * dz;
				x0 = Math.min(x0, x);
				x1 = Math.max(x1, x);
				z0 = Math.min(z0, z);
				z1 = Math.max(z1, z);
			}
		}
		return new net.minecraft.util.math.Box(x0, this.getY(), z0, x1, this.getY() + 2.6, z1);
	}

	/** Il suit du regard le joueur le plus proche ; c'est tout ce qu'il fait. */
	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 24.0F, 1.0F));
	}

	/**
	 * Le corps entier suit le regard : il se tourne vers qui approche, sans un
	 * pas — sauf à l'arbre, où le corps ne bouge pas.
	 */
	@Override
	public void tickMovement() {
		super.tickMovement();
		int danse = this.dataTracker.get(DANSE);
		if (danse > 0 && !this.getWorld().isClient()) {
			this.dataTracker.set(DANSE, danse - 1);
		}
		this.setVelocity(Vec3d.ZERO);
		if (this.adieu > 0 && this.getWorld() instanceof ServerWorld vide && --this.adieu == 0) {
			this.disparaître(vide);
			return;
		}
		if (this.àReplacer && !this.getWorld().isClient() && this.ancre != null) {
			this.àReplacer = false;
			this.épier();
		}
		if (this.ancre != null && this.dataTracker.get(ÉPIANT) && danse <= 0
				&& this.getWorld() instanceof ServerWorld monde) {
			this.guetter(monde);
		}
		if (this.corpsFixe()) {
			// Il ne quitte pas l'arbre des yeux du corps : seule la tête suit,
			// et caché, pas même elle.
			float lacet = this.lacetFixe();
			this.setYaw(lacet);
			this.setBodyYaw(lacet);
			// Pendant l'adieu non plus : à vingt centimètres, viser nos yeux lui
			// tournait la tête, et son visage sortait de l'écran.
			if (this.caché() || this.enAdieu()) {
				this.setHeadYaw(lacet);
				this.setPitch(0.0F);
			}
		} else {
			this.setBodyYaw(this.getHeadYaw());
		}
	}

	/**
	 * Le jeu ramène le corps d'un mob immobile vers sa tête au bout d'une
	 * demi-seconde ({@code BodyControl}), après {@link #tickMovement} : à
	 * l'arbre, le joueur à plus de 75° de côté, il se tournait tout entier vers
	 * lui, sorti de sa racine. Là, rien ne le tourne.
	 */
	@Override
	protected float turnHead(float bodyRotation, float headRotation) {
		if (this.corpsFixe()) {
			this.setBodyYaw(this.lacetFixe());
			return headRotation;
		}
		return super.turnHead(bodyRotation, headRotation);
	}

	/**
	 * Le toucher ouvre sa boutique ({@link GasterShop}) ; c'est en sortant
	 * qu'il s'en va, en lâchant son œuf. Boutique fermée par la règle de jeu :
	 * l'œuf, aussitôt.
	 */
	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (this.getWorld() instanceof ServerWorld monde) {
			if (GasterShop.ouverte(monde) && player instanceof net.minecraft.server.network.ServerPlayerEntity joueur) {
				GasterShop.ouvrir(joueur, this);
			} else {
				this.disparaître(monde);
			}
		}
		return ActionResult.success(this.getWorld().isClient);
	}

	/** On est sorti de sa boutique : il vient nous fixer, puis s'en va. */
	public void partir(net.minecraft.server.network.ServerPlayerEntity joueur) {
		if (this.getWorld() instanceof ServerWorld && this.isAlive() && !this.enAdieu()) {
			this.seMontrer(joueur);
		}
	}

	/**
	 * Juste devant le joueur, face à lui, ses yeux à {@link #PRÈS} des siens et
	 * à leur hauteur — le corps descend sous le sol s'il le faut, on ne voit que
	 * la tête. Un mur entre eux : il s'approche d'autant.
	 */
	private void seMontrer(net.minecraft.server.network.ServerPlayerEntity joueur) {
		Vec3d œil = joueur.getEyePos();
		double rad = Math.toRadians(joueur.getYaw());
		Vec3d devant = new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad));
		double près = PRÈS;
		BlockHitResult choc = this.getWorld().raycast(new RaycastContext(œil, œil.add(devant.multiply(près + 0.2)),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, joueur));
		if (choc.getType() != HitResult.Type.MISS) {
			près = Math.max(0.3, Math.min(près, choc.getPos().distanceTo(œil) - 0.25));
		}
		Vec3d droite = new Vec3d(-Math.cos(rad), 0.0, -Math.sin(rad));
		Vec3d pieds = œil.add(devant.multiply(près + AVANCE_YEUX)).subtract(droite.multiply(CÔTÉ_YEUX))
				.subtract(0.0, HAUTEUR_YEUX, 0.0);
		float lacet = joueur.getYaw() + 180.0F;
		this.dataTracker.set(ÉPIANT, false);
		this.dataTracker.set(CACHÉ, false);
		this.dataTracker.set(DANSE, 0);
		this.refreshPositionAndAngles(pieds.x, pieds.y, pieds.z, lacet, 0.0F);
		this.setHeadYaw(lacet);
		this.setBodyYaw(lacet);
		this.getLookControl().lookAt(joueur, 360.0F, 360.0F);
		this.dataTracker.set(ADIEU, true);
		this.adieu = DURÉE_ADIEU;
		this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
				SoundCategory.HOSTILE, 0.6F, 0.4F);
	}

	/** Son étal, tiré à la première ouverture et gardé avec lui. */
	public List<GasterShop.Article> étal() {
		if (this.étal == null) {
			this.étal = GasterShop.tirer(this.random);
			this.vendus = 0;
		}
		return this.étal;
	}

	/** Un bit par article de l'étal déjà vendu. */
	public int vendus() {
		return this.vendus;
	}

	public void vendre(int i) {
		this.vendus |= 1 << i;
	}

	/** Pour le portrait de la boutique : une copie hors du monde, à sa forme, en marchand. */
	public void copierApparence(GasterEntity copie) {
		copie.boutique = true;
	}

	/** Le portrait de la boutique (une copie, côté client) : il se frotte les mains. */
	public void enBoutique() {
		this.boutique = true;
	}

	/** Un œuf saute de lui, dans un bruit de poule qui pond ; il se dissipe en fumée. */
	private void disparaître(ServerWorld monde) {
		ItemEntity œuf = new ItemEntity(monde, this.getX(), this.getY() + 1.3, this.getZ(), new ItemStack(Items.EGG));
		œuf.setVelocity(this.random.nextGaussian() * 0.03, 0.32, this.random.nextGaussian() * 0.03);
		œuf.setToDefaultPickupDelay();
		monde.spawnEntity(œuf);
		monde.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.NEUTRAL, 1.0F, 1.0F);
		monde.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 0.8F, 0.6F);
		monde.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.5, this.getZ(),
				40, 0.35, 0.8, 0.35, 0.01);
		this.discard();
	}

	/** Parti pour de bon — touché, tué, retiré : l'arbre peut l'attendre de nouveau. */
	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (reason.shouldDestroy() && this.ancre != null && this.getWorld() instanceof ServerWorld monde) {
			GasterState.of(monde).libérer(this.ancre);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
			return super.damage(source, amount);
		}
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (this.adieu > 0) {
			nbt.putBoolean("Adieu", true);
		}
		if (this.ancre != null) {
			nbt.putLong("Arbre", this.ancre.asLong());
		}
		nbt.putBoolean("Epie", this.dataTracker.get(ÉPIANT));
		nbt.putBoolean("Cache", this.dataTracker.get(CACHÉ));
		if (this.étal != null) {
			net.minecraft.nbt.NbtList boutique = new net.minecraft.nbt.NbtList();
			for (GasterShop.Article a : this.étal) {
				NbtCompound article = new NbtCompound();
				article.put("Objet", a.objet().encode(this.getRegistryManager()));
				article.putInt("Rang", a.rang().ordinal());
				boutique.add(article);
			}
			nbt.put("Boutique", boutique);
			nbt.putInt("Vendus", this.vendus);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		// Sauvé en plein adieu : il finit de partir au chargement.
		if (nbt.getBoolean("Adieu")) {
			this.adieu = 1;
		}
		this.ancre = nbt.contains("Arbre") ? BlockPos.fromLong(nbt.getLong("Arbre")) : null;
		this.dataTracker.set(ÉPIANT, nbt.getBoolean("Epie"));
		this.dataTracker.set(CACHÉ, nbt.getBoolean("Cache"));
		if (nbt.contains("Boutique")) {
			this.étal = new ArrayList<>();
			for (net.minecraft.nbt.NbtElement e : nbt.getList("Boutique", net.minecraft.nbt.NbtElement.COMPOUND_TYPE)) {
				NbtCompound article = (NbtCompound) e;
				ItemStack.fromNbt(this.getRegistryManager(), article.get("Objet")).ifPresent(objet ->
						this.étal.add(new GasterShop.Article(objet, GasterShop.Rang.de(article.getInt("Rang")))));
			}
			this.vendus = nbt.getInt("Vendus");
		}
		// Paru avant l'épie (0.54) : il attend encore deux blocs à l'ouest de l'arbre.
		this.àReplacer = this.ancre != null && !nbt.contains("Epie");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "idle", 4, state -> state.setAndContinue(
				this.boutique ? BOUTIQUE : this.dataTracker.get(DANSE) > 0 ? SIX_SEVEN : this.épie() ? ÉPIE : REPOS)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
