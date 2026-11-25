package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.lift.PlatformEntity;
import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Une plateforme en route : chacun de ses blocs, dans l'apparence qu'il
 * imite, à la lumière de la case qu'il traverse.
 */
public class PlatformEntityRenderer extends EntityRenderer<PlatformEntity> {
	public PlatformEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(PlatformEntity plateforme, float lacet, float tickDelta, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière) {
		var rendu = MinecraftClient.getInstance().getBlockRenderManager();
		BlockPos a = plateforme.ancre();
		double x = plateforme.getLerpedPos(tickDelta).x;
		double y = plateforme.getLerpedPos(tickDelta).y;
		double z = plateforme.getLerpedPos(tickDelta).z;
		// Arrivée : dès que les vrais blocs sont là, on ne dessine plus (ils se superposeraient).
		if (!plateforme.enMouvement() && !plateforme.blocs().isEmpty()) {
			PlatformEntity.Bloc premier = plateforme.blocs().getFirst();
			if (plateforme.getWorld().getBlockState(new BlockPos(a.getX() + premier.x(), plateforme.destination(),
					a.getZ() + premier.z())).getBlock() instanceof dev.pyrofetch.jn1.lift.PlatformBlock) {
				return;
			}
		}
		for (PlatformEntity.Bloc b : plateforme.blocs()) {
			BlockState état = b.apparence() != null ? b.apparence() : ModBlocks.LIFT_PLATFORM.getDefaultState();
			matrices.push();
			matrices.translate(a.getX() + b.x() - x, 0.0, a.getZ() + b.z() - z);
			int clarté = WorldRenderer.getLightmapCoordinates(plateforme.getWorld(),
					BlockPos.ofFloored(a.getX() + b.x() + 0.5, y + 1.0, a.getZ() + b.z() + 0.5));
			rendu.renderBlockAsEntity(état, matrices, tampons, clarté, OverlayTexture.DEFAULT_UV);
			matrices.pop();
		}
		super.render(plateforme, lacet, tickDelta, matrices, tampons, lumière);
	}

	@Override
	public Identifier getTexture(PlatformEntity plateforme) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
