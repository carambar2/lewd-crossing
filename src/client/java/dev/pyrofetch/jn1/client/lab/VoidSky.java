package dev.pyrofetch.jn1.client.lab;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.pyrofetch.jn1.JN1;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
 */
public final class VoidSky implements DimensionRenderingRegistry.SkyRenderer {
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

	@Override
	public void render(WorldRenderContext contexte) {
		ClientWorld monde = contexte.world();
		if (monde == null) {
			return;
		}
		// L'horloge des animations. Ramenée à un cycle long pour garder la
		// précision d'un float : le défilement reste continu, à la jointure près.
		float t = (monde.getTime() % 240_000L) + contexte.tickCounter().getTickDelta(false);

		MatrixStack pile = new MatrixStack();
		pile.multiplyPositionMatrix(contexte.positionMatrix());
		Matrix4f m = pile.peek().getPositionMatrix();

		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

		// Le tunnel, en lumière ajoutée : ce qui est noir dans les planches ne
		// dessine rien.
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
		tube(m, NÉBULEUSE, 2, 3, t * 0.0004F, 0.0F, 1.0F);
		tube(m, COURANT, 4, 5, t * 0.0035F, 0.0F, 1.0F);
		tube(m, COURANT, 3, 3, t * 0.0021F + 0.37F, 0.5F, 0.55F);
		lueur(m, LONGUEUR - 1.0F, 13.0F, 1.0F);
		lueur(m, -(LONGUEUR - 1.0F), 9.0F, 0.45F);

		// Les fenêtres, par-dessus : elles cachent le courant derrière elles.
		RenderSystem.defaultBlendFunc();
		for (int i = 0; i < FENÊTRES.length; i++) {
			FENÊTRES[i].dessiner(m, t, i);
		}
		// Les cumtrails des avions qui passent, devant tout le reste.
		VoidCumtrails.dessiner(m, t);

		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
	}

	/**
	 * Une couche du tube. La planche fait {@code tours} fois le tour et
	 * {@code répétitions} fois la longueur ; elle glisse de {@code défilement}
	 * vers l'est.
	 */
	private static void tube(Matrix4f m, Identifier planche, int tours, int répétitions, float défilement,
	                         float décalage, float intensité) {
		RenderSystem.setShaderTexture(0, planche);
		BufferBuilder tampon = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION_TEXTURE_COLOR);
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
				float a = MathHelper.TAU * j / SEGMENTS;
				float b = MathHelper.TAU * (j + 1) / SEGMENTS;
				float ua = (float) j / SEGMENTS * tours + décalage;
				float ub = (float) (j + 1) / SEGMENTS * tours + décalage;
				tampon.vertex(m, x1, r1 * MathHelper.cos(a), r1 * MathHelper.sin(a)).texture(ua, v1).color(c1);
				tampon.vertex(m, x1, r1 * MathHelper.cos(b), r1 * MathHelper.sin(b)).texture(ub, v1).color(c1);
				tampon.vertex(m, x2, r2 * MathHelper.cos(b), r2 * MathHelper.sin(b)).texture(ub, v2).color(c2);
				tampon.vertex(m, x2, r2 * MathHelper.cos(a), r2 * MathHelper.sin(a)).texture(ua, v2).color(c2);
			}
		}
		BufferRenderer.drawWithGlobalProgram(tampon.end());
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
		RenderSystem.setShaderTexture(0, LUEUR);
		BufferBuilder tampon = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION_TEXTURE_COLOR);
		int c = couleur(1.0F, 1.0F, 1.0F, intensité);
		tampon.vertex(m, x, -taille, -taille).texture(0, 0).color(c);
		tampon.vertex(m, x, taille, -taille).texture(1, 0).color(c);
		tampon.vertex(m, x, taille, taille).texture(1, 1).color(c);
		tampon.vertex(m, x, -taille, taille).texture(0, 1).color(c);
		BufferRenderer.drawWithGlobalProgram(tampon.end());
	}

	private static int couleur(float r, float g, float b, float a) {
		return MathHelper.clamp((int) (a * 255), 0, 255) << 24
				| MathHelper.clamp((int) (r * 255), 0, 255) << 16
				| MathHelper.clamp((int) (g * 255), 0, 255) << 8
				| MathHelper.clamp((int) (b * 255), 0, 255);
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
			float i = this.inclinaison * MathHelper.RADIANS_PER_DEGREE;
			Vector3f dr = new Vector3f(droite).mul(MathHelper.cos(i)).add(new Vector3f(haut).mul(MathHelper.sin(i)));
			Vector3f ht = new Vector3f(haut).mul(MathHelper.cos(i)).sub(new Vector3f(droite).mul(MathHelper.sin(i)));

			// L'ondulation : chaque coin respire à son rythme, et l'éclat avec.
			float phase = rang * 1.7F;
			float[] souffle = new float[4];
			for (int k = 0; k < 4; k++) {
				souffle[k] = 1.0F + 0.05F * MathHelper.sin(t * 0.021F + phase + k * 1.9F);
			}
			int c = couleur(1.0F, 1.0F, 1.0F, 0.82F + 0.18F * MathHelper.sin(t * 0.033F + phase));

			Vector3f centre = new Vector3f(d).mul(DISTANCE);
			RenderSystem.setShaderTexture(0, this.planche);
			BufferBuilder tampon = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION_TEXTURE_COLOR);
			coin(tampon, m, centre, dr, ht, -this.largeur * souffle[0], this.hauteur * souffle[0], 0, 0, c);
			coin(tampon, m, centre, dr, ht, this.largeur * souffle[1], this.hauteur * souffle[1], 1, 0, c);
			coin(tampon, m, centre, dr, ht, this.largeur * souffle[2], -this.hauteur * souffle[2], 1, 1, c);
			coin(tampon, m, centre, dr, ht, -this.largeur * souffle[3], -this.hauteur * souffle[3], 0, 1, c);
			BufferRenderer.drawWithGlobalProgram(tampon.end());
		}

		private static void coin(BufferBuilder tampon, Matrix4f m, Vector3f centre, Vector3f dr, Vector3f ht,
		                         float x, float y, float u, float v, int c) {
			tampon.vertex(m, centre.x + dr.x * x + ht.x * y, centre.y + dr.y * x + ht.y * y,
					centre.z + dr.z * x + ht.z * y).texture(u, v).color(c);
		}
	}
}
