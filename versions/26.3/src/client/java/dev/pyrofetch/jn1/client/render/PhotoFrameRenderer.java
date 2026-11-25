package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pyrofetch.jn1.block.PhotoFrameBlockEntity;
import dev.pyrofetch.jn1.client.photo.PhotoTextures;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Dessine la photo sur son cadre d'ancrage, étalée sur toute l'emprise du
 * format.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 *
 * <p>En 26.3 : la photo et l'orientation du cadre sont relevées à l'image
 * ({@link État}), puis le dessin est soumis.
 */
public class PhotoFrameRenderer implements BlockEntityRenderer<PhotoFrameBlockEntity, PhotoFrameRenderer.État> {
	private static final float DEVANT = 0.0685F;

	/** La photo à poser (rien si le cadre n'en porte pas), et l'état du cadre. */
	public static class État extends BlockEntityRenderState {
		@Nullable
		Identifier texture;
		@Nullable
		PhotoFormat format;
		@Nullable
		BlockState cadre;
	}

	public PhotoFrameRenderer(Context contexte) {
	}

	@Override
	public État createRenderState() {
		return new État();
	}

	@Override
	public void extractRenderState(PhotoFrameBlockEntity cadre, État état, float tickDelta, Vec3 caméra,
	                               ModelFeatureRenderer.@Nullable CrumblingOverlay cassure) {
		BlockEntityRenderer.super.extractRenderState(cadre, état, tickDelta, caméra, cassure);
		état.texture = null;
		état.format = null;
		état.cadre = cadre.getBlockState();
		if (cadre.estAncre()) {
			état.format = cadre.format();
			if (état.format != null) {
				état.texture = PhotoTextures.get(cadre.photo());
			}
		}
	}

	@Override
	public void submit(État cadre, PoseStack matrices, SubmitNodeCollector tâches, CameraRenderState caméra) {
		PhotoFormat format = cadre.format;
		Identifier texture = cadre.texture;
		BlockState état = cadre.cadre;
		if (format == null || texture == null || état == null) {
			return;
		}
		int lumière = cadre.lightCoords;
		AttachFace face = état.getValue(BlockStateProperties.ATTACH_FACE);
		Direction regard = état.getValue(BlockStateProperties.HORIZONTAL_FACING);
		Direction[] axes = PhotoFrameBlockEntity.axes(face, regard);
		Direction droite = axes[0];
		Direction bas = axes[1];
		Direction normale = normale(face, regard);
		float[] coin = coinInitial(droite, bas, normale);
		float largeur = format.casesEnLargeur();
		float hauteur = format.casesEnHauteur();
		tâches.submitCustomGeometry(matrices, RenderTypes.entitySolid(texture), (entrée, sommets) -> {
			Matrix4f matrice = entrée.pose();
			sommet(sommets, entrée, matrice, coin, droite, bas, normale, 0.0F, 0.0F, 0.0F, 0.0F, lumière);
			sommet(sommets, entrée, matrice, coin, droite, bas, normale, 0.0F, hauteur, 0.0F, 1.0F, lumière);
			sommet(sommets, entrée, matrice, coin, droite, bas, normale, largeur, hauteur, 1.0F, 1.0F, lumière);
			sommet(sommets, entrée, matrice, coin, droite, bas, normale, largeur, 0.0F, 1.0F, 0.0F, lumière);
		});
	}

	private static void sommet(
		VertexConsumer sommets,
		Pose entrée,
		Matrix4f matrice,
		float[] coin,
		Direction droite,
		Direction bas,
		Direction normale,
		float versDroite,
		float versBas,
		float u,
		float v,
		int lumière
	) {
		float x = coin[0] + droite.getStepX() * versDroite + bas.getStepX() * versBas;
		float y = coin[1] + droite.getStepY() * versDroite + bas.getStepY() * versBas;
		float z = coin[2] + droite.getStepZ() * versDroite + bas.getStepZ() * versBas;
		sommets.addVertex(matrice, x, y, z)
			.setColor(-1)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(lumière)
			.setNormal(entrée, normale.getStepX(), normale.getStepY(), normale.getStepZ());
	}

	private static Direction normale(AttachFace face, Direction regard) {
		return switch (face) {
			case WALL -> regard;
			case FLOOR -> Direction.UP;
			case CEILING -> Direction.DOWN;
		};
	}

	private static float[] coinInitial(Direction droite, Direction bas, Direction normale) {
		float[] coin = new float[3];
		poser(coin, droite, 0.0F);
		poser(coin, bas, 0.0F);
		int axe = axe(normale);
		coin[axe] = normale.getAxisDirection() == AxisDirection.POSITIVE ? 0.0685F : 0.9315F;
		return coin;
	}

	private static void poser(float[] coin, Direction sens, float marge) {
		coin[axe(sens)] = sens.getAxisDirection() == AxisDirection.POSITIVE ? marge : 1.0F - marge;
	}

	private static int axe(Direction sens) {
		return switch (sens.getAxis()) {
			case X -> 0;
			case Y -> 1;
			case Z -> 2;
		};
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
