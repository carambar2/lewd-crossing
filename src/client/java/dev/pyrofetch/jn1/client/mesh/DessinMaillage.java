package dev.pyrofetch.jn1.client.mesh;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Dessine un {@link Maillage} déformé par les os que GeckoLib vient de poser.
 *
 * <p>GeckoLib ne sait dessiner que des cubes. On le laisse donc parcourir ses
 * os comme d'habitude — animations, regard, échelle, tout ce qu'il fait — et
 * l'on retient, pour chacun, la matrice qu'il tient au moment de dessiner ses
 * cubes ({@link #retenir}) : elle mène d'un point du modèle **sans les
 * rotations de repos** à l'écran. Le maillage, lui, est au repos ; pour chaque
 * os, on compose donc cette matrice avec l'inverse de sa chaîne de repos,
 * refaite comme {@code RenderUtil.prepMatrixForBone} la fait. Chaque sommet
 * suit ses quatre os au plus, selon leurs poids : la déformation d'un
 * squelette de Blender.
 *
 * <p>Fait pour Gaster ; écrit pour servir à toute créature exportée ainsi.
 */
public final class DessinMaillage {
	/** Par os : la pile telle que GeckoLib l'a préparée pour ses cubes, à cette image. */
	private final Map<String, MatrixStack.Entry> poses = new HashMap<>();
	/** Par os : l'inverse de sa chaîne de repos, et l'os dont on l'a tirée. */
	private final Map<String, Matrix4f> reposInverse = new HashMap<>();
	private final Map<String, GeoBone> source = new HashMap<>();

	public void oublier() {
		this.poses.clear();
	}

	/** À appeler depuis {@code renderCubesOfBone}, avec la pile telle que GeckoLib l'a préparée. */
	public void retenir(GeoBone os, MatrixStack pile) {
		this.poses.put(os.getName(), pile.peek().copy());
		// Un rechargement des ressources refait les os : on recalcule alors.
		if (this.source.get(os.getName()) != os) {
			this.source.put(os.getName(), os);
			this.reposInverse.put(os.getName(), chaîneDeRepos(os));
		}
	}

	/** La chaîne de repos d'un os, de la racine jusqu'à lui, inversée. */
	private static Matrix4f chaîneDeRepos(GeoBone os) {
		ArrayDeque<GeoBone> lignée = new ArrayDeque<>();
		for (GeoBone b = os; b != null; b = b.getParent()) {
			lignée.push(b);
		}
		Matrix4f m = new Matrix4f();
		for (GeoBone b : lignée) {
			BoneSnapshot repos = b.getInitialSnapshot();
			m.translate(b.getPivotX() / 16.0F, b.getPivotY() / 16.0F, b.getPivotZ() / 16.0F);
			if (repos.getRotZ() != 0) {
				m.rotate(RotationAxis.POSITIVE_Z.rotation(repos.getRotZ()));
			}
			if (repos.getRotY() != 0) {
				m.rotate(RotationAxis.POSITIVE_Y.rotation(repos.getRotY()));
			}
			if (repos.getRotX() != 0) {
				m.rotate(RotationAxis.POSITIVE_X.rotation(repos.getRotX()));
			}
			m.translate(-b.getPivotX() / 16.0F, -b.getPivotY() / 16.0F, -b.getPivotZ() / 16.0F);
		}
		return m.invert();
	}

	/**
	 * Par os du maillage, la matrice qui mène du repos à l'écran à cette image
	 * (nulle : os non retenu) ; {@code après}, s'il est donné, déplace encore le
	 * tout.
	 */
	private Matrix4f[] positions(Maillage maillage, @Nullable Matrix4f après) {
		Matrix4f[] positions = new Matrix4f[maillage.os.length];
		for (int i = 0; i < positions.length; i++) {
			MatrixStack.Entry pose = this.poses.get(maillage.os[i]);
			Matrix4f repos = this.reposInverse.get(maillage.os[i]);
			if (pose != null && repos != null) {
				positions[i] = new Matrix4f(pose.getPositionMatrix()).mul(repos);
				if (après != null) {
					positions[i] = new Matrix4f(après).mul(positions[i]);
				}
			}
		}
		return positions;
	}

	/** Où est le pivot de cet os à cette image, dans le repère du rendu ; rien s'il n'a pas été retenu. */
	@Nullable
	public org.joml.Vector3f pivot(String nom) {
		MatrixStack.Entry pose = this.poses.get(nom);
		Matrix4f repos = this.reposInverse.get(nom);
		GeoBone os = this.source.get(nom);
		if (pose == null || repos == null || os == null) {
			return null;
		}
		return new Matrix4f(pose.getPositionMatrix()).mul(repos).transformPosition(
				new org.joml.Vector3f(os.getPivotX() / 16.0F, os.getPivotY() / 16.0F, os.getPivotZ() / 16.0F));
	}

	/** Et pour les normales : le repos n'est fait que de rotations et de translations, sa partie 3 × 3 les tourne telle quelle. */
	private Matrix3f[] normales(Maillage maillage, @Nullable Matrix4f après) {
		Matrix3f[] normales = new Matrix3f[maillage.os.length];
		for (int i = 0; i < normales.length; i++) {
			MatrixStack.Entry pose = this.poses.get(maillage.os[i]);
			Matrix4f repos = this.reposInverse.get(maillage.os[i]);
			if (pose != null && repos != null) {
				normales[i] = new Matrix3f(pose.getNormalMatrix()).mul(new Matrix3f(repos));
				if (après != null) {
					normales[i] = new Matrix3f(après).mul(normales[i]);
				}
			}
		}
		return normales;
	}

	/**
	 * Les sommets d'une partie, déformés : chacun suit ses quatre os au plus.
	 * Normales déformées dans {@code nor} s'il est donné.
	 */
	private static float[] déformer(Maillage.Partie partie, Matrix4f[] positions, @Nullable Matrix3f[] normales,
	                                @Nullable float[] nor) {
		return déformer(partie, positions, normales, nor, partie.positions());
	}

	private static float[] déformer(Maillage.Partie partie, Matrix4f[] positions, @Nullable Matrix3f[] normales,
	                                @Nullable float[] nor, float[] source) {
		int sommets = partie.sommets();
		float[] sourceN = partie.normales();
		float[] pos = new float[sommets * 3];
		for (int s = 0; s < sommets; s++) {
			float x = 0, y = 0, z = 0, nx = 0, ny = 0, nz = 0;
			float px = source[s * 3], py = source[s * 3 + 1], pz = source[s * 3 + 2];
			float qx = sourceN[s * 3], qy = sourceN[s * 3 + 1], qz = sourceN[s * 3 + 2];
			for (int k = 0; k < 4; k++) {
				int o = partie.osIndex()[s * 4 + k];
				float w = partie.poids()[s * 4 + k];
				if (o < 0 || w <= 0 || positions[o] == null) {
					continue;
				}
				Matrix4f m = positions[o];
				x += w * (m.m00() * px + m.m10() * py + m.m20() * pz + m.m30());
				y += w * (m.m01() * px + m.m11() * py + m.m21() * pz + m.m31());
				z += w * (m.m02() * px + m.m12() * py + m.m22() * pz + m.m32());
				if (nor != null && normales != null && normales[o] != null) {
					Matrix3f r = normales[o];
					nx += w * (r.m00() * qx + r.m10() * qy + r.m20() * qz);
					ny += w * (r.m01() * qx + r.m11() * qy + r.m21() * qz);
					nz += w * (r.m02() * qx + r.m12() * qy + r.m22() * qz);
				}
			}
			pos[s * 3] = x;
			pos[s * 3 + 1] = y;
			pos[s * 3 + 2] = z;
			if (nor != null) {
				float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (l > 1e-6F) {
					nx /= l;
					ny /= l;
					nz /= l;
				}
				nor[s * 3] = nx;
				nor[s * 3 + 1] = ny;
				nor[s * 3 + 2] = nz;
			}
		}
		return pos;
	}

	/**
	 * Dessine le maillage avec les os retenus à cette image.
	 *
	 * @param cacher les parties à ne pas dessiner, ou rien
	 */
	public void dessiner(Maillage maillage, VertexConsumerProvider tampons, int lumière, int superposition,
	                     int couleur, @Nullable Predicate<Maillage.Partie> cacher) {
		this.dessiner(maillage, tampons, lumière, superposition, couleur, cacher, null);
	}

	/** Pareil, le tout encore déplacé par {@code après} (rotations et translations seulement). */
	public void dessiner(Maillage maillage, VertexConsumerProvider tampons, int lumière, int superposition,
	                     int couleur, @Nullable Predicate<Maillage.Partie> cacher, @Nullable Matrix4f après) {
		this.dessiner(maillage, tampons, lumière, superposition, couleur, cacher, après, null);
	}

	/**
	 * Une retouche du maillage au repos avant sa déformation par les os : elle
	 * reçoit la partie et une copie de ses positions (repère de GeckoLib), à
	 * modifier sur place — les expressions du visage, le regard.
	 */
	public interface Retouche {
		void retoucher(Maillage.Partie partie, float[] positions);
	}

	/** Ajoute à {@code positions} les formes de la partie, dosées par {@code poids} (0 : rien). */
	public static void appliquerFormes(Maillage.Partie partie, float[] positions, java.util.Map<String, Float> poids) {
		for (java.util.Map.Entry<String, Maillage.Forme> e : partie.formes().entrySet()) {
			Float w = poids.get(e.getKey());
			if (w == null || w <= 0.001F) {
				continue;
			}
			int[] s = e.getValue().sommets();
			float[] d = e.getValue().déplacements();
			for (int k = 0; k < s.length; k++) {
				positions[s[k] * 3] += w * d[k * 3];
				positions[s[k] * 3 + 1] += w * d[k * 3 + 1];
				positions[s[k] * 3 + 2] += w * d[k * 3 + 2];
			}
		}
	}

	/** Pareil, chaque partie d'abord retouchée au repos par {@code retouche}. */
	public void dessiner(Maillage maillage, VertexConsumerProvider tampons, int lumière, int superposition,
	                     int couleur, @Nullable Predicate<Maillage.Partie> cacher, @Nullable Matrix4f après,
	                     @Nullable Retouche retouche) {
		Matrix4f[] positions = this.positions(maillage, après);
		Matrix3f[] normales = this.normales(maillage, après);
		for (Maillage.Partie partie : maillage.parties) {
			if (cacher != null && cacher.test(partie)) {
				continue;
			}
			float[] nor = new float[partie.sommets() * 3];
			float[] source = partie.positions();
			if (retouche != null) {
				source = source.clone();
				retouche.retoucher(partie, source);
			}
			float[] pos = déformer(partie, positions, normales, nor, source);

			VertexConsumer tampon = tampons.getBuffer(RenderLayer.getEntityCutout(partie.texture()));
			int éclairage = partie.lumineuse() ? LightmapTextureManager.MAX_LIGHT_COORDINATE : lumière;
			int[] t = partie.triangles();
			float[] uv = partie.uv();
			// Le jeu dessine les entités par quadrilatères : un triangle en est un
			// dont le dernier sommet est doublé.
			for (int i = 0; i < t.length; i += 3) {
				for (int k = 0; k < 4; k++) {
					int s = t[i + Math.min(k, 2)];
					tampon.vertex(pos[s * 3], pos[s * 3 + 1], pos[s * 3 + 2], couleur, uv[s * 2], uv[s * 2 + 1],
							superposition, éclairage, nor[s * 3], nor[s * 3 + 1], nor[s * 3 + 2]);
				}
			}
		}
	}

	/**
	 * Redessine des parties en lumière, comme les yeux d'une araignée : couche
	 * additive, sans ombrage ni obscurité, d'une seule couleur ; chaque partie
	 * agrandie de {@code échelle} autour de son centre (plus grande qu'elle, une
	 * lueur pâle fait un halo).
	 *
	 * @param texture une planche blanche : la couleur vient de {@code couleur}
	 */
	public void dessinerLueur(Maillage maillage, VertexConsumerProvider tampons, Predicate<Maillage.Partie> quelles,
	                          Identifier texture, float échelle, int couleur, @Nullable Matrix4f après) {
		Matrix4f[] positions = this.positions(maillage, après);
		VertexConsumer tampon = tampons.getBuffer(RenderLayer.getEyes(texture));
		for (Maillage.Partie partie : maillage.parties) {
			if (!quelles.test(partie)) {
				continue;
			}
			float[] pos = déformer(partie, positions, null, null);
			int sommets = partie.sommets();
			float cx = 0, cy = 0, cz = 0;
			for (int s = 0; s < sommets; s++) {
				cx += pos[s * 3];
				cy += pos[s * 3 + 1];
				cz += pos[s * 3 + 2];
			}
			cx /= sommets;
			cy /= sommets;
			cz /= sommets;
			int[] t = partie.triangles();
			for (int i = 0; i < t.length; i += 3) {
				for (int k = 0; k < 4; k++) {
					int s = t[i + Math.min(k, 2)];
					tampon.vertex(cx + (pos[s * 3] - cx) * échelle, cy + (pos[s * 3 + 1] - cy) * échelle,
							cz + (pos[s * 3 + 2] - cz) * échelle, couleur, 0.5F, 0.5F, OverlayTexture.DEFAULT_UV,
							LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0F, 1.0F, 0.0F);
				}
			}
		}
	}
}
