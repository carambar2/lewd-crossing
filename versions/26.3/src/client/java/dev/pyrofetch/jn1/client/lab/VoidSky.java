package dev.pyrofetch.jn1.client.lab;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.lab.LabDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Le ciel du vide : le couloir entre les univers.
 *
 * <p>Un tunnel couché le long de l'axe est-ouest, vu de l'intérieur. Sa paroi
 * porte trois couches qui s'additionnent à la lumière : une brume sombre marquée
 * d'anneaux rompus, puis deux nappes de filaments qui filent vers l'est à des
 * vitesses différentes. Le tube s'amincit vers ses deux bouts, si bien qu'il
 * fuit en perspective ; une lueur bouche chacun de ses points de fuite, plus
 * vive à l'est, où va le courant. Par-dessus, des fenêtres ouvertes sur
 * d'autres univers flottent à distance infinie : on ne les atteint jamais.
 * Devant tout, des avions passent et y laissent leurs cumtrails
 * ({@link VoidCumtrails}).
 *
 * <p>Tout est dessiné autour de l'œil, sans translation : le ciel suit le joueur
 * et ne se rapproche jamais. Rien ne dépasse cent blocs, la limite où le jeu
 * pose ses propres astres — au-delà, une petite distance de rendu couperait le
 * ciel par le plan du fond.
 *
 * <p>Les textures viennent de {@code tools/gen_void_sky.py}. Rien de ceci n'est
 * vu par le serveur de test : il faut le jeu.
 *
 * <p>26.3 : plus de {@code DimensionRenderingRegistry}. Le ciel est une passe
 * du graphe de rendu du monde, ajoutée à la place de celle du jeu par
 * {@code LevelRendererSkyMixin} quand on est dans le vide ; le dessin passe par
 * {@link DessinCiel}.
 */
public final class VoidSky {
	private static final Identifier NÉBULEUSE = JN1.id("textures/environment/void_nebula.png");
	private static final Identifier COURANT = JN1.id("textures/environment/void_current.png");
	private static final Identifier LUEUR = JN1.id("textures/environment/void_glow.png");

	/** La demi-longueur du tube et son rayon au milieu, en blocs. */
	private static final float LONGUEUR = 95.0F;
	private static final float RAYON = 28.0F;
	/** Des tranches le long du tube, des segments autour. */
	private static final int TRANCHES = 24;
	private static final int SEGMENTS = 48;

	/**
	 * Les fenêtres : direction, demi-largeur et demi-hauteur de la planche en
	 * blocs, à la distance où elles sont posées (la déchirure en occupe les deux
	 * tiers, le reste est son halo), inclinaison en degrés, planche. Aucune n'approche de l'axe : elles
	 * cacheraient les points de fuite.
	 */
	private static final Fenêtre[] FENÊTRES = {
			new Fenêtre(0.25F, 0.30F, 1.0F, 17, 12, 12, "chaud"),
			new Fenêtre(-0.35F, 0.45F, -1.0F, 10, 13, -17, "mosaique"),
			new Fenêtre(-0.2F, -0.30F, 1.0F, 9, 7, 29, "profond"),
			new Fenêtre(0.1F, 1.0F, 0.3F, 10, 12, -6, "aplats"),
			new Fenêtre(0.35F, 0.1F, -1.0F, 13, 9, 8, "braise"),
			new Fenêtre(-0.45F, -0.6F, -0.8F, 12, 10, -23, "brume"),
	};

	private VoidSky() {
	}

	/** Le ciel du vide remplace celui du jeu : on est dans le laboratoire. */
	public static boolean actif() {
		ClientLevel monde = Minecraft.getInstance().level;
		return monde != null && monde.dimension().equals(LabDimension.LAB);
	}

	/** La passe du ciel : appelée par le graphe de rendu, avant le terrain. */
	public static void dessiner() {
		Minecraft client = Minecraft.getInstance();
		ClientLevel monde = client.level;
		if (monde == null) {
			return;
		}
		// L'horloge des animations. Ramenée à un cycle long pour garder la
		// précision d'un float : le défilement reste continu, à la jointure près.
		float t = (monde.getGameTime() % 240_000L) + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		// Les sommets sont posés autour de l'œil dans les axes du monde : la
		// rotation de la vue est celle de la passe (la pile du modèle).
		Matrix4f m = new Matrix4f();

		DessinCiel.passe(client.gameRenderer.mainRenderTarget(), planches(), () -> {
			// Le tunnel, en lumière ajoutée : ce qui est noir dans les planches ne
			// dessine rien.
			DessinCiel.additif();
			tube(m, NÉBULEUSE, 2, 3, t * 0.0004F, 0.0F, 1.0F);
			tube(m, COURANT, 4, 5, t * 0.0035F, 0.0F, 1.0F);
			tube(m, COURANT, 3, 3, t * 0.0021F + 0.37F, 0.5F, 0.55F);
			lueur(m, LONGUEUR - 1.0F, 13.0F, 1.0F);
			lueur(m, -(LONGUEUR - 1.0F), 9.0F, 0.45F);

			// Les fenêtres, par-dessus : elles cachent le courant derrière elles.
			DessinCiel.translucide();
			for (int i = 0; i < FENÊTRES.length; i++) {
				FENÊTRES[i].dessiner(m, t, i);
			}
			// Les cumtrails des avions qui passent, devant tout le reste.
			VoidCumtrails.dessiner(m, t);
		});
	}

	/** Toutes les planches du ciel : le tube, les lueurs, les fenêtres, les cumtrails. */
	private static List<Identifier> planches() {
		List<Identifier> toutes = new ArrayList<>(List.of(NÉBULEUSE, COURANT, LUEUR));
		for (Fenêtre fenêtre : FENÊTRES) {
			toutes.add(fenêtre.planche());
		}
		toutes.addAll(VoidCumtrails.PLANCHES);
		return toutes;
	}

	/**
	 * Une couche du tube. La planche fait {@code tours} fois le tour et
	 * {@code répétitions} fois la longueur ; elle glisse de {@code défilement}
	 * vers l'est.
	 */
	private static void tube(Matrix4f m, Identifier planche, int tours, int répétitions, float défilement,
	                         float décalage, float intensité) {
		BufferBuilder tampon = DessinCiel.commencer(planche);
		for (int i = 0; i < TRANCHES; i++) {
			float x1 = -LONGUEUR + 2 * LONGUEUR * i / TRANCHES;
			float x2 = -LONGUEUR + 2 * LONGUEUR * (i + 1) / TRANCHES;
			float r1 = rayon(x1);
			float r2 = rayon(x2);
			float v1 = x1 / (2 * LONGUEUR) * répétitions - défilement;
			float v2 = x2 / (2 * LONGUEUR) * répétitions - défilement;
			int c1 = couleur(1.0F, 1.0F, 1.0F, intensité * fondu(x1));
			int c2 = couleur(1.0F, 1.0F, 1.0F, intensité * fondu(x2));
			for (int j = 0; j < SEGMENTS; j++) {
				float a = Mth.TWO_PI * j / SEGMENTS;
				float b = Mth.TWO_PI * (j + 1) / SEGMENTS;
				float ua = (float) j / SEGMENTS * tours + décalage;
				float ub = (float) (j + 1) / SEGMENTS * tours + décalage;
				tampon.addVertex(m, x1, r1 * Mth.cos(a), r1 * Mth.sin(a)).setUv(ua, v1).setColor(c1);
				tampon.addVertex(m, x1, r1 * Mth.cos(b), r1 * Mth.sin(b)).setUv(ub, v1).setColor(c1);
				tampon.addVertex(m, x2, r2 * Mth.cos(b), r2 * Mth.sin(b)).setUv(ub, v2).setColor(c2);
				tampon.addVertex(m, x2, r2 * Mth.cos(a), r2 * Mth.sin(a)).setUv(ua, v2).setColor(c2);
			}
		}
		DessinCiel.dessiner(tampon);
	}

	/** Le rayon du tube à cette abscisse : plein au milieu, presque nul aux bouts, d'où la fuite. */
	private static float rayon(float x) {
		float f = Math.abs(x) / LONGUEUR;
		return RAYON * (1.0F - 0.92F * (float) Math.pow(f, 1.6));
	}

	/** Près des bouts, tout se resserre en quelques pixels : on l'éteint un peu pour éviter le moiré. */
	private static float fondu(float x) {
		float f = Math.abs(x) / LONGUEUR;
		return 1.0F - 0.5F * f * f * f * f;
	}

	/** La lueur d'un point de fuite, face à l'axe. */
	private static void lueur(Matrix4f m, float x, float taille, float intensité) {
		BufferBuilder tampon = DessinCiel.commencer(LUEUR);
		int c = couleur(1.0F, 1.0F, 1.0F, intensité);
		tampon.addVertex(m, x, -taille, -taille).setUv(0, 0).setColor(c);
		tampon.addVertex(m, x, taille, -taille).setUv(1, 0).setColor(c);
		tampon.addVertex(m, x, taille, taille).setUv(1, 1).setColor(c);
		tampon.addVertex(m, x, -taille, taille).setUv(0, 1).setColor(c);
		DessinCiel.dessiner(tampon);
	}

	private static int couleur(float r, float g, float b, float a) {
		return Mth.clamp((int) (a * 255), 0, 255) << 24
				| Mth.clamp((int) (r * 255), 0, 255) << 16
				| Mth.clamp((int) (g * 255), 0, 255) << 8
				| Mth.clamp((int) (b * 255), 0, 255);
	}

	/** Une déchirure sur un autre univers, posée dans le ciel. Elle ondule et son éclat palpite. */
	private record Fenêtre(Vector3f direction, float largeur, float hauteur, float inclinaison, Identifier planche) {
		/** La distance où elles sont posées : sous la limite des cent blocs. */
		private static final float DISTANCE = 90.0F;

		Fenêtre(float x, float y, float z, float largeur, float hauteur, float inclinaison, String nom) {
			this(new Vector3f(x, y, z).normalize(), largeur * 1.2F, hauteur * 1.2F, inclinaison,
					JN1.id("textures/environment/void_window_" + nom + ".png"));
		}

		void dessiner(Matrix4f m, float t, int rang) {
			// Un repère sur la fenêtre, vue de l'œil : sa droite et son haut, inclinés.
			Vector3f d = this.direction;
			Vector3f droite = new Vector3f(d).cross(0, 1, 0);
			if (droite.lengthSquared() < 1.0E-4F) {
				droite.set(-1, 0, 0);
			}
			droite.normalize();
			Vector3f haut = new Vector3f(droite).cross(d).normalize();
			float i = this.inclinaison * Mth.DEG_TO_RAD;
			Vector3f dr = new Vector3f(droite).mul(Mth.cos(i)).add(new Vector3f(haut).mul(Mth.sin(i)));
			Vector3f ht = new Vector3f(haut).mul(Mth.cos(i)).sub(new Vector3f(droite).mul(Mth.sin(i)));

			// L'ondulation : chaque coin respire à son rythme, et l'éclat avec.
			float phase = rang * 1.7F;
			float[] souffle = new float[4];
			for (int k = 0; k < 4; k++) {
				souffle[k] = 1.0F + 0.05F * Mth.sin(t * 0.021F + phase + k * 1.9F);
			}
			int c = couleur(1.0F, 1.0F, 1.0F, 0.82F + 0.18F * Mth.sin(t * 0.033F + phase));

			Vector3f centre = new Vector3f(d).mul(DISTANCE);
			BufferBuilder tampon = DessinCiel.commencer(this.planche);
			coin(tampon, m, centre, dr, ht, -this.largeur * souffle[0], this.hauteur * souffle[0], 0, 0, c);
			coin(tampon, m, centre, dr, ht, this.largeur * souffle[1], this.hauteur * souffle[1], 1, 0, c);
			coin(tampon, m, centre, dr, ht, this.largeur * souffle[2], -this.hauteur * souffle[2], 1, 1, c);
			coin(tampon, m, centre, dr, ht, -this.largeur * souffle[3], -this.hauteur * souffle[3], 0, 1, c);
			DessinCiel.dessiner(tampon);
		}

		private static void coin(BufferBuilder tampon, Matrix4f m, Vector3f centre, Vector3f dr, Vector3f ht,
		                         float x, float y, float u, float v, int c) {
			tampon.addVertex(m, centre.x + dr.x * x + ht.x * y, centre.y + dr.y * x + ht.y * y,
					centre.z + dr.z * x + ht.z * y).setUv(u, v).setColor(c);
		}
	}
}
