package dev.pyrofetch.jn1.client.mesh;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import com.geckolib.cache.model.GeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

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
 *
 * <p><strong>26.3.</strong> Le rendu parcourt lui-même les os posés et les
 * retient ({@link #retenir}) au moment de soumettre son dessin ; les sommets
 * sont alors déformés tout de suite, dans le repère de la caméra, et le dessin
 * soumis au jeu ({@code submitCustomGeometry}) ne fait plus que les écrire.
 */
public final class DessinMaillage {
	/**
	 * La couche des lueurs : celle des yeux d'araignée de 1.21.1 — sans ombrage
	 * ni obscurité, sans écrire la profondeur, et <em>additive</em> (un, un).
	 * En 26.3, la couche des yeux du jeu ({@code RenderTypes.eyes}) mélange en
	 * translucide : des halos sombres et opaques y recouvraient la pupille
	 * blanche, qui restait un point gris. Même pipeline que la sienne, au
	 * mélange près.
	 */
	private static final RenderPipeline LUEUR = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
					.withLocation(dev.pyrofetch.jn1.JN1.id("pipeline/lueur_additive"))
					.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
					.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
					.build());
	private static final java.util.function.Function<Identifier, RenderType> COUCHE_LUEUR = net.minecraft.util.Util.memoize(
			texture -> RenderType.create("jn1_lueur",
					RenderSetup.builder(LUEUR).withTexture("Sampler0", texture).createRenderSetup()));

	/** Par os : la pile telle que GeckoLib l'a préparée pour ses cubes, à cette image. */
	private final Map<String, PoseStack.Pose> poses = new HashMap<>();
	/** Par os : l'inverse de sa chaîne de repos, et l'os dont on l'a tirée. */
	private final Map<String, Matrix4f> reposInverse = new HashMap<>();
	private final Map<String, GeoBone> source = new HashMap<>();

	public void oublier() {
		this.poses.clear();
	}

	/** À appeler pour chaque os, avec la pile préparée comme pour dessiner ses cubes ({@code prepMatrixForBone}). */
	public void retenir(GeoBone os, PoseStack pile) {
		this.poses.put(os.name(), pile.last().copy());
		// Un rechargement des ressources refait les os : on recalcule alors.
		if (this.source.get(os.name()) != os) {
			this.source.put(os.name(), os);
			this.reposInverse.put(os.name(), chaîneDeRepos(os));
		}
	}

	/** La chaîne de repos d'un os, de la racine jusqu'à lui, inversée. */
	private static Matrix4f chaîneDeRepos(GeoBone os) {
		ArrayDeque<GeoBone> lignée = new ArrayDeque<>();
		for (GeoBone b = os; b != null; b = b.parent()) {
			lignée.push(b);
		}
		Matrix4f m = new Matrix4f();
		for (GeoBone b : lignée) {
			// La rotation de repos : celle du fichier, que GeckoLib 5 garde à part.
			m.translate(b.pivotX() / 16.0F, b.pivotY() / 16.0F, b.pivotZ() / 16.0F);
			if (b.baseRotZ() != 0) {
				m.rotate(Axis.ZP.rotation(b.baseRotZ()));
			}
			if (b.baseRotY() != 0) {
				m.rotate(Axis.YP.rotation(b.baseRotY()));
			}
			if (b.baseRotX() != 0) {
				m.rotate(Axis.XP.rotation(b.baseRotX()));
			}
			m.translate(-b.pivotX() / 16.0F, -b.pivotY() / 16.0F, -b.pivotZ() / 16.0F);
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
			PoseStack.Pose pose = this.poses.get(maillage.os[i]);
			Matrix4f repos = this.reposInverse.get(maillage.os[i]);
			if (pose != null && repos != null) {
				positions[i] = new Matrix4f(pose.pose()).mul(repos);
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
		PoseStack.Pose pose = this.poses.get(nom);
		Matrix4f repos = this.reposInverse.get(nom);
		GeoBone os = this.source.get(nom);
		if (pose == null || repos == null || os == null) {
			return null;
		}
		return new Matrix4f(pose.pose()).mul(repos).transformPosition(
				new org.joml.Vector3f(os.pivotX() / 16.0F, os.pivotY() / 16.0F, os.pivotZ() / 16.0F));
	}

	/** Et pour les normales : le repos n'est fait que de rotations et de translations, sa partie 3 × 3 les tourne telle quelle. */
	private Matrix3f[] normales(Maillage maillage, @Nullable Matrix4f après) {
		Matrix3f[] normales = new Matrix3f[maillage.os.length];
		for (int i = 0; i < normales.length; i++) {
			PoseStack.Pose pose = this.poses.get(maillage.os[i]);
			Matrix4f repos = this.reposInverse.get(maillage.os[i]);
			if (pose != null && repos != null) {
				normales[i] = new Matrix3f(pose.normal()).mul(new Matrix3f(repos));
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
	public void dessiner(Maillage maillage, OrderedSubmitNodeCollector tâches, PoseStack pile, int lumière,
	                     int superposition, int couleur, @Nullable Predicate<Maillage.Partie> cacher) {
		this.dessiner(maillage, tâches, pile, lumière, superposition, couleur, cacher, null);
	}

	/** Pareil, le tout encore déplacé par {@code après} (rotations et translations seulement). */
	public void dessiner(Maillage maillage, OrderedSubmitNodeCollector tâches, PoseStack pile, int lumière,
	                     int superposition, int couleur, @Nullable Predicate<Maillage.Partie> cacher,
	                     @Nullable Matrix4f après) {
		this.dessiner(maillage, tâches, pile, lumière, superposition, couleur, cacher, après, null);
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

	/**
	 * Pareil, chaque partie d'abord retouchée au repos par {@code retouche}.
	 * Une partie, un dessin soumis : chacune a sa planche. {@code pile} est la
	 * pile du rendu, dont la pose est remise au dessin (il ne s'en sert pas :
	 * les sommets sont déjà dans le repère de la caméra).
	 */
	public void dessiner(Maillage maillage, OrderedSubmitNodeCollector tâches, PoseStack pile, int lumière,
	                     int superposition, int couleur, @Nullable Predicate<Maillage.Partie> cacher,
	                     @Nullable Matrix4f après, @Nullable Retouche retouche) {
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

			int éclairage = partie.lumineuse() ? LightCoordsUtil.FULL_BRIGHT : lumière;
			int[] t = partie.triangles();
			float[] uv = partie.uv();
			// Faces arrière éliminées : l'ancien entityCutout (entityCutoutCull en 26.3).
			tâches.submitCustomGeometry(pile, RenderTypes.entityCutoutCull(partie.texture()), (pose, tampon) -> {
				// Le jeu dessine les entités par quadrilatères : un triangle en est un
				// dont le dernier sommet est doublé.
				for (int i = 0; i < t.length; i += 3) {
					for (int k = 0; k < 4; k++) {
						int s = t[i + Math.min(k, 2)];
						tampon.addVertex(pos[s * 3], pos[s * 3 + 1], pos[s * 3 + 2], couleur, uv[s * 2], uv[s * 2 + 1],
								superposition, éclairage, nor[s * 3], nor[s * 3 + 1], nor[s * 3 + 2]);
					}
				}
			});
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
	public void dessinerLueur(Maillage maillage, OrderedSubmitNodeCollector tâches, PoseStack pile,
	                          Predicate<Maillage.Partie> quelles, Identifier texture, float échelle, int couleur,
	                          @Nullable Matrix4f après) {
		Matrix4f[] positions = this.positions(maillage, après);
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
			float mx = cx / sommets;
			float my = cy / sommets;
			float mz = cz / sommets;
			int[] t = partie.triangles();
			tâches.submitCustomGeometry(pile, COUCHE_LUEUR.apply(texture), (pose, tampon) -> {
				for (int i = 0; i < t.length; i += 3) {
					for (int k = 0; k < 4; k++) {
						int s = t[i + Math.min(k, 2)];
						tampon.addVertex(mx + (pos[s * 3] - mx) * échelle, my + (pos[s * 3 + 1] - my) * échelle,
								mz + (pos[s * 3 + 2] - mz) * échelle, couleur, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY,
								LightCoordsUtil.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
					}
				}
			});
		}
	}
}
