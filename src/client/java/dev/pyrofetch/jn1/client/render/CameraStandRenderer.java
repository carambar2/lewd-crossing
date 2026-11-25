package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.CameraStandBlockEntity;
import dev.pyrofetch.jn1.client.camera.CameraSession;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * L'appareil vissé sur le trépied, tourné là où il cadre : le lacet et le
 * tangage réglés au viseur, que le bloc garde. Le pied, lui, est le modèle du
 * bloc.
 *
 * <p>Le modèle (`gen_appareil.py`) a l'objectif au nord, centré sur l'œil de
 * {@link CameraSession} : tourné autour de ce point, l'objectif reste où la vue
 * se place. Il n'est pas dessiné quand on regarde par lui — on serait dedans.
 */
public class CameraStandRenderer implements BlockEntityRenderer<CameraStandBlockEntity> {
	public static final Identifier TÊTE = JN1.id("block/camera_stand_head");
	/** L'œil de `CameraSession`, en blocs. */
	private static final double PIVOT = 0.62;

	public CameraStandRenderer(Context contexte) {
	}

	@Override
	public void render(CameraStandBlockEntity pied, float tickDelta, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière, int recouvrement) {
		if (CameraSession.vueDéportée() && pied.getPos().equals(CameraSession.pied())) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		BakedModel modèle = client.getBakedModelManager().getModel(TÊTE);
		matrices.push();
		matrices.translate(0.5, PIVOT, 0.5);
		// Objectif au nord, soit un lacet de 180° ; un tangage positif le baisse.
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - pied.lacet()));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-pied.tangage()));
		matrices.translate(-0.5, -PIVOT, -0.5);
		client.getBlockRenderManager().getModelRenderer().render(matrices.peek(),
				tampons.getBuffer(TexturedRenderLayers.getEntityCutout()), null, modèle,
				1.0F, 1.0F, 1.0F, lumière, recouvrement);
		matrices.pop();
	}
}
