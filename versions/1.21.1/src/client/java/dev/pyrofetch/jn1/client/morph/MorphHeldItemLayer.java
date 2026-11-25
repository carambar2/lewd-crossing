package dev.pyrofetch.jn1.client.morph;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Les objets que le joueur transformé tient, dans les mains de sa compagne.
 *
 * <p>Les modèles d'origine ont bien des os pour cela ({@code weapon},
 * {@code offhand}), mais pas chez toutes, et leur orientation se compose avec
 * le pli de repos des avant-bras — -140° chez Jenny, rien chez Ellie : un même
 * objet y serait tenu de deux façons. On part donc de l'avant-bras tel qu'il
 * est <em>dessiné</em> : le repère de l'os, puis celui de son cube, dont la
 * rotation compense justement ce pli. Dans ce repère, l'avant-bras pend le long
 * de l'axe y et la main est au bas du cube.
 *
 * <p>De là, on refait ce que fait le jeu pour un joueur
 * ({@code HeldItemFeatureRenderer}) : le jeu tient l'objet au bas du bras, au
 * milieu de sa largeur, deux pixels en avant, puis le tourne de -90° sur x et
 * de 180° sur y. Ses modèles d'entité ont y vers le bas et x inversé, ceux de
 * GeckoLib non : un demi-tour sur z fait passer des uns aux autres. L'objet est
 * ensuite dessiné par le jeu lui-même, dans les modes de la troisième personne,
 * ce qui règle bouclier, arbalète et bloc comme sur un joueur.
 *
 * <p>Pas d'armure : c'est voulu.
 */
public class MorphHeldItemLayer extends GeoRenderLayer<MorphAnimatable> {
	private static final String AVANT_BRAS_DROIT = "lowerArmR";
	private static final String AVANT_BRAS_GAUCHE = "lowerArmL";
	/** Deux pixels en avant de la main, comme le jeu. */
	private static final float EN_AVANT = -0.125F;

	private final MorphRenderer rendu;

	public MorphHeldItemLayer(MorphRenderer rendu) {
		super(rendu);
		this.rendu = rendu;
	}

	@Override
	public void renderForBone(MatrixStack poseStack, MorphAnimatable animatable, GeoBone bone, RenderLayer renderType,
	                          VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick,
	                          int packedLight, int packedOverlay) {
		boolean droite = bone.getName().equals(AVANT_BRAS_DROIT);
		if (!droite && !bone.getName().equals(AVANT_BRAS_GAUCHE)) {
			return;
		}
		AbstractClientPlayerEntity joueur = this.rendu.getCurrentEntity();
		if (joueur == null) {
			return;
		}
		Arm côté = droite ? Arm.RIGHT : Arm.LEFT;
		ItemStack objet = joueur.getMainArm() == côté ? joueur.getMainHandStack() : joueur.getOffHandStack();
		GeoCube avantBras = plusGrandCube(bone);
		if (objet.isEmpty() || avantBras == null) {
			return;
		}

		poseStack.push();
		RenderUtil.translateToPivotPoint(poseStack, avantBras);
		RenderUtil.rotateMatrixAroundCube(poseStack, avantBras);
		RenderUtil.translateAwayFromPivotPoint(poseStack, avantBras);

		float[] boîte = bornes(avantBras);
		poseStack.translate((boîte[0] + boîte[3]) / 2, boîte[1], (boîte[2] + boîte[5]) / 2);
		poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		poseStack.translate(0.0F, 0.0F, EN_AVANT);
		poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
		poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

		MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer().renderItem(joueur, objet,
				droite ? ModelTransformationMode.THIRD_PERSON_RIGHT_HAND : ModelTransformationMode.THIRD_PERSON_LEFT_HAND,
				!droite, poseStack, bufferSource, packedLight);
		poseStack.pop();
	}

	/** L'avant-bras lui-même : chez Ellie, l'os porte aussi des manchettes. */
	@Nullable
	private static GeoCube plusGrandCube(GeoBone bone) {
		GeoCube meilleur = null;
		float volume = -1;
		for (GeoCube cube : bone.getCubes()) {
			float[] b = bornes(cube);
			float v = (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
			if (v > volume) {
				volume = v;
				meilleur = cube;
			}
		}
		return meilleur;
	}

	/** min x, y, z puis max x, y, z des sommets, en blocs, dans le repère du cube. */
	private static float[] bornes(GeoCube cube) {
		float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) {
				continue;
			}
			for (GeoVertex sommet : quad.vertices()) {
				b[0] = Math.min(b[0], sommet.position().x());
				b[1] = Math.min(b[1], sommet.position().y());
				b[2] = Math.min(b[2], sommet.position().z());
				b[3] = Math.max(b[3], sommet.position().x());
				b[4] = Math.max(b[4], sommet.position().y());
				b[5] = Math.max(b[5], sommet.position().z());
			}
		}
		return b;
	}
}
