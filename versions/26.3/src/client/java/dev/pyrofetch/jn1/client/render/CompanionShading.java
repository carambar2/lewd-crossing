package dev.pyrofetch.jn1.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * L'éclairage propre aux modèles des compagnes, partagé par leur rendu
 * ({@link CompanionRenderer}) et par celui du joueur transformé
 * ({@link dev.pyrofetch.jn1.client.morph.MorphRenderer}) : lissage des
 * normales de la poitrine et de la croupe, vraie normale des cubes plats.
 *
 * <p>Un objet par rendu : il retient le centre du sous-arbre en cours. Les
 * dessins d'un même rendu se suivent sur le fil du rendu (GeckoLib 5 les
 * diffère, mais ne les entrelace pas) : un seul centre à la fois.
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Les os et les cubes cuits se
 * dessinent eux-mêmes ({@code CuboidGeoBone.render}, {@code GeoCube.render}) ;
 * plus de crochet par cube ni par facette. Le rendu des compagnes parcourt
 * donc les os lui-même ({@link CompanionRenderer}) et dessine chaque cube
 * par {@link #dessinerCube}, qui refait les trois pas de GeckoLib en y mettant
 * nos normales.
 */
public final class CompanionShading {
	/**
	 * Sous-arbre dont l'éclairage est aplani.
	 *
	 * <p>Minecraft éclaire une entité en diffus : la clarté d'une face vient de
	 * l'orientation de sa normale face à deux sources fixes. La poitrine est
	 * sculptée avec des centaines de facettes dont les os pointent dans toutes
	 * les directions — mesuré : jusqu'à 176° d'écart, soit de 40 % à 100 % de
	 * clarté d'une facette à sa voisine. D'où une mosaïque d'ombres figée sur le
	 * galbe, qui contredit le dégradé peint dont ce galbe est censé venir.
	 *
	 * <p>Ce n'est ni un scintillement ni un conflit de profondeur : les taches ne
	 * bougent pas d'elles-mêmes, elles suivent l'observateur parce que le corps
	 * tourne devant deux sources qui, elles, ne bougent pas.
	 */
	private static final Set<String> SMOOTHED_ROOTS = Set.of("boobs", "booty");

	/** Le centre de la forme, en repère de vue, d'où partent les normales. */
	@Nullable
	private Vector3f smoothCentre;

	/** De quoi accumuler sans allouer un vecteur par sommet. */
	private final Vector3f scratch = new Vector3f();

	/**
	 * À appeler en entrant dans un os, avant de préparer sa matrice. Rend vrai
	 * s'il ouvre un sous-arbre lissé : il faudra alors le dire à {@link #leaveBone}.
	 */
	public boolean enterBone(PoseStack poseStack, GeoBone bone) {
		boolean root = this.smoothCentre == null && SMOOTHED_ROOTS.contains(bone.name());
		if (root) {
			this.smoothCentre = subtreeCentre(poseStack, bone, this.scratch);
		}
		return root;
	}

	public void leaveBone(boolean root) {
		if (root) {
			this.smoothCentre = null;
		}
	}

	/** Les cubes d'un os, s'il en porte. */
	public static GeoCube[] cubes(GeoBone bone) {
		return bone instanceof CuboidGeoBone cuboïde ? cuboïde.cubes : new GeoCube[0];
	}

	/**
	 * Le barycentre d'un sous-arbre entier, en repère de vue.
	 *
	 * <p>C'est bien le sous-arbre qu'il faut prendre, et non l'os porteur : le
	 * même calcul limité aux cubes de chaque os ne divise le contraste entre
	 * facettes voisines que par 1,5, et l'aggrave même sur la croupe de Jenny,
	 * parce qu'un os n'y couvre qu'un morceau de la forme. Sur le sous-arbre, le
	 * contraste tombe de 0,108 à 0,038 — mesuré par {@code render_model.py}.
	 *
	 * <p>Le pivot de l'os ne convient pas non plus : il est au-dessus de la
	 * géométrie, ce qui fait pointer presque toutes les normales vers le bas et
	 * assombrit l'ensemble.
	 */
	@Nullable
	private static Vector3f subtreeCentre(PoseStack poseStack, GeoBone bone, Vector3f scratch) {
		Vector3f somme = new Vector3f();
		int[] compte = {0};
		poseStack.pushPose();
		RenderUtil.prepMatrixForBone(poseStack, bone);
		accumulate(poseStack, bone, somme, compte, scratch);
		poseStack.popPose();
		return compte[0] == 0 ? null : somme.div(compte[0]);
	}

	private static void accumulate(PoseStack poseStack, GeoBone bone, Vector3f somme,
	                               int[] compte, Vector3f scratch) {
		for (GeoCube cube : cubes(bone)) {
			poseStack.pushPose();
			// Les mêmes trois pas que le dessin du cube, sans quoi un cube tourné
			// compterait à côté de l'endroit où il sera dessiné.
			cube.translateToPivotPoint(poseStack);
			cube.rotate(poseStack);
			cube.translateAwayFromPivotPoint(poseStack);
			Matrix4f matrice = poseStack.last().pose();
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) {
					continue;
				}
				for (GeoVertex sommet : quad.vertices()) {
					scratch.set(sommet.posX(), sommet.posY(), sommet.posZ());
					matrice.transformPosition(scratch);
					somme.add(scratch);
					compte[0]++;
				}
			}
			poseStack.popPose();
		}
		for (GeoBone fils : bone.children()) {
			poseStack.pushPose();
			RenderUtil.prepMatrixForBone(poseStack, fils);
			accumulate(poseStack, fils, somme, compte, scratch);
			poseStack.popPose();
		}
	}

	/**
	 * Un os et sa descendance, comme {@code renderRecursively} de GeckoLib 4 :
	 * sa matrice, ses cubes s'il est visible (avec nos normales), puis ses
	 * enfants s'il ne les cache pas. À appeler pendant que les os sont posés
	 * ({@code RenderPassInfo.renderPosed}).
	 */
	public void dessinerOs(@Nullable com.geckolib.renderer.base.RenderPassInfo<?> info, PoseStack poseStack,
	                       GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
		boolean root = this.enterBone(poseStack, bone);
		poseStack.pushPose();
		RenderUtil.prepMatrixForBoneAndUpdateListeners(poseStack, bone, info);
		com.geckolib.animation.state.BoneSnapshot instantané = bone.frameSnapshot;
		if (instantané == null || !instantané.isHidden()) {
			for (GeoCube cube : cubes(bone)) {
				this.dessinerCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
			}
		}
		if (instantané == null || !instantané.areChildrenHidden()) {
			for (GeoBone fils : bone.children()) {
				this.dessinerOs(info, poseStack, fils, buffer, packedLight, packedOverlay, colour);
			}
		}
		poseStack.popPose();
		this.leaveBone(root);
	}

	/**
	 * Dessine un cube comme GeckoLib ({@code GeoCube.render}), avec nos normales :
	 * celle des cubes plats ({@link #flatNormal}), puis le lissage du galbe
	 * ({@link #normal}).
	 */
	public void dessinerCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer,
	                         int packedLight, int packedOverlay, int colour) {
		poseStack.pushPose();
		cube.translateToPivotPoint(poseStack);
		cube.rotate(poseStack);
		cube.translateAwayFromPivotPoint(poseStack);
		Matrix3f normales = poseStack.last().normal();
		Matrix4f poseState = new Matrix4f(poseStack.last().pose());
		boolean plat = cube.size().x == 0 || cube.size().y == 0 || cube.size().z == 0;
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) {
				continue;
			}
			Vector3f normal = normales.transform(quad.normalVec());
			if (plat) {
				flatNormal(quad, poseState, normal);
			}
			// Un cube plein : RenderUtil.fixInvertedFlatCube serait sans effet.
			quad.render(poseState, this.normal(quad, poseState, normal), buffer, packedLight, packedOverlay, colour);
		}
		poseStack.popPose();
	}

	/**
	 * La normale d'une face de cube plat, autrement que chez GeckoLib.
	 *
	 * <p>GeckoLib passe leur normale par {@code RenderUtil.fixInvertedFlatCube},
	 * qui rend positives ses composantes x et y <em>dans le repère de la vue</em>.
	 * Un plan collé à une face pleine prend alors une normale qui n'est plus la
	 * sienne, et s'éclaire autrement qu'elle : c'est le rectangle gris qui
	 * couvrait le visage de Jenny dès que le rougissement ({@code blush}, plan
	 * de peau peint, avancé devant le visage par les scènes) paraissait — plus
	 * ou moins sombre selon l'angle de vue et l'inclinaison de la tête.
	 *
	 * <p>On garde la vraie normale, retournée vers l'œil si elle lui tourne le
	 * dos : un plan se voit toujours par la face qu'il présente, et ses deux
	 * faces confondues s'éclairent de même. L'œil est à l'origine de ce repère,
	 * les matrices de rendu des entités étant relatives à la caméra.
	 */
	private static void flatNormal(GeoQuad quad, Matrix4f poseState, Vector3f normal) {
		Vector3f milieu = new Vector3f();
		for (GeoVertex sommet : quad.vertices()) {
			milieu.add(poseState.transformPosition(new Vector3f(sommet.posX(), sommet.posY(), sommet.posZ())));
		}
		if (normal.dot(milieu) > 0) {
			normal.negate();
		}
	}

	/**
	 * La normale d'une facette de galbe est refaite : on la fait partir du centre
	 * de la forme vers la facette, au lieu de la prendre sur l'os qui la porte.
	 *
	 * <p>Prise sur l'os, elle suit une orientation de sculpture qui saute d'une
	 * facette à sa voisine — d'où la mosaïque. Prise sur le centre, elle varie
	 * continûment avec la position : le dessus s'éclaire, le dessous s'ombre, et
	 * le relief revient sans que deux facettes mitoyennes se contredisent.
	 */
	public Vector3f normal(GeoQuad quad, Matrix4f poseState, Vector3f normal) {
		Vector3f centre = this.smoothCentre;
		if (centre == null) {
			return normal;
		}
		Vector3f milieu = new Vector3f();
		for (GeoVertex sommet : quad.vertices()) {
			milieu.add(poseState.transformPosition(new Vector3f(sommet.posX(), sommet.posY(), sommet.posZ())));
		}
		milieu.div(quad.vertices().length);
		Vector3f radiale = milieu.sub(centre, new Vector3f());
		// Une facette pile sur le centre ne dit rien : on garde la sienne.
		return radiale.lengthSquared() > 1.0E-6F ? radiale.normalize() : normal;
	}
}
