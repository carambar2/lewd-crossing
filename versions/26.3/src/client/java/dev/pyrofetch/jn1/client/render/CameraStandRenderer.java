package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.CameraStandBlockEntity;
import dev.pyrofetch.jn1.client.camera.CameraSession;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * L'appareil vissé sur le trépied, tourné là où il cadre : le lacet et le
 * tangage réglés au viseur, que le bloc garde. Le pied, lui, est le modèle du
 * bloc.
 *
 * <p>Le modèle (`gen_appareil.py`) a l'objectif au nord, centré sur l'œil de
 * {@link CameraSession} : tourné autour de ce point, l'objectif reste où la vue
 * se place. Il n'est pas dessiné quand on regarde par lui — on serait dedans.
 *
 * <p>En 26.3, un modèle qu'aucun état de bloc ne réclame se demande par une
 * clé ({@link #CLÉ}, {@link #ajouterModèle}) et se lit par elle.
 */
public class CameraStandRenderer implements BlockEntityRenderer<CameraStandBlockEntity, CameraStandRenderer.État> {
	public static final Identifier TÊTE = JN1.id("block/camera_stand_head");
	/** La clé du modèle de l'appareil, chargé à part ({@link #ajouterModèle}). */
	public static final ExtraModelKey<BlockStateModel> CLÉ = ExtraModelKey.create(TÊTE::toString);
	/** L'œil de `CameraSession`, en blocs. */
	private static final double PIVOT = 0.62;

	/** Le cadrage réglé ; rien à dessiner quand on regarde par lui. */
	public static class État extends BlockEntityRenderState {
		float lacet;
		float tangage;
		boolean caché;
	}

	public CameraStandRenderer(Context contexte) {
	}

	/** À la demande des modèles ({@code ModelLoadingPlugin}) : l'appareil, que le jeu ne chargerait pas seul. */
	public static void ajouterModèle(ModelLoadingPlugin.Context contexte) {
		contexte.addModel(CLÉ, SimpleUnbakedExtraModel.blockStateModel(TÊTE));
	}

	@Override
	public État createRenderState() {
		return new État();
	}

	@Override
	public void extractRenderState(CameraStandBlockEntity pied, État état, float tickDelta, Vec3 caméra,
	                               ModelFeatureRenderer.@Nullable CrumblingOverlay cassure) {
		BlockEntityRenderer.super.extractRenderState(pied, état, tickDelta, caméra, cassure);
		état.lacet = pied.lacet();
		état.tangage = pied.tangage();
		état.caché = CameraSession.vueDéportée() && pied.getBlockPos().equals(CameraSession.pied());
	}

	@Override
	public void submit(État état, PoseStack matrices, SubmitNodeCollector tâches, CameraRenderState caméra) {
		if (état.caché) {
			return;
		}
		BlockStateModel modèle = Minecraft.getInstance().getModelManager().getModel(CLÉ);
		if (modèle == null) {
			return;
		}
		List<BlockStateModelPart> parties = new ArrayList<>();
		modèle.collectParts(RandomSource.create(42L), parties);
		matrices.pushPose();
		matrices.translate(0.5, PIVOT, 0.5);
		// Objectif au nord, soit un lacet de 180° ; un tangage positif le baisse.
		matrices.rotate(Axis.YP.rotationDegrees(180.0F - état.lacet));
		matrices.rotate(Axis.XP.rotationDegrees(-état.tangage));
		matrices.translate(-0.5, -PIVOT, -0.5);
		tâches.submitBlockModel(matrices, Sheets.cutoutBlockItemSheet(), parties, BlockModelRenderState.EMPTY_TINTS,
				état.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		matrices.popPose();
	}
}
