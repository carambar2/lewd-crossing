package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pyrofetch.jn1.lift.PlatformEntity;
import dev.pyrofetch.jn1.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Une plateforme en route : chacun de ses blocs, dans l'apparence qu'il
 * imite, à la lumière de la case qu'il traverse.
 *
 * <p>En 26.3, les blocs sont relevés à l'image ({@link État}) — modèle,
 * place et lumière — puis soumis.
 */
public class PlatformEntityRenderer extends EntityRenderer<PlatformEntity, PlatformEntityRenderer.État> {
	private static final BlockDisplayContext AFFICHAGE = BlockDisplayContext.create();

	/** Un bloc porté : sa place par rapport à l'entité, son modèle, sa lumière. */
	record Bloc(double dx, double dz, BlockModelRenderState modèle, int lumière) {
	}

	/** Les blocs à dessiner ; aucun une fois arrivée. */
	public static class État extends EntityRenderState {
		final List<Bloc> blocs = new ArrayList<>();
	}

	private final BlockModelResolver résolveur;

	public PlatformEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.résolveur = context.getBlockModelResolver();
		this.shadowRadius = 0.0F;
	}

	@Override
	public État createRenderState() {
		return new État();
	}

	@Override
	public void extractRenderState(PlatformEntity plateforme, État état, float tickDelta) {
		super.extractRenderState(plateforme, état, tickDelta);
		état.blocs.clear();
		BlockPos a = plateforme.ancre();
		Vec3 lieu = plateforme.getPosition(tickDelta);
		double x = lieu.x;
		double y = lieu.y;
		double z = lieu.z;
		// Arrivée : dès que les vrais blocs sont là, on ne dessine plus (ils se superposeraient).
		if (!plateforme.enMouvement() && !plateforme.blocs().isEmpty()) {
			PlatformEntity.Bloc premier = plateforme.blocs().getFirst();
			if (plateforme.level().getBlockState(new BlockPos(a.getX() + premier.x(), plateforme.destination(),
					a.getZ() + premier.z())).getBlock() instanceof dev.pyrofetch.jn1.lift.PlatformBlock) {
				return;
			}
		}
		for (PlatformEntity.Bloc b : plateforme.blocs()) {
			BlockState apparence = b.apparence() != null ? b.apparence() : ModBlocks.LIFT_PLATFORM.defaultBlockState();
			BlockModelRenderState modèle = new BlockModelRenderState();
			this.résolveur.update(modèle, apparence, AFFICHAGE);
			int clarté = LightCoordsUtil.getLightCoords(plateforme.level(),
					BlockPos.containing(a.getX() + b.x() + 0.5, y + 1.0, a.getZ() + b.z() + 0.5));
			état.blocs.add(new Bloc(a.getX() + b.x() - x, a.getZ() + b.z() - z, modèle, clarté));
		}
	}

	@Override
	public void submit(État état, PoseStack matrices, SubmitNodeCollector tâches, CameraRenderState caméra) {
		for (Bloc b : état.blocs) {
			matrices.pushPose();
			matrices.translate(b.dx(), 0.0, b.dz());
			b.modèle().submit(matrices, tâches, b.lumière(), OverlayTexture.NO_OVERLAY, état.outlineColor);
			matrices.popPose();
		}
		super.submit(état, matrices, tâches, caméra);
	}
}
