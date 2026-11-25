package dev.pyrofetch.jn1.client.jouets;

import dev.pyrofetch.jn1.jouets.DildoPogoItem;
import dev.pyrofetch.jn1.network.PogoPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Le mouvement du dildo pogo, fait par le client comme tout déplacement du
 * joueur, et sa jauge, dessinée à la place de la barre d'expérience comme le
 * saut du cheval.
 *
 * <p>Le saut se joue au rythme : pendant la descente, la jauge se remplit à
 * mesure que le sol approche — pleine aux deux derniers ticks. Un appui sur
 * la touche de saut la fige, et ce remplissage est la <em>qualité</em> du
 * rebond qui suit ; un seul essai par descente — en montée, l'appui ne compte
 * pas, sauf juste après le décollage : un appui en retard d'un ou deux ticks
 * relève encore le bond en cours ({@link #RETARD}). Sans appui, on rebondit à peine
 * ({@link #PETIT}, la hauteur d'un saut du jeu) et l'élan se perd ; au bon
 * moment, {@link #GRAND}, environ cinq blocs, et l'élan grandit.
 *
 * <p>Posé au sol, le joueur est remis « en l'air » dans le même tick : le saut
 * du jeu ne remplace pas le rebond, et le frottement du sol ne mange pas
 * l'élan. En l'air, la vitesse au sol tend vers la direction des touches, à
 * une vitesse que la qualité du dernier rebond règle.
 */
public final class PogoClient {
	static final double PETIT = 0.42;
	static final double GRAND = 1.0;
	static final double MAX = 1.2;
	/** Vitesses visées en l'air (blocs par tick) pour un rebond parfait, et la part du chemin faite par tick. */
	static final double VITESSE = 0.6;
	static final double VITESSE_COURSE = 0.75;
	static final double CONTRÔLE = 0.15;
	/** La jauge part de vide à {@code PLEINE + FENÊTRE} ticks du sol, pleine à {@code PLEINE}. */
	static final int PLEINE = 2;
	static final int FENÊTRE = 10;
	/** La qualité d'un appui en retard d'un, puis de deux ticks après le décollage. */
	static final float[] RETARD = {0.7f, 0.4f};

	private static final Identifier FOND = Identifier.ofVanilla("hud/jump_bar_background");
	private static final Identifier PLEIN = Identifier.ofVanilla("hud/jump_bar_progress");
	private static final Identifier GRISÉ = Identifier.ofVanilla("hud/jump_bar_cooldown");

	/** La vitesse verticale du tick d'avant : l'atterrissage l'a déjà remise à zéro. */
	private static double vyAvant;
	private static boolean sautAvant;
	/** Appuyé pendant ce vol, et la qualité figée à l'appui. */
	private static boolean appuyé;
	private static float qualité;
	/** La jauge de ce tick, la qualité du dernier rebond, l'élan qu'il a donné. */
	private static float jauge;
	private static float dernière;
	private static double élan = 0.45;
	/** Ticks depuis le dernier rebond, et si ce rebond a déjà été relevé par un appui en retard. */
	private static int depuisRebond = 99;
	private static boolean relevé;

	private PogoClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(PogoClient::tick);
	}

	private static void tick(MinecraftClient client) {
		var joueur = client.player;
		boolean saut = client.options.jumpKey.isPressed();
		boolean nouvelAppui = saut && !sautAvant;
		sautAvant = saut;
		if (joueur == null || client.world == null || !DildoPogoItem.actif(joueur)) {
			vyAvant = joueur == null ? 0 : joueur.getVelocity().y;
			appuyé = false;
			jauge = 0;
			return;
		}
		Vec3d v = joueur.getVelocity();
		double vx = v.x;
		double vz = v.z;
		double vy = v.y;

		// La jauge : pleine aux derniers ticks avant le sol, vide en montant.
		int avantSol = joueur.isOnGround() ? 0 : ticksAvantSol(joueur);
		jauge = vy > 0 && !joueur.isOnGround() ? 0
				: MathHelper.clamp(1f - (avantSol - PLEINE) / (float) FENÊTRE, 0f, 1f);
		depuisRebond++;
		if (nouvelAppui && !appuyé && !joueur.isOnGround()) {
			if (depuisRebond <= RETARD.length && !relevé) {
				// En retard : le bond en cours est relevé, la descente reste à jouer.
				float q = RETARD[depuisRebond - 1];
				if (q > dernière) {
					vy += MathHelper.lerp(q, PETIT, GRAND) - MathHelper.lerp(dernière, PETIT, GRAND);
					élan = 0.45 + 0.75 * q;
					dernière = q;
				}
				relevé = true;
			} else if (vy <= 0) {
				appuyé = true;
				qualité = jauge;
			}
		}

		// La direction des touches, tournée selon le regard.
		float avant = joueur.input.movementForward;
		float côté = joueur.input.movementSideways;
		if (avant != 0 || côté != 0) {
			double norme = Math.sqrt(avant * avant + côté * côté);
			double lacet = Math.toRadians(joueur.getYaw());
			double sin = MathHelper.sin((float) lacet);
			double cos = MathHelper.cos((float) lacet);
			double dx = (côté * cos - avant * sin) / norme;
			double dz = (avant * cos + côté * sin) / norme;
			boolean course = client.options.sprintKey.isPressed() || joueur.isSprinting();
			double cible = (course ? VITESSE_COURSE : VITESSE) * élan;
			vx += (dx * cible - vx) * CONTRÔLE;
			vz += (dz * cible - vz) * CONTRÔLE;
		}

		if (joueur.isOnGround()) {
			// Un appui dans le tick même de l'atterrissage : parfait.
			float q = appuyé ? qualité : nouvelAppui ? 1f : 0f;
			vy = MathHelper.lerp(q, PETIT, GRAND);
			// Une chute de haut rend un rebond plus haut, si l'on a su le prendre.
			vy = Math.max(vy, Math.min(MAX, -vyAvant * 0.6 * q));
			// Raté, l'élan se perd ; parfait, il grandit un peu.
			double garde = 0.65 + 0.45 * q;
			vx *= garde;
			vz *= garde;
			élan = 0.45 + 0.75 * q;
			dernière = q;
			appuyé = false;
			depuisRebond = 0;
			relevé = false;
			joueur.setOnGround(false);
			joueur.fallDistance = 0;
			client.world.playSound(joueur, joueur.getX(), joueur.getY(), joueur.getZ(), SoundEvents.ENTITY_SLIME_JUMP,
					SoundCategory.PLAYERS, 0.35f + 0.4f * q, 1.3f - 0.5f * q + joueur.getRandom().nextFloat() * 0.1f);
			ClientPlayNetworking.send(new PogoPayload(q));
		}
		joueur.setVelocity(vx, vy, vz);
		vyAvant = vy;
	}

	/** Combien de ticks avant de toucher le sol, à la physique du jeu (32 au plus). */
	private static int ticksAvantSol(ClientPlayerEntity joueur) {
		Vec3d pieds = joueur.getPos();
		var touché = joueur.getWorld().raycast(new RaycastContext(pieds, pieds.add(0, -40, 0),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, joueur));
		if (touché.getType() == HitResult.Type.MISS) {
			return 32;
		}
		double hauteur = pieds.y - touché.getPos().y;
		double vy = joueur.getVelocity().y;
		for (int n = 1; n <= 32; n++) {
			hauteur += vy;
			if (hauteur <= 0) {
				return n;
			}
			vy = (vy - 0.08) * 0.98;
		}
		return 32;
	}

	/** La jauge remplace la barre d'expérience tant qu'on est sur le ressort. */
	public static boolean jaugeAffichée() {
		var joueur = MinecraftClient.getInstance().player;
		return joueur != null && DildoPogoItem.actif(joueur);
	}

	/** Dessinée comme celle du cheval : au saut figé, en couleur ; en montant, le dernier rebond, grisé. */
	public static void dessinerJauge(DrawContext contexte, int x) {
		int y = contexte.getScaledWindowHeight() - 32 + 3;
		contexte.drawGuiTexture(FOND, x, y, 182, 5);
		var joueur = MinecraftClient.getInstance().player;
		boolean monte = joueur != null && joueur.getVelocity().y > 0 && !appuyé;
		float remplissage = appuyé ? qualité : monte ? dernière : jauge;
		int largeur = (int) (remplissage * 183f);
		if (largeur > 0) {
			contexte.drawGuiTexture(monte ? GRISÉ : PLEIN, 182, 5, 0, 0, x, y, largeur, 5);
		}
	}

	/** Ce que la jauge montre (pour les essais). */
	public static float[] relevé() {
		return new float[]{jauge, appuyé ? qualité : -1, dernière};
	}
}
