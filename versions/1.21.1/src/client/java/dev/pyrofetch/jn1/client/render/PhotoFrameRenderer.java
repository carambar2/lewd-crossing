package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.block.PhotoFrameBlockEntity;
import dev.pyrofetch.jn1.client.photo.PhotoTextures;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import org.joml.Matrix4f;

/**
 * Dessine la photo sur son cadre d'ancrage, étalée sur toute l'emprise du
 * format.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoFrameRenderer implements BlockEntityRenderer<PhotoFrameBlockEntity> {
	private static final float DEVANT = 0.0685F;

	public PhotoFrameRenderer(Context contexte) {
	}

	public void render(PhotoFrameBlockEntity cadre, float tickDelta, MatrixStack matrices, VertexConsumerProvider fournisseur, int lumière, int recouvrement) {
		if (cadre.estAncre()) {
			PhotoFormat format = cadre.format();
			if (format != null) {
				Identifier texture = PhotoTextures.get(cadre.photo());
				if (texture != null) {
					BlockState état = cadre.getCachedState();
					BlockFace face = état.get(Properties.BLOCK_FACE);
					Direction regard = état.get(Properties.HORIZONTAL_FACING);
					Direction[] axes = PhotoFrameBlockEntity.axes(face, regard);
					Direction droite = axes[0];
					Direction bas = axes[1];
					Direction normale = normale(face, regard);
					float[] coin = coinInitial(droite, bas, normale);
					float largeur = format.casesEnLargeur();
					float hauteur = format.casesEnHauteur();
					VertexConsumer sommets = fournisseur.getBuffer(RenderLayer.getEntitySolid(texture));
					Matrix4f matrice = matrices.peek().getPositionMatrix();
					Entry entrée = matrices.peek();
					sommet(sommets, entrée, matrice, coin, droite, bas, normale, 0.0F, 0.0F, 0.0F, 0.0F, lumière);
					sommet(sommets, entrée, matrice, coin, droite, bas, normale, 0.0F, hauteur, 0.0F, 1.0F, lumière);
					sommet(sommets, entrée, matrice, coin, droite, bas, normale, largeur, hauteur, 1.0F, 1.0F, lumière);
					sommet(sommets, entrée, matrice, coin, droite, bas, normale, largeur, 0.0F, 1.0F, 0.0F, lumière);
				}
			}
		}
	}

	private static void sommet(
		VertexConsumer sommets,
		Entry entrée,
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
		float x = coin[0] + droite.getOffsetX() * versDroite + bas.getOffsetX() * versBas;
		float y = coin[1] + droite.getOffsetY() * versDroite + bas.getOffsetY() * versBas;
		float z = coin[2] + droite.getOffsetZ() * versDroite + bas.getOffsetZ() * versBas;
		sommets.vertex(matrice, x, y, z)
			.color(-1)
			.texture(u, v)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lumière)
			.normal(entrée, normale.getOffsetX(), normale.getOffsetY(), normale.getOffsetZ());
	}

	private static Direction normale(BlockFace face, Direction regard) {
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
		coin[axe] = normale.getDirection() == AxisDirection.POSITIVE ? 0.0685F : 0.9315F;
		return coin;
	}

	private static void poser(float[] coin, Direction sens, float marge) {
		coin[axe(sens)] = sens.getDirection() == AxisDirection.POSITIVE ? marge : 1.0F - marge;
	}

	private static int axe(Direction sens) {
		return switch (sens.getAxis()) {
			case X -> 0;
			case Y -> 1;
			case Z -> 2;
		};
	}

	public boolean rendersOutsideBoundingBox(PhotoFrameBlockEntity cadre) {
		return true;
	}

	@Override
	public int getRenderDistance() {
		return 96;
	}
}
