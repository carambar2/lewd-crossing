package dev.pyrofetch.jn1.client.lab;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.lab.LabDimension;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Les épaves du vide : des fragments arrachés à d'autres univers, qui dérivent
 * et tournent lentement sur eux-mêmes.
 *
 * <p>Ce ne sont ni des blocs ni des entités : le client les dessine, et rien
 * d'autre ne les connaît. On passe au travers. Leur place se déduit de la
 * position seule — le vide est découpé en cases de {@value #CASE} blocs, et
 * chaque case tire la sienne d'une graine qui ne dépend que de ses
 * coordonnées —, si bien qu'elles sont au même endroit à chaque visite, pour
 * chaque joueur, sans rien sauvegarder ni générer.
 *
 * <p>Elles restent hors de la tranche des laboratoires ({@value #LABO_BAS} à
 * {@value #LABO_HAUT}) : aucune ne traverse un mur. La plupart flottent
 * au-dessus, là où l'on arrive et où l'on nage.
 *
 * <p>Quatre sortes : une motte de terre au dessus coloré, parfois plantée d'un
 * poteau lumineux ; un amas de cristaux qui luisent ; une dalle en damier et son
 * pilier ; une poignée d'éclats. Les teintes sont tirées de palettes sans modèle
 * — des matières d'ailleurs, rien de reconnaissable. Le vide n'éclairant rien,
 * elles portent leur propre clarté, légèrement bleuie par le courant.
 */
public final class VoidDebris {
	private static final Identifier PLANCHE = JN1.id("textures/misc/void_debris.png");
	private static final RenderType COUCHE = RenderTypes.entitySolid(PLANCHE);

	/** Le côté d'une case, en blocs : une épave au plus par case. */
	private static final int CASE = 48;
	/** Jusqu'où l'on en dessine, en cases autour de l'œil. Au-delà, la brume les a mangées. */
	private static final int PORTÉE = 3;
	private static final int LABO_BAS = 28;
	private static final int LABO_HAUT = 58;

	/** La clarté d'une épave ordinaire, et celle de ce qui luit. */
	private static final int CLARTÉ = LightCoordsUtil.pack(11, 0);
	private static final int LUMINEUX = LightCoordsUtil.FULL_BRIGHT;

	/** Les épaves déjà tirées, par case. Rien ne s'y perd à les retirer : elles se retirent à l'identique. */
	private static final Map<Long, Épave> TIRÉES = new HashMap<>();
	private static final Épave AUCUNE = new Épave(Vec3.ZERO, new Vector3f(0, 1, 0), 0, 0, List.of());

	private VoidDebris() {
	}

	/**
	 * 26.3 : les épaves sont « soumises » au rendu du monde avec les entités
	 * ({@code COLLECT_SUBMITS}), et dessinées par le jeu avec elles, dans la
	 * même couche qu'avant.
	 */
	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(VoidDebris::dessiner);
	}

	private static void dessiner(LevelRenderContext contexte) {
		ClientLevel monde = Minecraft.getInstance().level;
		SubmitNodeCollector tampons = contexte.submitNodeCollector();
		PoseStack pile = contexte.poseStack();
		if (monde == null || tampons == null || pile == null || !monde.dimension().equals(LabDimension.LAB)) {
			return;
		}
		LevelRenderState état = contexte.levelState();
		Vec3 œil = état.cameraRenderState.pos;
		Frustum champ = état.cameraRenderState.cullFrustum;
		float t = (monde.getGameTime() % 240_000L) + état.worldPartialTicks;
		int cx = Mth.floor(œil.x / CASE);
		int cz = Mth.floor(œil.z / CASE);
		if (TIRÉES.size() > 1024) {
			TIRÉES.clear();
		}
		for (int i = cx - PORTÉE; i <= cx + PORTÉE; i++) {
			for (int j = cz - PORTÉE; j <= cz + PORTÉE; j++) {
				int ci = i;
				int cj = j;
				Épave épave = TIRÉES.computeIfAbsent(clé(i, j), k -> tirer(ci, cj));
				if (épave == AUCUNE) {
					continue;
				}
				Vec3 centre = épave.centre.add(0, 1.2 * Mth.sin(t * 0.012F + épave.phase), 0);
				if (champ != null && !champ.isVisible(new AABB(centre, centre).inflate(7))) {
					continue;
				}
				pile.pushPose();
				pile.translate(centre.x - œil.x, centre.y - œil.y, centre.z - œil.z);
				pile.rotate(new Quaternionf().rotationAxis(t * épave.vitesse + épave.phase, épave.axe));
				List<Boîte> boîtes = épave.boîtes;
				tampons.submitCustomGeometry(pile, COUCHE, (repère, tampon) -> {
					for (Boîte boîte : boîtes) {
						boîte.émettre(repère, tampon);
					}
				});
				pile.popPose();
			}
		}
	}

	private static long clé(int i, int j) {
		return ((long) i << 32) ^ (j & 0xFFFFFFFFL);
	}

	// --- le tirage -------------------------------------------------------------------

	private static final int[] DESSUS = {0x59D9C7, 0x9BE36B, 0x5A6FE0, 0xF08A6E, 0xC7A2F0};
	private static final int[] TERRE = {0x9E5C66, 0xB08A4A, 0x55607A, 0x6E3F6B};
	private static final int[] CRISTAL = {0xBFA6FF, 0x8FF0E6, 0xFFB3E1, 0xFFE38F};
	private static final int[][] DAMIER = {{0xE6DDCC, 0x1F1F24}, {0xCFE8F0, 0x2B3550}, {0xF2C4C4, 0x3A1F2A}};
	private static final int PIERRE = 0x2E2A3A;
	private static final int PILIER = 0x8C3348;

	private static Épave tirer(int i, int j) {
		Random hasard = new Random(i * 0x9E3779B97F4A7C15L ^ j * 0xC2B2AE3D27D4EB4FL ^ 0x5EEDL);
		if (hasard.nextFloat() > 0.55F) {
			return AUCUNE;
		}
		double x = i * CASE + 8 + hasard.nextDouble() * (CASE - 16);
		double z = j * CASE + 8 + hasard.nextDouble() * (CASE - 16);
		// Surtout au-dessus des laboratoires ; quelques-unes dessous.
		double y = hasard.nextFloat() < 0.8F ? LABO_HAUT + 6 + hasard.nextDouble() * 52
				: 6 + hasard.nextDouble() * (LABO_BAS - 12);
		Vector3f axe = new Vector3f(hasard.nextFloat() - 0.5F, hasard.nextFloat() - 0.5F + 0.6F,
				hasard.nextFloat() - 0.5F).normalize();
		float vitesse = (0.0015F + hasard.nextFloat() * 0.005F) * (hasard.nextBoolean() ? 1 : -1);
		float phase = hasard.nextFloat() * Mth.TWO_PI;
		float sorte = hasard.nextFloat();
		List<Boîte> boîtes = sorte < 0.4F ? motte(hasard)
				: sorte < 0.6F ? cristaux(hasard)
				: sorte < 0.75F ? dalle(hasard)
				: éclats(hasard);
		return new Épave(new Vec3(x, y, z), axe, vitesse, phase, boîtes);
	}

	private static List<Boîte> motte(Random hasard) {
		List<Boîte> boîtes = new ArrayList<>();
		int dessus = choisir(hasard, DESSUS);
		int terre = choisir(hasard, TERRE);
		int rx = 2 + hasard.nextInt(3);
		int rz = 2 + hasard.nextInt(2);
		for (int x = -rx; x <= rx; x++) {
			for (int z = -rz; z <= rz; z++) {
				float e = (float) (x * x) / (rx * rx) + (float) (z * z) / (rz * rz);
				if (e > 1.05F) {
					continue;
				}
				// Plus épaisse au milieu, qui pend en dessous.
				int épaisseur = 1 + hasard.nextInt(2) + (e < 0.4F ? 1 + hasard.nextInt(2) : 0);
				boîtes.add(new Boîte(x - 0.5F, -épaisseur - 0.5F, z - 0.5F, x + 0.5F, -0.5F, z + 0.5F,
						Carreau.GRAIN, terre, false));
				boîtes.add(new Boîte(x - 0.5F, -0.5F, z - 0.5F, x + 0.5F, 0.5F, z + 0.5F,
						Carreau.DESSUS, dessus, false));
			}
		}
		if (hasard.nextFloat() < 0.35F) {
			boîtes.add(new Boîte(-0.2F, 0.5F, -0.2F, 0.2F, 2.5F, 0.2F, Carreau.CRISTAL, choisir(hasard, CRISTAL), true));
		}
		return boîtes;
	}

	private static List<Boîte> cristaux(Random hasard) {
		List<Boîte> boîtes = new ArrayList<>();
		boîtes.add(new Boîte(-1.5F, -1.0F, -1.5F, 1.5F, 0.0F, 1.5F, Carreau.GRAIN, PIERRE, false));
		int teinte = choisir(hasard, CRISTAL);
		int n = 5 + hasard.nextInt(4);
		for (int k = 0; k < n; k++) {
			float x = -1.2F + hasard.nextFloat() * 2.4F;
			float z = -1.2F + hasard.nextFloat() * 2.4F;
			float h = 1.5F + hasard.nextFloat() * 3.0F;
			float l = 0.2F + hasard.nextFloat() * 0.15F;
			boîtes.add(new Boîte(x - l, 0.0F, z - l, x + l, h, z + l, Carreau.CRISTAL, teinte, true));
		}
		return boîtes;
	}

	private static List<Boîte> dalle(Random hasard) {
		List<Boîte> boîtes = new ArrayList<>();
		int[] paire = DAMIER[hasard.nextInt(DAMIER.length)];
		int n = 3 + hasard.nextInt(3);
		float o = -n / 2.0F;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				boîtes.add(new Boîte(o + x, -0.2F, o + z, o + x + 1, 0.2F, o + z + 1,
						Carreau.CARREAU, paire[(x + z) % 2], false));
			}
		}
		float h = 2.0F + hasard.nextFloat() * 1.5F;
		boîtes.add(new Boîte(o, 0.2F, o, o + 0.6F, 0.2F + h, o + 0.6F, Carreau.CARREAU, PILIER, false));
		return boîtes;
	}

	private static List<Boîte> éclats(Random hasard) {
		List<Boîte> boîtes = new ArrayList<>();
		int n = 5 + hasard.nextInt(5);
		for (int k = 0; k < n; k++) {
			float s = 0.15F + hasard.nextFloat() * 0.25F;
			float x = (hasard.nextFloat() - 0.5F) * 6;
			float y = (hasard.nextFloat() - 0.5F) * 4;
			float z = (hasard.nextFloat() - 0.5F) * 6;
			Carreau carreau = Carreau.values()[hasard.nextInt(Carreau.values().length)];
			int teinte = switch (carreau) {
				case GRAIN -> choisir(hasard, TERRE);
				case DESSUS -> choisir(hasard, DESSUS);
				case CRISTAL -> choisir(hasard, CRISTAL);
				case CARREAU -> DAMIER[hasard.nextInt(DAMIER.length)][hasard.nextInt(2)];
			};
			boîtes.add(new Boîte(x - s, y - s, z - s, x + s, y + s, z + s, carreau, teinte, carreau == Carreau.CRISTAL));
		}
		return boîtes;
	}

	private static int choisir(Random hasard, int[] teintes) {
		return teintes[hasard.nextInt(teintes.length)];
	}

	// --- ce qui se dessine -----------------------------------------------------------

	/** Les carreaux de la planche, dans l'ordre où {@code gen_void_sky.py} les peint. */
	private enum Carreau {
		GRAIN, DESSUS, CRISTAL, CARREAU;

		float u1() {
			return this.ordinal() / 4.0F;
		}

		float u2() {
			return (this.ordinal() + 1) / 4.0F;
		}
	}

	private record Épave(Vec3 centre, Vector3f axe, float vitesse, float phase, List<Boîte> boîtes) {
	}

	/** Une boîte en blocs, autour du centre de l'épave. La planche est grise : la teinte fait la matière. */
	private record Boîte(float x1, float y1, float z1, float x2, float y2, float z2,
	                     Carreau carreau, int couleur, boolean lumineux) {
		/** Le courant bleuit un peu tout ce qui dérive. */
		private static final int COURANT = 0x7373E6;

		void émettre(PoseStack.Pose repère, VertexConsumer tampon) {
			int c = 0xFF000000 | mêler(this.couleur, COURANT, 0.15F);
			int l = this.lumineux ? LUMINEUX : CLARTÉ;
			float u1 = this.carreau.u1();
			float u2 = this.carreau.u2();
			Matrix4f m = repère.pose();
			float a = this.x1, b = this.y1, d = this.z1, e = this.x2, f = this.y2, g = this.z2;
			// Chaque face dans le sens inverse des aiguilles d'une montre, vue de dehors.
			face(tampon, m, repère, c, l, u1, u2, 0, 0, -1, e, f, d, a, f, d, a, b, d, e, b, d);
			face(tampon, m, repère, c, l, u1, u2, 0, 0, 1, a, f, g, e, f, g, e, b, g, a, b, g);
			face(tampon, m, repère, c, l, u1, u2, 1, 0, 0, e, f, g, e, f, d, e, b, d, e, b, g);
			face(tampon, m, repère, c, l, u1, u2, -1, 0, 0, a, f, d, a, f, g, a, b, g, a, b, d);
			face(tampon, m, repère, c, l, u1, u2, 0, 1, 0, a, f, d, e, f, d, e, f, g, a, f, g);
			face(tampon, m, repère, c, l, u1, u2, 0, -1, 0, a, b, g, e, b, g, e, b, d, a, b, d);
		}

		/** Une face : ses coins haut-gauche, haut-droite, bas-droite, bas-gauche vus de dehors. */
		private static void face(VertexConsumer tampon, Matrix4f m, PoseStack.Pose repère, int c, int l,
		                         float u1, float u2, float nx, float ny, float nz,
		                         float ax, float ay, float az, float bx, float by, float bz,
		                         float cx, float cy, float cz, float dx, float dy, float dz) {
			sommet(tampon, m, repère, ax, ay, az, u1, 0, c, l, nx, ny, nz);
			sommet(tampon, m, repère, dx, dy, dz, u1, 1, c, l, nx, ny, nz);
			sommet(tampon, m, repère, cx, cy, cz, u2, 1, c, l, nx, ny, nz);
			sommet(tampon, m, repère, bx, by, bz, u2, 0, c, l, nx, ny, nz);
		}

		private static void sommet(VertexConsumer tampon, Matrix4f m, PoseStack.Pose repère,
		                           float x, float y, float z, float u, float v, int c, int l,
		                           float nx, float ny, float nz) {
			tampon.addVertex(m, x, y, z).setColor(c).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(l)
					.setNormal(repère, nx, ny, nz);
		}

		private static int mêler(int a, int b, float f) {
			int r = (int) Mth.lerpInt(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
			int g = (int) Mth.lerpInt(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
			int bl = (int) Mth.lerpInt(f, a & 0xFF, b & 0xFF);
			return r << 16 | g << 8 | bl;
		}
	}
}
