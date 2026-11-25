package dev.pyrofetch.jn1.entity;

import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.dialogue.DialogueTrigger;
import dev.pyrofetch.jn1.entity.goal.JennyFollowGoal;
import dev.pyrofetch.jn1.entity.goal.JennyLeadGoal;
import dev.pyrofetch.jn1.entity.goal.JennyWanderGoal;
import dev.pyrofetch.jn1.entity.goal.SceneGoal;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModParticles;
import dev.pyrofetch.jn1.screen.JennyScreenHandler;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * J.N-1. Le dernier membre du groupe témoin.
 *
 * <p>Deux propriétés portent tout le lore : le sculk ne la voit pas (elle
 * n'émet aucune vibration) et rien ne peut la tuer (elle est l'ancre du
 * réseau). Ce sont les deux mêmes lignes de code qui font que les Wardens
 * l'ignorent : {@code isValidTarget} du Warden écarte les entités
 * invulnérables.
 */
public abstract class CompanionEntity extends PathAwareEntity implements GeoEntity {
	private static final TrackedData<Integer> MODE =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> UNSETTLED =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> TALK_TICKS =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> POINTING =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> GESTURE =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/**
	 * Le dernier cadeau qu'elle a aimé, un seul exemplaire : elle le tient en
	 * main le temps du geste de réception. Non sauvé : le
	 * geste ne l'est pas non plus.
	 */
	private static final TrackedData<ItemStack> GIFT =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	/** Ticks restants de sourire : elle plisse les yeux et bat des paupières. */
	private static final TrackedData<Integer> DELIGHTED =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/**
	 * La tenue qu'elle porte, par le nom de son modèle ({@link CompanionKind#outfits()}) ;
	 * vide pour celle par défaut. Suivie plutôt que déduite du joueur : le
	 * modèle est rendu pour tout le monde, et deux joueurs ne peuvent pas la
	 * voir autrement l'un que l'autre. C'est donc un état de la compagne.
	 */
	private static final TrackedData<String> OUTFIT =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.STRING);
	/**
	 * La scène que la télécommande lui fait jouer, par son nom court
	 * ({@code b_su}) ; vide quand elle n'en joue aucune.
	 */
	private static final TrackedData<String> SCENE =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.STRING);
	/**
	 * Compte les demandes de scène. Redemander celle qui joue ne change pas
	 * {@link #SCENE} : c'est ce compteur qui dit au client de la reprendre au
	 * début — sans lui, une scène qui ne boucle pas ne se rejouerait jamais.
	 */
	private static final TrackedData<Integer> SCENE_SERIAL =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** Le rythme d'une {@link Demande} en cours (0 le plus lent), −2 pendant son introduction, −1 sans demande. */
	private static final TrackedData<Integer> DEMANDE_RYTHME =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** La vitesse de lecture de la scène : une demande la règle, 1 sinon. */
	private static final TrackedData<Float> SCENE_SPEED =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.FLOAT);
	/**
	 * La pièce de scène ({@link CompanionKind#sceneProp()}) est-elle dessinée
	 * pendant les scènes. Un réglage de la télécommande, gardé dans le NBT.
	 */
	private static final TrackedData<Boolean> SCENE_PROP =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/**
	 * Le joueur qui participe à la scène : il est monté sur elle, voit par les
	 * yeux de {@code steve}, et c'est son skin qui habille le mannequin. Vide
	 * quand on regarde la scène de l'extérieur.
	 */
	private static final TrackedData<java.util.Optional<UUID>> SCENE_PARTNER =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	/** Le partenaire est une femme : la pièce de scène ne paraît pas sur lui. */
	private static final TrackedData<Boolean> SCENE_PARTNER_FEMALE =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/**
	 * Ses traces blanches, de 0 à {@link #MAX_TRACES} : un calque par niveau,
	 * dessiné par-dessus sa planche. Seule la douche du sas les retire.
	 */
	private static final TrackedData<Integer> TRACES =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	public static final int MAX_TRACES = 3;
	/**
	 * Son humeur, de {@link #HUMEUR_MIN} à {@link #HUMEUR_MAX} (0.56.0, le
	 * système d'interaction) : ce qu'on lui dit, ce qu'on lui offre et ce qu'on
	 * lui fait la font monter ou descendre, et elle revient d'elle-même vers 0.
	 * Elle change ses chances de bien prendre ce qu'on lui dit, et ses répliques ;
	 * jamais ce qu'on peut lui demander (Demande) — elle ne refuse jamais le sexe.
	 */
	private static final TrackedData<Integer> MOOD =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	public static final int HUMEUR_MIN = -10;
	public static final int HUMEUR_MAX = 10;
	/** Elle revient d'un cran vers 0 toutes les deux minutes. */
	private static final int HUMEUR_RETOUR = 2400;
	/** Menottée : mains jointes dans le dos, immobile d'elle-même ({@link dev.pyrofetch.jn1.jouets.HandcuffsItem}). */
	private static final TrackedData<Boolean> CUFFED =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Niveaux des potions de poitrine et de croupe ({@link dev.pyrofetch.jn1.shape.Shapes}). */
	private static final TrackedData<Integer> SHAPE_BOOBS =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> SHAPE_BOOTY =
			DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** Ticks écoulés depuis le début de la scène en cours : rythme les traces des scènes « cu ». */
	private int sceneTicks;
	/**
	 * Le joueur qui a lancé la scène : c'est son excitation qui monte
	 * ({@link dev.pyrofetch.jn1.excitation.Excitation}). Serveur seulement, non
	 * sauvé — la scène ne l'est pas non plus. Vide pour une scène lancée sans
	 * joueur (commande) : elle boucle alors sans fin.
	 */
	@org.jetbrains.annotations.Nullable
	private UUID sceneOwner;
	/** Vrai pendant la fin « cu » amenée par la barre : une boucle, puis la scène s'arrête. */
	private boolean sceneFinale;

	// --- Posture et déplacement ---
	/** Acte 3 : l'ancre lâche, elle ne tient plus debout comme avant. */
	/** Mode « guide » : elle s'arrête et scrute en attendant qu'on la rattrape. */
	/** Expression, purement faciale : elle se superpose sans rien contredire. */

	/** Portée dans laquelle son passage réveille le sculk. */
	private static final int SENSOR_RADIUS = 6;
	private static final int SENSOR_INTERVAL = 10;

	/** Cadence d'évaluation des répliques d'ambiance. */
	private static final int AMBIENT_INTERVAL = 40;
	private static final double DIALOGUE_RANGE = 12.0;
	/** Une minute passée à portée vaut un point de confiance… */
	private static final int PROXIMITY_INTERVAL = 1200;
	private static final double PROXIMITY_RANGE = 8.0;
	/** …jusqu'à ce plafond. Le reste se mérite autrement. */
	private static final int PROXIMITY_TRUST_CAP = 5;

	/** Ce qu'elle sait retrouver, et jusqu'où elle cherche (en chunks). */
	private static final TagKey<Structure> GUIDE_DESTINATION =
			TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("jn1", "guide_destination"));
	private static final int GUIDE_SEARCH_RADIUS = 100;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	/** Ce qu'elle porte. Elle garde ce qu'on lui donne, et le rend si elle part. */
	private final SimpleInventory backpack = new SimpleInventory(JennyScreenHandler.SLOT_COUNT);

	@Nullable
	private UUID companionUuid;
	@Nullable
	private BlockPos anchorPos;
	@Nullable
	private BlockPos leadTarget;
	private int idleTicks;

	/** Compte à rebours avant qu'elle ne s'en aille. -1 : elle reste. */
	private int departureTicks = -1;
	private boolean leavesArchive;
	@Nullable
	private BlockPos lastSculkScanPos;
	private int gestureTicks;
	private int ticksAlone;
	/** Côté client : la dernière demande de scène que le contrôleur a prise en compte. */
	private int seenSceneSerial;

	public CompanionEntity(EntityType<? extends CompanionEntity> type, World world) {
		super(type, world);
		// Elle encaisse les coups comme n'importe qui : l'invulnérabilité, qui
		// venait du temps où elle servait d'ancre, la rendait insensible à tout
		// et privait les combats de sens.
		this.setPersistent();
		this.experiencePoints = 0;
	}

	public static DefaultAttributeContainer.Builder createJennyAttributes() {
		return PathAwareEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	// ------------------------------------------------------------------
	// État synchronisé
	// ------------------------------------------------------------------

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(MODE, JennyMode.IDLE.id());
		builder.add(UNSETTLED, false);
		builder.add(TALK_TICKS, 0);
		builder.add(POINTING, false);
		builder.add(GESTURE, CompanionGesture.NONE.id());
		builder.add(GIFT, ItemStack.EMPTY);
		builder.add(DELIGHTED, 0);
		builder.add(OUTFIT, "");
		builder.add(SCENE, "");
		builder.add(SCENE_SERIAL, 0);
		builder.add(DEMANDE_RYTHME, -1);
		builder.add(SCENE_SPEED, 1.0F);
		// Visible par défaut.
		builder.add(SCENE_PROP, true);
		builder.add(SCENE_PARTNER, java.util.Optional.empty());
		builder.add(SCENE_PARTNER_FEMALE, false);
		builder.add(TRACES, 0);
		builder.add(MOOD, 0);
		builder.add(CUFFED, false);
		builder.add(SHAPE_BOOBS, 0);
		builder.add(SHAPE_BOOTY, 0);
	}

	public JennyMode getMode() {
		return JennyMode.byId(this.dataTracker.get(MODE));
	}

	public void setMode(JennyMode mode) {
		this.dataTracker.set(MODE, mode.id());
		if (mode != JennyMode.LEAD) {
			this.leadTarget = null;
		}
		if (mode == JennyMode.WANDER && this.anchorPos == null) {
			this.anchorPos = this.getBlockPos();
		}
		this.idleTicks = 0;
	}

	/** Acte 3 : le réseau déborde sur elle. Change la texture et l'animation. */
	public boolean isUnsettled() {
		return this.dataTracker.get(UNSETTLED);
	}

	public void setUnsettled(boolean unsettled) {
		this.dataTracker.set(UNSETTLED, unsettled);
	}

	/** La tenue qu'elle porte : toujours l'une des siennes. */
	public String getOutfit() {
		return this.kind().outfit(this.dataTracker.get(OUTFIT));
	}

	public void setOutfit(String outfit) {
		this.dataTracker.set(OUTFIT, this.kind().outfit(outfit));
	}

	/** La scène en cours, par son nom court ; vide s'il n'y en a pas. */
	public String getScene() {
		return this.dataTracker.get(SCENE);
	}

	public boolean hasScene() {
		return !this.getScene().isEmpty();
	}

	/**
	 * Lui fait jouer une scène, ou la reprend au début si c'est déjà celle-là.
	 *
	 * <p>Le nom n'est pas vérifié ici : c'est aux appelants de le confronter à
	 * {@link CompanionScenes}, qui seuls savent ce que le fichier contient.
	 */
	public void playScene(String scene) {
		this.oublierDemande();
		this.lancer(scene);
	}

	private void lancer(String scene) {
		this.sceneFinale = false;
		this.sceneOwner = null;
		this.getNavigation().stop();
		this.dataTracker.set(SCENE, scene);
		this.dataTracker.set(SCENE_SERIAL, this.dataTracker.get(SCENE_SERIAL) + 1);
		this.sceneTicks = 0;
	}

	// ------------------------------------------------------------- demandes --

	/** La demande en cours, ou rien (scène lancée à la télécommande, ou aucune). */
	@Nullable
	private Demande demande;
	private boolean demandeBrutale;
	/** L'introduction de la demande joue encore. */
	private boolean demandeIntro;

	/**
	 * Ce qu'un joueur lui demande depuis son menu : elle se tourne vers lui, il
	 * prend la place du partenaire, et la scène commence — par son introduction,
	 * puis au rythme le plus lent. Les seuils sont vérifiés par l'appelant.
	 */
	public void commencerDemande(ServerPlayerEntity joueur, Demande demande, boolean brutale) {
		this.getNavigation().stop();
		dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, this, "demande");
		float cap = (float) Math.toDegrees(Math.atan2(joueur.getZ() - this.getZ(), joueur.getX() - this.getX())) - 90.0F;
		this.setYaw(cap);
		this.setBodyYaw(cap);
		this.setHeadYaw(cap);
		this.demande = demande;
		this.demandeBrutale = brutale;
		if (demande.intro() != null) {
			this.demandeIntro = true;
			this.dataTracker.set(DEMANDE_RYTHME, INTRO);
			this.dataTracker.set(SCENE_SPEED, 1.0F);
			this.lancer(demande.intro());
		} else {
			this.demandeIntro = false;
			this.jouerRythme(Demande.DÉPART);
		}
		this.setSceneOwner(joueur);
		this.joinScene(joueur);
		this.showHearts(6);
	}

	/**
	 * Plus vite (+1) ou plus lentement (−1) ; sans effet hors d'une demande ou
	 * pendant sa fin. Pendant l'introduction, « plus vite » la passe : celle de
	 * la fellation (`b_in`) dure 49 secondes.
	 */
	public void changerRythme(int sens) {
		if (this.demande == null || this.sceneFinale) {
			return;
		}
		if (this.demandeIntro) {
			if (sens > 0) {
				this.demandeIntro = false;
				this.jouerRythme(Demande.DÉPART);
			}
			return;
		}
		int niveau = Math.clamp(this.dataTracker.get(DEMANDE_RYTHME) + Integer.signum(sens), 0,
				this.demande.rythmes().size() - 1);
		if (niveau != this.dataTracker.get(DEMANDE_RYTHME)) {
			this.jouerRythme(niveau);
		}
	}

	private void jouerRythme(int niveau) {
		Demande.Rythme rythme = this.demande.rythme(this.kind(), niveau, this.demandeBrutale);
		this.dataTracker.set(DEMANDE_RYTHME, niveau);
		this.dataTracker.set(SCENE_SPEED, rythme.vitesse());
		UUID maître = this.sceneOwner;
		this.lancer(rythme.scène());
		this.sceneOwner = maître;
	}

	/** L'introduction finie, le premier rythme. */
	private void tickDemande() {
		if (this.demande != null && this.demandeIntro && this.hasScene()
				&& this.sceneTicks >= CompanionScenes.ticks(this.kind(), this.getScene())) {
			this.demandeIntro = false;
			this.jouerRythme(Demande.DÉPART);
		}
	}

	private void oublierDemande() {
		this.demande = null;
		this.demandeIntro = false;
		this.dataTracker.set(DEMANDE_RYTHME, -1);
		this.dataTracker.set(SCENE_SPEED, 1.0F);
	}

	/**
	 * Peut-on l'emmener ailleurs (barre, cloison, réveil,
	 * capsule, suivi) ? FapGPT, non : elle ne quitte pas son projecteur.
	 */
	public boolean peutQuitterSaPlace() {
		return true;
	}

	/**
	 * Sait-elle jouer les poses et les scènes de Jenny (réveil, cloison, barre,
	 * menottes) ? Une fille sur un autre squelette, non : on ne l'y appelle pas.
	 */
	public boolean joueLesScènes() {
		return this.kind().hasScenes();
	}

	/** Elle suit ce joueur, ou elle attend là — sauf menottée (sa chaîne la mène). */
	public void basculerSuivi(ServerPlayerEntity joueur) {
		if (!this.peutQuitterSaPlace()) {
			joueur.sendMessage(Text.translatable("message.jn1.aura.anchored", this.getName()), true);
			return;
		}
		if (this.isCuffed()) {
			joueur.sendMessage(Text.translatable("message.jn1.handcuffs.cannot_follow", this.getName()), true);
			return;
		}
		this.setCompanion(joueur);
		JennyMode suivant = this.getMode() == JennyMode.FOLLOW ? JennyMode.IDLE : JennyMode.FOLLOW;
		this.setMode(suivant);
		if (suivant == JennyMode.IDLE) {
			this.setAnchorPos(this.getBlockPos());
		}
		joueur.sendMessage(Text.translatable(suivant == JennyMode.FOLLOW
				? "message.jn1.jenny.follow" : "message.jn1.jenny.stay"), true);
	}

	/** Le rythme de la demande en cours (0 le plus lent), {@link #INTRO} pendant son introduction, −1 sans demande. */
	public static final int INTRO = -2;

	public int demandeRythme() {
		return this.dataTracker.get(DEMANDE_RYTHME);
	}

	/** La vitesse de lecture de la scène en cours. */
	public float sceneSpeed() {
		return this.dataTracker.get(SCENE_SPEED);
	}

	/** La lance pour ce joueur : son excitation montera tant qu'elle joue. */
	public void playScene(String scene, ServerPlayerEntity owner) {
		this.playScene(scene);
		this.setSceneOwner(owner);
	}

	public void setSceneOwner(@org.jetbrains.annotations.Nullable PlayerEntity owner) {
		this.sceneOwner = owner == null ? null : owner.getUuid();
	}

	/**
	 * La barre est pleine : la scène passe à sa fin « cu » ({@code d_st} →
	 * {@code d_cu}), qui jouera une boucle puis l'arrêtera. Sans fin dans le
	 * fichier, elle finit tout de suite. Rend le nom de la fin, vide sinon.
	 */
	public String finishScene() {
		if (!this.hasScene() || this.sceneFinale) {
			return "";
		}
		String fin = CompanionScenes.fin(this.kind(), this.getScene());
		if (fin.isEmpty()) {
			dev.pyrofetch.jn1.cum.Cum.jaillir(this);
			this.stopScene();
			return "";
		}
		UUID maître = this.sceneOwner;
		this.playScene(fin);
		this.sceneOwner = maître;
		this.sceneFinale = true;
		return fin;
	}

	public boolean isSceneFinale() {
		return this.sceneFinale;
	}

	/** Quand elle a lâché ses sous-vêtements pour la dernière fois (temps du monde). */
	private long lingerieDepuis = -24000L;

	/** Vrai si elle peut en lâcher : une fois par jour du jeu, sinon on les cultivait en changeant de tenue. */
	public boolean peutLâcherLingerie() {
		long maintenant = this.getWorld().getTime();
		if (maintenant - this.lingerieDepuis < 24000L) {
			return false;
		}
		this.lingerieDepuis = maintenant;
		return true;
	}

	public boolean isCuffed() {
		return this.dataTracker.get(CUFFED);
	}

	/** Menotter ou libérer ; libérée, sa chaîne tombe, sans laisse à ramasser. */
	public void setCuffed(boolean cuffed) {
		this.dataTracker.set(CUFFED, cuffed);
		if (!cuffed && this.isLeashed()) {
			// La chaîne ne laisse rien (`detachLeash` y veille) ; une vraie laisse est rendue.
			this.detachLeash(true, true);
		}
	}

	public int getTraces() {
		return this.dataTracker.get(TRACES);
	}

	public void setTraces(int traces) {
		this.dataTracker.set(TRACES, Math.clamp(traces, 0, MAX_TRACES));
	}

	public void addTraces(int traces) {
		this.setTraces(this.getTraces() + traces);
	}

	/**
	 * Ce qui se voit d'elle, recopié sur une autre du même type : son portrait
	 * dans un écran ({@code client.screen.Portrait}), qui a sa propre horloge
	 * d'animation. La taille n'y est pas : le portrait la ramène à son cadre.
	 */
	public void copierApparence(CompanionEntity copie) {
		copie.setOutfit(this.getOutfit());
		copie.setTraces(this.getTraces());
		copie.dataTracker.set(SHAPE_BOOBS, this.dataTracker.get(SHAPE_BOOBS));
		copie.dataTracker.set(SHAPE_BOOTY, this.dataTracker.get(SHAPE_BOOTY));
		copie.dataTracker.set(CUFFED, this.isCuffed());
	}

	/** Le niveau d'une forme ; la taille, elle, est lue sur l'attribut. */
	public int getShape(dev.pyrofetch.jn1.shape.Shapes.Part part) {
		return switch (part) {
			case BOOBS -> this.dataTracker.get(SHAPE_BOOBS);
			case BOOTY -> this.dataTracker.get(SHAPE_BOOTY);
			case SIZE -> dev.pyrofetch.jn1.shape.Shapes.niveau(this, part);
		};
	}

	public void setShape(dev.pyrofetch.jn1.shape.Shapes.Part part, int niveau) {
		switch (part) {
			case BOOBS -> this.dataTracker.set(SHAPE_BOOBS, niveau);
			case BOOTY -> this.dataTracker.set(SHAPE_BOOTY, niveau);
			case SIZE -> dev.pyrofetch.jn1.shape.Shapes.fixer(this, part, niveau);
		}
	}

	public boolean isScenePropShown() {
		return this.dataTracker.get(SCENE_PROP);
	}

	public void setScenePropShown(boolean shown) {
		this.dataTracker.set(SCENE_PROP, shown && this.kind().hasSceneProp());
	}

	/** Vrai si la pièce de scène est à dessiner maintenant : réglée, et une scène joue. */
	public boolean showsSceneProp() {
		// Pas pendant les poses (la barre) : sans steve placé, la pièce
		// flottait à la place de repos de steve.
		return this.hasScene() && this.isScenePropShown() && !this.getScene().startsWith("jn1_");
	}

	/** Rend la main à son comportement ordinaire ; le partenaire descend. */
	public void stopScene() {
		this.oublierDemande();
		this.sceneOwner = null;
		this.sceneFinale = false;
		this.leaveScene();
		this.dataTracker.set(SCENE, "");
	}

	public java.util.Optional<UUID> getScenePartner() {
		return this.dataTracker.get(SCENE_PARTNER);
	}

	public boolean isScenePartner(net.minecraft.entity.Entity entity) {
		return this.getScenePartner().map(entity.getUuid()::equals).orElse(false);
	}

	public boolean isScenePartnerFemale() {
		return this.dataTracker.get(SCENE_PARTNER_FEMALE);
	}

	/** Vrai si un joueur tient la place de {@code steve} dans la scène en cours. */
	public boolean showsScenePartner() {
		return this.hasScene() && this.getScenePartner().isPresent();
	}

	/**
	 * Le joueur prend la place de {@code steve} : il monte sur elle, ce qui le
	 * tient immobile sans rien écrire de plus, et s'en défait en s'accroupissant
	 * comme de n'importe quelle monture. Une seule place.
	 */
	public void joinScene(ServerPlayerEntity player) {
		if (!this.hasScene() || (this.getScenePartner().isPresent() && !this.isScenePartner(player))) {
			return;
		}
		// Les poses `jn1_` (la danse) n'animent pas `steve` ; et sans
		// mannequin (Gardevoir, sans pièce de scène), le joueur deviendrait
		// invisible, la caméra dans le corps de la fille.
		if (this.getScene().startsWith("jn1_") || !this.kind().hasSceneProp()) {
			return;
		}
		if (player.getVehicle() != this) {
			this.retourPartenaire = player.getPos();
			this.retourUuid = player.getUuid();
		}
		this.dataTracker.set(SCENE_PARTNER, java.util.Optional.of(player.getUuid()));
		this.dataTracker.set(SCENE_PARTNER_FEMALE, dev.pyrofetch.jn1.gender.Gender.femme(player));
		dev.pyrofetch.jn1.progress.Succes.accorder(player, "participer");
		if (player.getVehicle() != this) {
			player.startRiding(this, true);
		}
	}

	/** On le fait descendre ; la scène continue, vue de l'extérieur. */
	public void leaveScene() {
		this.descenteVoulue = true;
		try {
			for (net.minecraft.entity.Entity passager : java.util.List.copyOf(this.getPassengerList())) {
				if (this.isScenePartner(passager)) {
					passager.stopRiding();
				}
			}
		} finally {
			this.descenteVoulue = false;
		}
		this.dataTracker.set(SCENE_PARTNER, java.util.Optional.empty());
	}

	/** Vrai pendant {@link #leaveScene} : le partenaire ne part pas de lui-même. */
	private boolean descenteVoulue;

	/** D'où le partenaire est monté : il y redescend (côté joueur de la cloison, à côté du lit). */
	@Nullable
	private net.minecraft.util.math.Vec3d retourPartenaire;
	@Nullable
	private UUID retourUuid;

	@Override
	public net.minecraft.util.math.Vec3d updatePassengerForDismount(LivingEntity passenger) {
		if (this.retourPartenaire != null && passenger.getUuid().equals(this.retourUuid)
				&& passenger.squaredDistanceTo(this.retourPartenaire) < 16 * 16) {
			net.minecraft.util.math.Vec3d retour = this.retourPartenaire;
			this.retourPartenaire = null;
			this.retourUuid = null;
			return retour;
		}
		return super.updatePassengerForDismount(passenger);
	}

	@Override
	protected void removePassenger(net.minecraft.entity.Entity passenger) {
		super.removePassenger(passenger);
		if (!this.getWorld().isClient() && this.isScenePartner(passenger)) {
			this.dataTracker.set(SCENE_PARTNER, java.util.Optional.empty());
			// Descendu de lui-même (accroupi, déconnecté, mort, téléporté) : la
			// scène s'arrête avec lui (0.56.5).
			// Parti avant la fin, il l'entend : « ça ne t'a pas plu ? » — pas
			// pendant la fin « cu », où il n'y a plus rien à regretter.
			if (!this.descenteVoulue && this.hasScene()) {
				boolean fin = this.sceneFinale;
				this.stopScene();
				if (!fin && passenger instanceof ServerPlayerEntity joueur && joueur.isAlive()
						&& !joueur.isDisconnected() && joueur.getWorld() == this.getWorld()) {
					dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, this, "left");
				}
			}
		}
	}

	/**
	 * Le partenaire reste aux pieds de la compagne, pas sur sa tête : sa place
	 * visible est celle de {@code steve}, dessinée par elle, et le point où le
	 * serveur le tient ne sert qu'aux sons et au chargement.
	 */
	@Override
	protected void updatePassengerPosition(net.minecraft.entity.Entity passenger, PositionUpdater positionUpdater) {
		if (this.isScenePartner(passenger)) {
			positionUpdater.accept(passenger, this.getX(), this.getY(), this.getZ());
		} else {
			super.updatePassengerPosition(passenger, positionUpdater);
		}
	}

	/**
	 * Pendant une scène, la boîte recouvre le corps là où l'animation l'a mis,
	 * et non plus là où elle se tiendrait debout : sans cela on visait le vide,
	 * et l'on ne pouvait plus l'atteindre.
	 *
	 * <p>La boîte mesurée ({@link CompanionScenes#bounds}) est dans le repère du
	 * modèle ; GeckoLib inverse x et fait tourner le modèle de {@code 180 -
	 * bodyYaw} autour de y. On fait tourner ses quatre coins de même et l'on
	 * garde leur enveloppe, du sol au sommet de la pose — un corps qui
	 * s'agenouille « sous » le sol ne descend pas la boîte sous les pieds.
	 */
	@Override
	protected net.minecraft.util.math.Box calculateBoundingBox() {
		CompanionScenes.Bounds boîte = this.hasScene() ? CompanionScenes.bounds(this.kind(), this.getScene()) : null;
		if (boîte == null) {
			return super.calculateBoundingBox();
		}
		// Mesurée à la taille d'origine : la potion de taille la met à l'échelle.
		double échelle = this.getScale();
		double angle = Math.toRadians(180.0F - this.bodyYaw);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (float bx : new float[] {-boîte.maxX(), -boîte.minX()}) {
			for (float bz : new float[] {boîte.minZ(), boîte.maxZ()}) {
				double x = (bx * cos + bz * sin) / 16.0 * échelle;
				double z = (-bx * sin + bz * cos) / 16.0 * échelle;
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minZ = Math.min(minZ, z);
				maxZ = Math.max(maxZ, z);
			}
		}
		// Toujours depuis les pieds. Attention : la boîte partait du point
		// le plus bas de la pose ; les scènes « d_ », relevées de 7 pixels comme
		// sur un lit, la faisaient flotter, la gravité la descendait jusqu'au sol
		// — les pieds de l'entité 7 pixels dans le bloc —, et à la scène suivante
		// la boîte, repartie des pieds, était dans le sol : le jeu n'arrête que
		// ce qui y entre, elle passait au travers.
		double haut = Math.max(0.25, boîte.maxY() / 16.0 * échelle);
		return new net.minecraft.util.math.Box(this.getX() + minX, this.getY(), this.getZ() + minZ,
				this.getX() + maxX, this.getY() + haut, this.getZ() + maxZ);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (SCENE.equals(data)) {
			this.setBoundingBox(this.calculateBoundingBox());
			this.sortirDuSol();
		}
	}

	/**
	 * Une compagne enfoncée dans le sol — par l'ancienne boîte des scènes, dans
	 * un monde déjà joué — en remonte, d'un seizième à la fois, jusqu'à un bloc.
	 */
	private void sortirDuSol() {
		if (this.getWorld().isClient() || this.hasVehicle()) {
			return;
		}
		net.minecraft.util.math.Box pieds = this.getBoundingBox().withMaxY(this.getY() + 0.05);
		if (this.getWorld().isSpaceEmpty(this, pieds)) {
			return;
		}
		for (int i = 1; i <= 16; i++) {
			double dy = i / 16.0;
			if (this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0, dy, 0))) {
				this.setPosition(this.getX(), this.getY() + dy, this.getZ());
				return;
			}
		}
	}

	public boolean isTalking() {
		return this.dataTracker.get(TALK_TICKS) > 0;
	}

	/** Appelé quand le moteur de dialogue lui fait dire quelque chose. */
	public void setTalkingFor(int ticks) {
		this.dataTracker.set(TALK_TICKS, Math.max(this.dataTracker.get(TALK_TICKS), ticks));
	}

	public int getMood() {
		return this.dataTracker.get(MOOD);
	}

	public void setMood(int humeur) {
		this.dataTracker.set(MOOD, Math.clamp(humeur, HUMEUR_MIN, HUMEUR_MAX));
	}

	public void changerHumeur(int delta) {
		this.setMood(this.getMood() + delta);
	}

	public boolean isDelighted() {
		return this.dataTracker.get(DELIGHTED) > 0;
	}

	public void setDelightedFor(int ticks) {
		this.dataTracker.set(DELIGHTED, ticks);
	}

	/** Le cadeau qu'elle tient pendant le geste de réception (vide s'il n'y en a pas). */
	public ItemStack getGift() {
		return this.dataTracker.get(GIFT);
	}

	public CompanionGesture getGesture() {
		return CompanionGesture.byId(this.dataTracker.get(GESTURE));
	}

	/** Joue un geste par-dessus la posture, le temps de sa durée propre. */
	/**
	 * Où elle attendait avant de venir se coller au joueur pour un câlin ; rien
	 * si elle le suivait. Le câlin fini, elle y retourne (0.56.7 : FapGPT
	 * restait plantée à côté du joueur, loin de son projecteur).
	 */
	@Nullable
	private net.minecraft.util.math.Vec3d avantCâlin;

	public void retenirPlaceAvantCâlin() {
		this.avantCâlin = this.getMode() == JennyMode.FOLLOW ? null : this.getPos();
	}

	private void revenirAprèsCâlin() {
		net.minecraft.util.math.Vec3d place = this.avantCâlin;
		this.avantCâlin = null;
		if (place == null || this.hasScene() || this.squaredDistanceTo(place) < 0.25) {
			return;
		}
		// Une projection reprend sa place d'un coup ; les autres y retournent à pied.
		if (this instanceof AuraEntity) {
			this.refreshPositionAndAngles(place.x, place.y, place.z, this.getYaw(), this.getPitch());
		} else {
			this.getNavigation().startMovingTo(place.x, place.y, place.z, 1.0);
		}
	}

	public void playGesture(CompanionGesture gesture) {
		this.dataTracker.set(GESTURE, gesture.id());
		this.gestureTicks = gesture.durationTicks();
		// Sa voix accompagne les gestes qui touchent le joueur.
		switch (gesture) {
			case HUG -> dev.pyrofetch.jn1.sound.GirlSounds.crier(this, this.getRandom().nextBoolean() ? "giggle" : "happyoh", 0.9F);
			case RECEIVE -> dev.pyrofetch.jn1.sound.GirlSounds.crier(this, "happyoh", 0.9F);
			case DANCE -> dev.pyrofetch.jn1.sound.GirlSounds.crier(this, "giggle", 0.8F);
			default -> {
			}
		}
	}

	/** Un coup reçu : un « hein ? » plutôt qu'un cri de douleur — rien ne la blesse vraiment. */
	@Override
	protected net.minecraft.sound.SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
		return dev.pyrofetch.jn1.sound.GirlSounds.voix(this, "huh");
	}

	/**
	 * Son humeur, de loin en loin : soupir, moue, souffle, petit rire. Rien
	 * pendant une scène — la scène a ses propres sons.
	 */
	@Override
	protected net.minecraft.sound.SoundEvent getAmbientSound() {
		if (this.hasScene()) {
			return null;
		}
		String[] humeurs = {"sigh", "hmph", "lightbreathing", "giggle", "mmm"};
		return dev.pyrofetch.jn1.sound.GirlSounds.voix(this, humeurs[this.getRandom().nextInt(humeurs.length)]);
	}

	/** Une fois par minute au plus, en moyenne bien moins. */
	@Override
	public int getMinAmbientSoundDelay() {
		return 1200;
	}

	@Override
	protected float getSoundVolume() {
		return 0.7F;
	}

	public boolean isPointing() {
		return this.dataTracker.get(POINTING);
	}

	public void setPointing(boolean pointing) {
		this.dataTracker.set(POINTING, pointing);
	}

	@Nullable
	public UUID getCompanionUuid() {
		return this.companionUuid;
	}

	public void setCompanion(@Nullable PlayerEntity player) {
		this.companionUuid = player == null ? null : player.getUuid();
	}

	@Nullable
	public PlayerEntity getCompanion() {
		if (this.companionUuid == null) {
			return null;
		}
		return this.getWorld().getPlayerByUuid(this.companionUuid);
	}

	@Nullable
	public BlockPos getAnchorPos() {
		return this.anchorPos;
	}

	public void setAnchorPos(@Nullable BlockPos pos) {
		this.anchorPos = pos;
	}

	@Nullable
	public BlockPos getLeadTarget() {
		return this.leadTarget;
	}

	/** Lui demande de guider le joueur vers un point. Bascule en mode LEAD. */
	public void leadTo(BlockPos target) {
		this.leadTarget = target;
		this.dataTracker.set(MODE, JennyMode.LEAD.id());
	}

	// ------------------------------------------------------------------
	// IA
	// ------------------------------------------------------------------

	@Override
	protected void initGoals() {
		// Pendant une scène, elle ne bouge plus ni ne regarde ailleurs : un
		// seul objectif qui tient le déplacement, le saut et le regard suffit à
		// suspendre tous les autres.
		this.goalSelector.add(0, new SceneGoal(this));
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new JennyLeadGoal(this));
		this.goalSelector.add(2, new JennyFollowGoal(this, 1.05, 2.5F, 12.0F));
		this.goalSelector.add(3, new JennyWanderGoal(this, 0.8));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
	}

	@Override
	public void tick() {
		// La chaîne des menottes dont le porteur meurt (le nœud d'une barrière
		// cassée) : le jeu la détache par un chemin qui lâcherait une laisse.
		if (!this.getWorld().isClient() && this.chaîneDeMenottes && this.getLeashHolder() != null
				&& !this.getLeashHolder().isAlive()) {
			this.detachLeash(true, false);
		}
		super.tick();

		int talk = this.dataTracker.get(TALK_TICKS);
		if (talk > 0) {
			this.dataTracker.set(TALK_TICKS, talk - 1);
		}
		// La boîte suit l'orientation du corps, qui peut encore tourner au début
		// d'une scène ; elle ne se recalcule d'elle-même qu'au déplacement.
		if (this.hasScene()) {
			this.setBoundingBox(this.calculateBoundingBox());
		}
		// Personne d'autre que le partenaire ne la monte : après une sauvegarde
		// faite pendant une scène, le jeu remettait le joueur sur elle (la monture
		// est sauvée, la scène non), perché sur sa tête, sans moyen d'en sortir.
		if (!this.getWorld().isClient()) {
			for (net.minecraft.entity.Entity passager : java.util.List.copyOf(this.getPassengerList())) {
				if (passager instanceof PlayerEntity && !this.isScenePartner(passager)) {
					passager.stopRiding();
				}
			}
		}
		if (!this.getWorld().isClient() && this.getScenePartner().isPresent()) {
			boolean monté = this.getPassengerList().stream().anyMatch(this::isScenePartner);
			if (!this.hasScene() || !monté) {
				this.leaveScene();
			}
		}
		if (this.gestureTicks > 0 && --this.gestureTicks == 0) {
			boolean câlin = this.getGesture() == CompanionGesture.HUG;
			this.dataTracker.set(GESTURE, CompanionGesture.NONE.id());
			if (câlin) {
				this.revenirAprèsCâlin();
			}
		}
		int delighted = this.dataTracker.get(DELIGHTED);
		if (delighted > 0) {
			this.dataTracker.set(DELIGHTED, delighted - 1);
		}

		if (this.getWorld() instanceof ServerWorld serverWorld) {
			this.tickCumScene();
			if (this.age % dev.pyrofetch.jn1.shape.Shapes.DÉCRUE == 0) {
				dev.pyrofetch.jn1.shape.Shapes.décroître(this);
			}
			if (this.age % SENSOR_INTERVAL == 0) {
				this.wakeNearbySculk(serverWorld);
			}
			if (this.getNavigation().isIdle()) {
				this.idleTicks++;
			} else {
				this.idleTicks = 0;
			}
			if (this.age % AMBIENT_INTERVAL == 0) {
				this.tickAmbientDialogue();
			}
			if (this.age % HUMEUR_RETOUR == 0 && this.getMood() != 0) {
				this.changerHumeur(this.getMood() > 0 ? -1 : 1);
			}
			if (this.age % PROXIMITY_INTERVAL == 0) {
				this.tickProximityTrust();
			}
			if (this.departureTicks >= 0) {
				this.tickDeparture(serverWorld);
			}
		}
	}

	/**
	 * Lui demande de partir dans {@code ticks} ticks — le temps qu'elle finisse
	 * de parler.
	 *
	 * @param leavesArchive vrai pour la fin A : elle laisse l'archive derrière
	 *                      elle. Faux pour la troisième porte, qui ne laisse rien.
	 */
	public void beginDeparture(int ticks, boolean leavesArchive) {
		this.departureTicks = Math.max(1, ticks);
		this.leavesArchive = leavesArchive;
		this.setMode(JennyMode.IDLE);
	}

	public boolean isLeaving() {
		return this.departureTicks >= 0;
	}

	private void tickDeparture(ServerWorld world) {
		this.departureTicks--;

		// Les dernières secondes se voient : elle se défait par le bas.
		if (this.departureTicks < 100 && this.age % 2 == 0) {
			int count = Math.max(1, (100 - this.departureTicks) / 12);
			world.spawnParticles(ModParticles.GLYPH_MOTE,
					this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
					count, 0.3, 0.6, 0.3, 0.01);
		}

		if (this.departureTicks > 0) {
			return;
		}

		world.spawnParticles(ModParticles.GLYPH_MOTE,
				this.getX(), this.getY() + 1.0, this.getZ(), 60, 0.4, 0.9, 0.4, 0.08);
		world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
				SoundCategory.NEUTRAL, 1.0F, 0.6F);

		// Ce qu'on lui a confié ne part pas avec elle.
		for (int slot = 0; slot < this.backpack.size(); slot++) {
			ItemStack carried = this.backpack.removeStack(slot);
			if (!carried.isEmpty()) {
				ItemEntity dropped = new ItemEntity(world, this.getX(), this.getY() + 0.5,
						this.getZ(), carried);
				dropped.setToDefaultPickupDelay();
				world.spawnEntity(dropped);
			}
		}


		this.discard();
	}

	/**
	 * Elle s'adresse au joueur le plus proche. En multijoueur, c'est lui qui
	 * décide de ce qu'elle peut dire : ses drapeaux, sa confiance, son acte.
	 */
	private void tickAmbientDialogue() {
		// En scène ou en veille, elle a autre chose à faire que bavarder.
		if (this.hasScene() || this.estEnVeille()) {
			return;
		}
		PlayerEntity nearest = this.getWorld().getClosestPlayer(this, DIALOGUE_RANGE);
		if (!(nearest instanceof ServerPlayerEntity serverPlayer) || serverPlayer.isSpectator()) {
			this.ticksAlone += AMBIENT_INTERVAL;
			return;
		}

		// Personne depuis un moment : elle fait signe avant même de parler.
		boolean retrouvailles = this.ticksAlone > REUNION_TICKS;
		if (retrouvailles) {
			this.playGesture(CompanionGesture.GREET);
		}
		this.ticksAlone = 0;
		if (!retrouvailles || !DialogueEngine.réagir(serverPlayer, this, "reunion")) {
			DialogueEngine.trySpeak(serverPlayer, this, DialogueTrigger.AMBIENT);
		}
	}

	/**
	 * Rester près d'elle finit par compter. Mais la présence seule plafonne :
	 * au-delà, il faut lui donner quelque chose ou avancer dans l'histoire.
	 */
	private void tickProximityTrust() {
		for (PlayerEntity player : this.getWorld().getPlayers()) {
			if (!(player instanceof ServerPlayerEntity serverPlayer) || serverPlayer.isSpectator()) {
				continue;
			}
			if (this.squaredDistanceTo(serverPlayer) > PROXIMITY_RANGE * PROXIMITY_RANGE) {
				continue;
			}
			if (ModAttachments.get(serverPlayer).trust(this.kind().id()) < PROXIMITY_TRUST_CAP) {
				DialogueEngine.grantTrust(serverPlayer, this.kind().id(), 1);
			}
		}
	}

	/**
	 * À chaque boucle achevée d'une scène « cu », elle prend des traces et ce
	 * qui retombe va à l'extracteur, ou au sol ({@link dev.pyrofetch.jn1.cum.Cum}).
	 */
	private void tickCumScene() {
		if (!this.hasScene()) {
			this.sceneTicks = 0;
			return;
		}
		this.sceneTicks++;
		this.tickDemande();
		if (!this.getScene().endsWith("cu")) {
			this.exciter();
			return;
		}
		// Une fin amenée par la barre a son introduction éventuelle en plus.
		int boucle = CompanionScenes.ticks(this.kind(), this.getScene());
		int intro = CompanionScenes.ticks(this.kind(), this.getScene() + CompanionScenes.INTRO);
		if (boucle > 0 && this.sceneTicks > intro && (this.sceneTicks - intro) % boucle == 0) {
			dev.pyrofetch.jn1.cum.Cum.jaillir(this);
			if (this.sceneFinale) {
				// La fin amenée par la barre : celui qui l'a menée jouit avec elle.
				if (this.sceneOwner != null && this.getWorld() instanceof ServerWorld monde) {
					ServerPlayerEntity maître = monde.getServer().getPlayerManager().getPlayer(this.sceneOwner);
					if (maître != null) {
						dev.pyrofetch.jn1.excitation.Excitation.orgasme(maître);
					}
					dev.pyrofetch.jn1.interaction.Conversation.événement(maître, this, "after");
				}
				this.stopScene();
			}
		}
	}

	/** De combien le rythme de chaque moment d'une scène fait monter la barre. */
	private static float rythme(String scène) {
		String moment = scène.substring(scène.indexOf('_') + 1);
		// Les versions brutales (« _r », tools/gen_brutal.py) : moitié plus vite.
		if (moment.endsWith("_r")) {
			return 1.5F * rythme(scène.substring(0, scène.length() - 2));
		}
		return switch (moment) {
			case "in", "go", "wa" -> 0.5F;
			case "th" -> 1.6F;
			case "fa" -> 2.2F;
			default -> 1.0F;
		};
	}

	/**
	 * L'excitation de celui qui a lancé la scène monte, s'il est encore là (32
	 * blocs) : pleinement s'il y participe, un peu plus de moitié s'il regarde.
	 * Pleine, la scène passe à sa fin.
	 */
	private void exciter() {
		if (this.sceneOwner == null || !(this.getWorld() instanceof ServerWorld monde)) {
			return;
		}
		ServerPlayerEntity maître = monde.getServer().getPlayerManager().getPlayer(this.sceneOwner);
		if (maître == null || maître.getWorld() != monde || maître.squaredDistanceTo(this) > 32 * 32) {
			return;
		}
		// Joué plus vite, il monte plus vite : la branlette d'une demande n'a qu'une
		// animation, ses rythmes ne diffèrent que par la vitesse.
		float part = dev.pyrofetch.jn1.excitation.Excitation.PAR_TICK * rythme(this.getScene()) * this.sceneSpeed()
				* (this.isScenePartner(maître) ? 1.0F : 0.6F);
		if (dev.pyrofetch.jn1.excitation.Excitation.ajouter(maître, part)) {
			dev.pyrofetch.jn1.excitation.Excitation.set(maître, 0);
			dev.pyrofetch.jn1.progress.Succes.accorder(maître, "premiere_fois");
			this.finishScene();
		}
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (this.getWorld().isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		// Une étiquette de nom se pose comme sur n'importe quel animal : sans
		// ce cas, le clic ouvrait son menu et l'étiquette ne servait à rien.
		if (player.getStackInHand(hand).isOf(Items.NAME_TAG)) {
			return ActionResult.PASS;
		}

		// La télécommande ouvre le choix des scènes, et rien d'autre : ni
		// dialogue, ni cadeau, ni changement de mode.
		if (player.getStackInHand(hand).isOf(dev.pyrofetch.jn1.registry.ModItems.SCENE_REMOTE)) {
			dev.pyrofetch.jn1.interaction.SceneHandler.open(serverPlayer, this);
			return ActionResult.SUCCESS;
		}

		// Les menottes : la menotter, prendre sa chaîne, la libérer.
		if (player.getStackInHand(hand).isOf(dev.pyrofetch.jn1.registry.ModItems.HANDCUFFS)) {
			dev.pyrofetch.jn1.jouets.HandcuffsItem.utiliserSur(this, serverPlayer);
			return ActionResult.SUCCESS;
		}

		// Une potion de forme tendue : c'est elle qui la boit.
		if (player.getStackInHand(hand).getItem() instanceof dev.pyrofetch.jn1.shape.ShapePotionItem potion) {
			potion.faireBoire(this, player, hand);
			return ActionResult.SUCCESS;
		}

		// Accroupi, le cum se verse sur elle ; debout, c'est un cadeau (plus bas).
		if (player.getStackInHand(hand).isOf(dev.pyrofetch.jn1.registry.ModItems.CUM) && player.isSneaking()) {
			dev.pyrofetch.jn1.cum.CumItem.verserSur(this, player, hand);
			return ActionResult.SUCCESS;
		}

		this.setCompanion(player);
		this.getLookControl().lookAt(player);

		// Accroupi + clic : elle suit, ou elle s'arrête là — comme le bouton de son écran.
		if (player.isSneaking()) {
			this.basculerSuivi(serverPlayer);
			return ActionResult.SUCCESS;
		}


		if (player.getStackInHand(hand).isEmpty()) {
			PlayerProgress progress = ModAttachments.get(serverPlayer);
			if (progress.trust(this.kind().id()) >= PlayerProgress.MAX_TRUST
					&& this.getRandom().nextInt(8) == 0) {
				// Elle n'a plus rien à prouver : elle se lâche.
				this.playGesture(CompanionGesture.DANCE);
			}
			// Elle l'accueille d'une réplique, que son écran reprend.
			net.minecraft.text.Text salut = DialogueEngine.parler(serverPlayer, this, DialogueTrigger.INTERACT,
					null, null, null);
			// Son écran : portrait, affection, et ce qu'on peut lui demander ;
			// son sac s'ouvre de là (bouton « Inventaire »).
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer,
					new dev.pyrofetch.jn1.network.OpenCompanionPayload(this.getId(),
							progress.trust(this.kind().id()),
							salut == null ? net.minecraft.text.Text.empty() : salut));
			return ActionResult.SUCCESS;
		}

		// Une capsule tenue en main sert à capturer, pas à offrir : à bout
		// portant on n'a pas la place de la lancer.
		ItemStack held = player.getStackInHand(hand);
		if (held.isOf(dev.pyrofetch.jn1.registry.ModItems.CAPSULE)
				&& !dev.pyrofetch.jn1.capsule.CapsuleItem.isFull(held)) {
			if (!dev.pyrofetch.jn1.capsule.CapsuleCapture.capturable(this, serverPlayer)) {
				return ActionResult.SUCCESS;
			}
			held.decrement(1);
			dev.pyrofetch.jn1.capsule.CapsuleCapture.capture(this, serverPlayer);
			return ActionResult.SUCCESS;
		}

		int value = giftValue(player.getStackInHand(hand));
		if (value > 0) {
			this.acceptGift(serverPlayer, player.getStackInHand(hand), value);
			DialogueEngine.parler(serverPlayer, this, DialogueTrigger.GIFT, null, "liked", null);
			return ActionResult.SUCCESS;
		}

		DialogueEngine.parler(serverPlayer, this, DialogueTrigger.GIFT, null, "disliked", null);
		return ActionResult.SUCCESS;
	}

	/**
	 * Ce qu'elle apprécie, et à quel point.
	 *
	 * <p>Rien d'autre ne compte : le reste, elle le range simplement dans son
	 * sac. Ces matières-là étant coûteuses, la progression se limite d'elle-même
	 * sans avoir besoin d'un délai artificiel entre deux cadeaux.
	 */
	public static int giftValue(ItemStack stack) {
		if (stack.isOf(Items.DIAMOND) || stack.isOf(Items.DIAMOND_BLOCK)) {
			return 2;
		}
		if (stack.isOf(Items.GOLD_INGOT) || stack.isOf(Items.EMERALD)
				|| stack.isOf(Items.ENDER_PEARL)) {
			return 1;
		}
		if (stack.isOf(Items.GOLD_BLOCK) || stack.isOf(Items.EMERALD_BLOCK)) {
			return 2;
		}
		// Le cum : frais en fiole, ce qu'elle préfère ; séché, en fromage.
		if (stack.isOf(dev.pyrofetch.jn1.registry.ModItems.CUM)) {
			return 3;
		}
		if (stack.isOf(dev.pyrofetch.jn1.registry.ModItems.CUM_CHEESE)) {
			return 2;
		}
		if (stack.isOf(dev.pyrofetch.jn1.registry.ModItems.DRIED_CUM)) {
			return 1;
		}
		return 0;
	}

	private void acceptGift(ServerPlayerEntity player, ItemStack stack, int value) {
		boolean fiole = stack.isOf(dev.pyrofetch.jn1.registry.ModItems.CUM);
		this.dataTracker.set(GIFT, stack.copyWithCount(1));
		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
			if (fiole && !player.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE))) {
				player.dropItem(new ItemStack(Items.GLASS_BOTTLE), false);
			}
		}
		DialogueEngine.grantTrust(player, this.kind().id(), value);
		this.changerHumeur(1 + value);

		this.playGesture(CompanionGesture.RECEIVE);
		this.setDelightedFor(CompanionGesture.RECEIVE.durationTicks());
		this.getLookControl().lookAt(player);
		this.showHearts(6);

		this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
				SoundCategory.NEUTRAL, 0.4F, 1.8F);
	}

	/** Les cœurs, au-dessus d'elle. */
	public void showHearts(int count) {
		if (this.getWorld() instanceof ServerWorld world) {
			world.spawnParticles(net.minecraft.particle.ParticleTypes.HEART,
					this.getX(), this.getY() + this.getHeight() * 0.85, this.getZ(),
					count, 0.35, 0.25, 0.35, 0.02);
		}
	}

	/**
	 * Elle n'emmène pas un inconnu au fond du monde : il faut que l'acte 1 soit
	 * bouclé, c'est-à-dire qu'elle ait admis que le joueur reste.
	 */


	/**
	 * Les sensors s'illuminent sur son passage, mais rien ne se propage : on
	 * pose l'état actif à la main au lieu de passer par une vibration, donc
	 * aucun shrieker n'est prévenu. Ils la reconnaissent, ils ne la signalent
	 * pas.
	 */
	private void wakeNearbySculk(ServerWorld world) {
		BlockPos center = this.getBlockPos();

		// Immobile, elle a déjà réveillé tout ce qu'il y avait à réveiller :
		// inutile de réinspecter deux mille blocs pendant qu'elle est assise.
		if (center.equals(this.lastSculkScanPos)) {
			return;
		}
		this.lastSculkScanPos = center;

		Box box = new Box(center).expand(SENSOR_RADIUS);

		for (BlockPos pos : BlockPos.iterate(
				BlockPos.ofFloored(box.minX, box.minY, box.minZ),
				BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
			BlockState state = world.getBlockState(pos);
			if (!(state.getBlock() instanceof SculkSensorBlock)) {
				continue;
			}
			if (state.get(SculkSensorBlock.SCULK_SENSOR_PHASE) != SculkSensorPhase.INACTIVE) {
				continue;
			}
			// Phase seulement : pas de POWER, donc aucun signal de redstone,
			// et surtout aucun GameEvent émis.
			world.setBlockState(pos.toImmutable(),
					state.with(SculkSensorBlock.SCULK_SENSOR_PHASE, SculkSensorPhase.ACTIVE),
					Block.NOTIFY_LISTENERS);
			world.scheduleBlockTick(pos.toImmutable(), state.getBlock(), 30);
		}
	}

	/** Le sculk ne l'entend pas : elle n'émet aucune vibration, jamais. */
	@Override
	public void emitGameEvent(RegistryEntry<GameEvent> event, @Nullable net.minecraft.entity.Entity entity) {
		// Silence volontaire.
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Le fouet ne la blesse pas : elle sursaute et gémit.
		if (!this.getWorld().isClient() && source.getAttacker() instanceof ServerPlayerEntity fouetteur
				&& source.getSource() == fouetteur
				&& fouetteur.getMainHandStack().isOf(dev.pyrofetch.jn1.registry.ModItems.WHIP)) {
			dev.pyrofetch.jn1.jouets.WhipItem.fouetter(this, fouetteur);
			return false;
		}
		boolean blessee = super.damage(source, amount);
		// Elle rend le coup, mais seulement s'il a porté : riposter à une
		// attaque parée ou hors délai d'invincibilité ferait bégayer le geste.
		if (blessee && !this.getWorld().isClient()
				&& source.getAttacker() instanceof PlayerEntity) {
			this.playGesture(CompanionGesture.randomSwipe(this.getRandom()));
		}
		return blessee;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		// Elle ne tombe ni de faim ni d'ennui : le vide et l'écrasement
		// restent mortels, le reste du décor ne doit pas la tuer toute seule
		// pendant qu'on a le dos tourné.
		if (source.isOf(DamageTypes.CACTUS) || source.isOf(DamageTypes.SWEET_BERRY_BUSH)
				|| source.isOf(DamageTypes.STARVE) || source.isOf(DamageTypes.IN_WALL)) {
			return true;
		}
		return super.isInvulnerableTo(source);
	}

	/** Seulement menottée : sa chaîne est la laisse du jeu. */
	/** En veille (FapGPT au laboratoire) : ni barre, ni machine, ni réveil ne l'appellent. */
	public boolean estEnVeille() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return this.isCuffed();
	}

	/** La chaîne des menottes n'est pas une laisse : défaite, elle ne laisse rien. Une vraie laisse, si. */
	private boolean chaîneDeMenottes;

	public void attacherChaîne(PlayerEntity joueur) {
		this.chaîneDeMenottes = true;
		this.attachLeash(joueur, true);
	}

	@Override
	public void detachLeash(boolean sendPacket, boolean dropItem) {
		super.detachLeash(sendPacket, dropItem && !this.chaîneDeMenottes);
		this.chaîneDeMenottes = false;
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean shouldDropXp() {
		return false;
	}

	@Override
	protected boolean shouldDropLoot() {
		return false;
	}

	/** Morte, elle lâche son sac : avant, son contenu disparaissait avec elle. */
	@Override
	protected void dropInventory() {
		super.dropInventory();
		for (int slot = 0; slot < this.backpack.size(); slot++) {
			ItemStack contenu = this.backpack.removeStack(slot);
			if (!contenu.isEmpty()) {
				this.dropStack(contenu);
			}
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushAway(net.minecraft.entity.Entity entity) {
		// Elle ne bouscule personne et personne ne la bouscule.
	}

	public SimpleInventory getBackpack() {
		return this.backpack;
	}

	public boolean isMovingOnGround() {
		return !this.getNavigation().isIdle() && this.getVelocity().horizontalLengthSquared() > 1.0E-5;
	}

	public int getIdleTicks() {
		return this.idleTicks;
	}

	// ------------------------------------------------------------------
	// Persistance
	// ------------------------------------------------------------------

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Mode", this.getMode().id());
		nbt.putBoolean("Unsettled", this.isUnsettled());
		nbt.putString("Outfit", this.getOutfit());
		nbt.putBoolean("ScenePropShown", this.isScenePropShown());
		nbt.putInt("Traces", this.getTraces());
		nbt.putInt("Mood", this.getMood());
		nbt.putBoolean("Cuffed", this.isCuffed());
		nbt.putBoolean("CuffChain", this.chaîneDeMenottes);
		nbt.putLong("LingerieAt", this.lingerieDepuis);
		nbt.putInt("ShapeBoobs", this.dataTracker.get(SHAPE_BOOBS));
		nbt.putInt("ShapeBooty", this.dataTracker.get(SHAPE_BOOTY));
		if (this.companionUuid != null) {
			nbt.putUuid("Companion", this.companionUuid);
		}
		if (this.anchorPos != null) {
			nbt.put("Anchor", net.minecraft.nbt.NbtHelper.fromBlockPos(this.anchorPos));
		}
		if (this.leadTarget != null) {
			nbt.put("LeadTarget", net.minecraft.nbt.NbtHelper.fromBlockPos(this.leadTarget));
		}
		nbt.put("Backpack", this.backpack.toNbtList(this.getRegistryManager()));
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.dataTracker.set(MODE, nbt.getInt("Mode"));
		// Une fille coincée dans l'ancien lave-linge y flottait (NoGravity tenu à
		// chaque tick) : le lave-linge retiré, elle retombe.
		if (nbt.contains("StuckIn")) {
			this.setNoGravity(false);
		}
		this.setUnsettled(nbt.getBoolean("Unsettled"));
		// Absent d'une sauvegarde d'avant cette version : getBoolean rend faux,
		// et elle reparaît dans sa première apparence.
		// Avant 0.40, une seconde apparence et rien d'autre : « Alternate ».
		if (nbt.contains("Outfit")) {
			this.setOutfit(nbt.getString("Outfit"));
		} else {
			this.setOutfit(nbt.getBoolean("Alternate") ? this.kind().legacyAlternate() : this.kind().defaultOutfit());
		}
		// Nouvelle clé : l'ancienne (« SceneProp ») valait faux chez presque
		// toutes, faute d'avoir été réglée ; le nouveau défaut est l'affichage.
		this.setScenePropShown(!nbt.contains("ScenePropShown") || nbt.getBoolean("ScenePropShown"));
		this.setTraces(nbt.getInt("Traces"));
		this.setMood(nbt.getInt("Mood"));
		this.dataTracker.set(CUFFED, nbt.getBoolean("Cuffed"));
		this.chaîneDeMenottes = nbt.getBoolean("CuffChain");
		this.lingerieDepuis = nbt.getLong("LingerieAt");
		this.dataTracker.set(SHAPE_BOOBS, nbt.getInt("ShapeBoobs"));
		this.dataTracker.set(SHAPE_BOOTY, nbt.getInt("ShapeBooty"));
		this.companionUuid = nbt.containsUuid("Companion") ? nbt.getUuid("Companion") : null;
		this.anchorPos = net.minecraft.nbt.NbtHelper.toBlockPos(nbt, "Anchor").orElse(null);
		this.leadTarget = net.minecraft.nbt.NbtHelper.toBlockPos(nbt, "LeadTarget").orElse(null);
		this.backpack.readNbtList(nbt.getList("Backpack", net.minecraft.nbt.NbtElement.COMPOUND_TYPE),
				this.getRegistryManager());
	}

	// ------------------------------------------------------------------
	// GeckoLib
	// ------------------------------------------------------------------

	/** Au-delà, on court ; en dessous du second seuil, on marche. */
	private static final double RUN_SPEED = 0.155;
	private static final double FAST_WALK_SPEED = 0.085;
	/** Absence au-delà de laquelle un retour vaut des retrouvailles. */
	private static final int REUNION_TICKS = 2400;

	/** Ce que cette compagne sait faire : chaque sous-classe apporte le sien. */
	public abstract CompanionKind kind();

	/**
	 * L'animation qui tient ce rôle, ou {@code null} si le modèle ne l'a pas.
	 *
	 * <p>Reconstruire le {@link RawAnimation} à chaque appel évite d'avoir à
	 * tenir un cache par personnage pour un objet aussi léger.
	 */
	protected RawAnimation looping(CompanionKind.Role role) {
		String name = this.kind().animation(role);
		return name == null ? null : RawAnimation.begin().thenLoop(name);
	}

	/**
	 * Joue ce rôle, ou rend la main si cette compagne ne le connaît pas.
	 *
	 * <p>Réclamer à GeckoLib une animation absente du fichier lève une erreur :
	 * mieux vaut ne rien jouer et laisser le modèle dans sa pose de repos.
	 */
	private PlayState play(software.bernie.geckolib.animation.AnimationState<CompanionEntity> state,
	                       CompanionKind.Role role) {
		RawAnimation animation = this.looping(role);
		return animation == null ? PlayState.STOP : state.setAndContinue(animation);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// La scène a tout le corps pour elle : les autres contrôleurs se taisent
		// tant qu'elle joue, sans quoi les os qu'elle ne pilote pas garderaient
		// la marche ou le sourire. Chaque animation garde la boucle que lui
		// donne son fichier.
		controllers.add(new AnimationController<CompanionEntity>(this, "scene", 4, state -> {
			if (!this.hasScene()) {
				return PlayState.STOP;
			}
			int serial = this.dataTracker.get(SCENE_SERIAL);
			if (serial != this.seenSceneSerial) {
				this.seenSceneSerial = serial;
				state.getController().forceAnimationReset();
			}
			state.getController().setAnimationSpeed(this.sceneSpeed());
			String scène = this.kind().sceneAnimation(this.getScene());
			// Son introduction d'abord, s'il y en a une ; la scène garde ensuite
			// la boucle de son fichier.
			RawAnimation animation = CompanionScenes.hasIntro(this.kind(), this.getScene())
					? RawAnimation.begin().thenPlay(scène + CompanionScenes.INTRO).thenPlay(scène)
					: RawAnimation.begin().thenPlay(scène);
			return state.setAndContinue(animation);
		}).setSoundKeyframeHandler(événement ->
				// Les sons des scènes sont dans les animations : voir GirlSounds.
				dev.pyrofetch.jn1.sound.GirlSounds.jouer(this, événement.getKeyframeData().getSound())));

		controllers.add(new AnimationController<>(this, "posture", 5, state -> {
			if (this.hasScene()) {
				return PlayState.STOP;
			}
			if (this.isTouchingWater()) {
				return this.play(state, CompanionKind.Role.SWIM);
			}
			if (!this.isOnGround() && this.getVelocity().y < -0.24) {
				return this.play(state, CompanionKind.Role.FALL);
			}
			// Elle guide et s'est arrêtée : le bras tendu de « bowcharge » montre
			// la direction à prendre.
			if (this.isPointing()) {
				return this.play(state, CompanionKind.Role.WAITING);
			}

			if (state.isMoving()) {
				double speed = this.getVelocity().horizontalLength();
				if (this.isRetreating()) {
					return this.play(state, CompanionKind.Role.WALK_BACK);
				}
				if (speed > RUN_SPEED) {
					return this.play(state, CompanionKind.Role.RUN);
				}
				if (speed > FAST_WALK_SPEED) {
					return this.play(state, CompanionKind.Role.FAST_WALK);
				}
				return this.play(state, CompanionKind.Role.WALK);
			}

			// L'acte 3 se voit dans sa tenue debout avant de se dire.
			if (this.isUnsettled()) {
				return this.play(state, CompanionKind.Role.DOWNED);
			}
			if (this.getMode() == JennyMode.IDLE && this.getIdleTicks() > 200) {
				return this.play(state, CompanionKind.Role.SIT);
			}
			return this.play(state, CompanionKind.Role.IDLE);
		}).setSoundKeyframeHandler(événement ->
				dev.pyrofetch.jn1.sound.GirlSounds.repère(this, événement.getKeyframeData().getSound())));

		// Les gestes se superposent à la posture ; « null » est l'animation vide
		// du modèle, qui laisse la posture intacte.
		controllers.add(new AnimationController<>(this, "gesture", 3, state -> {
			CompanionGesture gesture = this.getGesture();
			if (gesture == CompanionGesture.NONE || this.hasScene()) {
				return PlayState.STOP;
			}
			return this.play(state, gesture.role());
		}).setSoundKeyframeHandler(événement ->
				dev.pyrofetch.jn1.sound.GirlSounds.repère(this, événement.getKeyframeData().getSound())));

		// Menottée : les bras seuls, joints dans le dos, par-dessus la posture —
		// elle marche encore si on la tire par sa chaîne.
		controllers.add(new AnimationController<>(this, "cuffs", 4, state -> {
			if (!this.isCuffed() || this.hasScene() || !this.joueLesScènes()) {
				return PlayState.STOP;
			}
			return state.setAndContinue(RawAnimation.begin().thenLoop("animation." + this.kind().id() + ".jn1_cuffed"));
		}));

		// L'expression ne tient que le visage. Pas pendant le câlin (0.85.2) : sa
		// réplique la faisait « parler », et `fhappy` lui plissait les yeux en
		// deux arcs — elle doit l'y regarder dans les yeux (son visage est dans
		// l'animation du câlin).
		controllers.add(new AnimationController<>(this, "face", 4, state -> {
			if (!this.hasScene() && this.getGesture() != CompanionGesture.HUG
					&& (this.isTalking() || this.isDelighted())) {
				return this.play(state, CompanionKind.Role.HAPPY);
			}
			return PlayState.STOP;
		}));
	}

	/** Vrai si elle se déplace vers l'arrière par rapport à son orientation. */
	private boolean isRetreating() {
		net.minecraft.util.math.Vec3d velocity = this.getVelocity();
		if (velocity.horizontalLengthSquared() < 1.0E-4) {
			return false;
		}
		double facing = Math.toRadians(this.bodyYaw);
		double forwardX = -Math.sin(facing);
		double forwardZ = Math.cos(facing);
		return velocity.x * forwardX + velocity.z * forwardZ < -0.01;
	}



	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public boolean canTarget(LivingEntity target) {
		return false;
	}
}
