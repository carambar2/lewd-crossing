package dev.pyrofetch.jn1.client.render;

import java.util.Set;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * L'éclairage propre aux modèles des compagnes, partagé par leur rendu
 * ({@link CompanionRenderer}) et par celui du joueur transformé
 * ({@link dev.pyrofetch.jn1.client.morph.MorphRenderer}) : lissage des
 * normales de la poitrine et de la croupe, vraie normale des cubes plats.
 *
 * <p>Un objet par rendu : il retient le centre du sous-arbre en cours.
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
	 * À appeler en entrant dans un os, avant de le dessiner. Rend vrai s'il
	 * ouvre un sous-arbre lissé : il faudra alors le dire à {@link #leaveBone}.
	 */
	public boolean enterBone(MatrixStack poseStack, GeoBone bone) {
		boolean root = this.smoothCentre == null && SMOOTHED_ROOTS.contains(bone.getName());
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
	private static Vector3f subtreeCentre(MatrixStack poseStack, GeoBone bone, Vector3f scratch) {
		Vector3f somme = new Vector3f();
		int[] compte = {0};
		poseStack.push();
		RenderUtil.prepMatrixForBone(poseStack, bone);
		accumulate(poseStack, bone, somme, compte, scratch);
		poseStack.pop();
		return compte[0] == 0 ? null : somme.div(compte[0]);
	}

	private static void accumulate(MatrixStack poseStack, GeoBone bone, Vector3f somme,
	                               int[] compte, Vector3f scratch) {
		for (GeoCube cube : bone.getCubes()) {
			poseStack.push();
			// Les mêmes trois pas que renderCube, sans quoi un cube tourné
			// compterait à côté de l'endroit où il sera dessiné.
			RenderUtil.translateToPivotPoint(poseStack, cube);
			RenderUtil.rotateMatrixAroundCube(poseStack, cube);
			RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
			Matrix4f matrice = poseStack.peek().getPositionMatrix();
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) {
					continue;
				}
				for (GeoVertex sommet : quad.vertices()) {
					scratch.set(sommet.position());
					matrice.transformPosition(scratch);
					somme.add(scratch);
					compte[0]++;
				}
			}
			poseStack.pop();
		}
		for (GeoBone fils : bone.getChildBones()) {
			poseStack.push();
			RenderUtil.prepMatrixForBone(poseStack, fils);
			accumulate(poseStack, fils, somme, compte, scratch);
			poseStack.pop();
		}
	}

	/**
	 * Comme celui de GeckoLib, sauf pour les cubes plats.
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
	public static <T extends GeoAnimatable> boolean renderFlatCube(GeoRenderer<T> renderer, MatrixStack poseStack,
	                                                               GeoCube cube, VertexConsumer buffer,
	                                                               int packedLight, int packedOverlay, int colour) {
		boolean plat = cube.size().x == 0 || cube.size().y == 0 || cube.size().z == 0;
		if (!plat) {
			return false;
		}
		RenderUtil.translateToPivotPoint(poseStack, cube);
		RenderUtil.rotateMatrixAroundCube(poseStack, cube);
		RenderUtil.translateAwayFromPivotPoint(poseStack, cube);

		org.joml.Matrix3f normales = poseStack.peek().getNormalMatrix();
		Matrix4f poseState = new Matrix4f(poseStack.peek().getPositionMatrix());
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) {
				continue;
			}
			Vector3f normal = normales.transform(new Vector3f(quad.normal()));
			Vector3f milieu = new Vector3f();
			for (GeoVertex sommet : quad.vertices()) {
				milieu.add(poseState.transformPosition(new Vector3f(sommet.position())));
			}
			if (normal.dot(milieu) > 0) {
				normal.negate();
			}
			renderer.createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, colour);
		}
		return true;
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
			milieu.add(poseState.transformPosition(new Vector3f(sommet.position())));
		}
		milieu.div(quad.vertices().length);
		Vector3f radiale = milieu.sub(centre, new Vector3f());
		// Une facette pile sur le centre ne dit rien : on garde la sienne.
		return radiale.lengthSquared() > 1.0E-6F ? radiale.normalize() : normal;
	}
}
