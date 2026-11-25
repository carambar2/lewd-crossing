package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;

/**
 * De quoi dessiner à la main, dans un rendu d'entité ou d'entité de bloc : des
 * boîtes texturées et des aplats colorés.
 *
 * <p>Les coordonnées sont en pixels de bloc — seize par bloc —, comme dans les
 * modèles JSON : on transpose un élément de modèle sans rien recalculer.
 */
public final class Geometrie {
	private static final float PX = 1.0F / 16.0F;

	private Geometrie() {
	}

	/**
	 * Une boîte, ses six faces tirées du sprite comme le ferait un modèle : chaque
	 * face prend la portion de texture qui correspond à sa place dans le bloc.
	 */
	public static void boîte(PoseStack.Pose repère, VertexConsumer tampon, TextureAtlasSprite sprite,
	                         float x1, float y1, float z1, float x2, float y2, float z2,
	                         int lumière, int couleur) {
		// Nord et sud.
		face(repère, tampon, sprite, couleur, lumière, 0, 0, -1,
				x2, y2, z1, x1, y2, z1, x1, y1, z1, x2, y1, z1, 16 - x2, 16 - y2, 16 - x1, 16 - y1);
		face(repère, tampon, sprite, couleur, lumière, 0, 0, 1,
				x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2, x1, 16 - y2, x2, 16 - y1);
		// Est et ouest.
		face(repère, tampon, sprite, couleur, lumière, 1, 0, 0,
				x2, y2, z2, x2, y2, z1, x2, y1, z1, x2, y1, z2, 16 - z2, 16 - y2, 16 - z1, 16 - y1);
		face(repère, tampon, sprite, couleur, lumière, -1, 0, 0,
				x1, y2, z1, x1, y2, z2, x1, y1, z2, x1, y1, z1, z1, 16 - y2, z2, 16 - y1);
		// Dessus et dessous.
		face(repère, tampon, sprite, couleur, lumière, 0, 1, 0,
				x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, x1, z1, x2, z2);
		face(repère, tampon, sprite, couleur, lumière, 0, -1, 0,
				x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, x1, 16 - z2, x2, 16 - z1);
	}

	/**
	 * Une face : quatre coins dans l'ordre haut-gauche, haut-droite, bas-droite,
	 * bas-gauche vus de dehors, et la portion de texture (en pixels, repliée sur
	 * zéro à seize) qui les couvre.
	 */
	private static void face(PoseStack.Pose repère, VertexConsumer tampon, TextureAtlasSprite sprite, int couleur,
	                         int lumière, float nx, float ny, float nz,
	                         float ax, float ay, float az, float bx, float by, float bz,
	                         float cx, float cy, float cz, float dx, float dy, float dz,
	                         float u1, float v1, float u2, float v2) {
		float uA = sprite.getU(replie(u1) / 16.0F);
		float uB = sprite.getU(replie(u2) / 16.0F);
		float vA = sprite.getV(replie(v1) / 16.0F);
		float vB = sprite.getV(replie(v2) / 16.0F);
		// Dans le sens inverse des aiguilles d'une montre, vu de dehors : le jeu
		// élimine les faces arrière, et une face tournée à l'envers ne se verrait
		// que de l'intérieur de la boîte.
		Matrix4f m = repère.pose();
		sommet(tampon, m, repère, ax, ay, az, uA, vA, couleur, lumière, nx, ny, nz);
		sommet(tampon, m, repère, dx, dy, dz, uA, vB, couleur, lumière, nx, ny, nz);
		sommet(tampon, m, repère, cx, cy, cz, uB, vB, couleur, lumière, nx, ny, nz);
		sommet(tampon, m, repère, bx, by, bz, uB, vA, couleur, lumière, nx, ny, nz);
	}

	/** Ramène une coordonnée de texture dans [0, 16] : une boîte peut déborder de son bloc. */
	private static float replie(float v) {
		if (v >= 0.0F && v <= 16.0F) {
			return v;
		}
		float r = v % 16.0F;
		return r < 0 ? r + 16.0F : r;
	}

	private static void sommet(VertexConsumer tampon, Matrix4f m, PoseStack.Pose repère,
	                           float x, float y, float z, float u, float v, int couleur, int lumière,
	                           float nx, float ny, float nz) {
		tampon.addVertex(m, x * PX, y * PX, z * PX)
				.setColor(couleur)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(lumière)
				.setNormal(repère, nx, ny, nz);
	}

	/**
	 * Une boîte dont toutes les faces prennent la même portion de texture,
	 * (u1, v1)-(u2, v2) en pixels : une plaque qui montre son dessin entier,
	 * où qu'elle soit posée.
	 */
	public static void boîteUV(PoseStack.Pose repère, VertexConsumer tampon, TextureAtlasSprite sprite,
	                           float x1, float y1, float z1, float x2, float y2, float z2,
	                           float u1, float v1, float u2, float v2, int lumière, int couleur) {
		face(repère, tampon, sprite, couleur, lumière, 0, 0, -1,
				x2, y2, z1, x1, y2, z1, x1, y1, z1, x2, y1, z1, u1, v1, u2, v2);
		face(repère, tampon, sprite, couleur, lumière, 0, 0, 1,
				x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2, u1, v1, u2, v2);
		face(repère, tampon, sprite, couleur, lumière, 1, 0, 0,
				x2, y2, z2, x2, y2, z1, x2, y1, z1, x2, y1, z2, u1, v1, u2, v2);
		face(repère, tampon, sprite, couleur, lumière, -1, 0, 0,
				x1, y2, z1, x1, y2, z2, x1, y1, z2, x1, y1, z1, u1, v1, u2, v2);
		face(repère, tampon, sprite, couleur, lumière, 0, 1, 0,
				x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, u1, v1, u2, v2);
		face(repère, tampon, sprite, couleur, lumière, 0, -1, 0,
				x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, u1, v1, u2, v2);
	}

	/**
	 * Le dessous d'une boîte seul, la normale tournée vers le haut : une face
	 * qui regarde le sol sans que le jeu l'assombrisse, comme un plafonnier
	 * qui éclaire au lieu d'être éclairé.
	 */
	public static void dessousÉclairé(PoseStack.Pose repère, VertexConsumer tampon, TextureAtlasSprite sprite,
	                                  float x1, float z1, float x2, float z2, float y, int lumière, int couleur) {
		face(repère, tampon, sprite, couleur, lumière, 0, 1, 0,
				x1, y, z2, x2, y, z2, x2, y, z1, x1, y, z1, x1, 16 - z2, x2, 16 - z1);
	}

	/**
	 * Une boîte carrelée : découpée en carreaux de seize pixels comptés depuis
	 * (ox, oy), chacun de la texture entière. Elle se répète sans s'étirer sur
	 * une grande surface, et suit son origine quand celle-ci glisse — un vantail
	 * qui s'ouvre emporte son dessin.
	 */
	public static void carrelage(PoseStack.Pose repère, VertexConsumer tampon, TextureAtlasSprite sprite,
	                             float x1, float y1, float z1, float x2, float y2, float z2,
	                             float ox, float oy, int lumière, int couleur) {
		for (float xa = x1; xa < x2 - 1.0E-4F; ) {
			float bordX = ox + 16.0F * ((float) Math.floor((xa - ox) / 16.0F + 1.0E-4F) + 1.0F);
			float xb = Math.min(x2, bordX);
			float ua = xa - (bordX - 16.0F);
			float ub = xb - (bordX - 16.0F);
			for (float ya = y1; ya < y2 - 1.0E-4F; ) {
				float bordY = oy + 16.0F * ((float) Math.floor((ya - oy) / 16.0F + 1.0E-4F) + 1.0F);
				float yb = Math.min(y2, bordY);
				float va = 16.0F - (yb - (bordY - 16.0F));
				float vb = 16.0F - (ya - (bordY - 16.0F));
				boîteUV(repère, tampon, sprite, xa, ya, z1, xb, yb, z2, ua, va, ub, vb, lumière, couleur);
				ya = yb;
			}
			xa = xb;
		}
	}

	/**
	 * Un aplat de couleur unie dans le plan z, pour une couche sans texture
	 * (position, couleur, lumière — celle des fonds de texte).
	 *
	 * <p>Il est émis dans les deux sens : ces couches éliminent les faces arrière,
	 * et l'on n'a pas à savoir de quel côté on le regarde.
	 */
	public static void aplatUni(PoseStack.Pose repère, VertexConsumer tampon,
	                            float x1, float y1, float x2, float y2, float z, int couleur, int lumière) {
		Matrix4f m = repère.pose();
		tampon.addVertex(m, x1, y2, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x1, y1, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x2, y1, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x2, y2, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x2, y2, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x2, y1, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x1, y1, z).setColor(couleur).setLight(lumière);
		tampon.addVertex(m, x1, y2, z).setColor(couleur).setLight(lumière);
	}

	/** Un trait de couleur unie, émis dans les deux sens comme {@link #aplatUni}. */
	public static void traitUni(PoseStack.Pose repère, VertexConsumer tampon,
	                            float ax, float ay, float bx, float by, float épaisseur, float z, int couleur, int lumière) {
		float dx = bx - ax;
		float dy = by - ay;
		float longueur = (float) Math.sqrt(dx * dx + dy * dy);
		if (longueur < 1.0E-6F) {
			return;
		}
		float px = -dy / longueur * épaisseur / 2;
		float py = dx / longueur * épaisseur / 2;
		Matrix4f m = repère.pose();
		float[][] coins = {{ax + px, ay + py}, {ax - px, ay - py}, {bx - px, by - py}, {bx + px, by + py}};
		for (float[] c : coins) {
			tampon.addVertex(m, c[0], c[1], z).setColor(couleur).setLight(lumière);
		}
		for (int i = coins.length - 1; i >= 0; i--) {
			tampon.addVertex(m, coins[i][0], coins[i][1], z).setColor(couleur).setLight(lumière);
		}
	}
}
