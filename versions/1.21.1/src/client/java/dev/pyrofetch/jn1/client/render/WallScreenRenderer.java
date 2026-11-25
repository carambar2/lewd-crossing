package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.block.WallScreenBlock;
import dev.pyrofetch.jn1.block.WallScreenBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Le texte d'un écran mural, étalé sur tout l'ensemble.
 *
 * <p>Seul l'écran du coin supérieur gauche dessine : il mesure l'ensemble, puis
 * choisit la plus grande taille de police qui fasse tenir toutes les lignes.
 * Un écran seul affiche donc un petit texte serré, un mur d'écrans une affiche.
 *
 * <p>Le texte luit : il est rendu à pleine clarté, comme un écran allumé dans
 * une pièce sombre. Les lignes qui commencent par un chevron sont teintées,
 * comme les titres de flux.txt.
 */
public class WallScreenRenderer implements BlockEntityRenderer<WallScreenBlockEntity> {
	private static final int TEXTE = 0xFF9AF0C8;
	private static final int TITRE = 0xFF7FE0FF;
	/** Marge, en blocs, entre le bord de l'ensemble et le texte. */
	private static final float MARGE = WallScreenBlock.MARGE;
	/** Au plus quatre lignes par bloc : au-delà, un mot tiendrait tout un écran. */
	private static final float ÉCHELLE_MAX = 1.0F / 44.0F;
	/** Épaisseur de la dalle : le texte se pose juste devant. */
	private static final float DEVANT = 2.0F / 16.0F + 0.004F;

	private final TextRenderer police;

	public WallScreenRenderer(BlockEntityRendererFactory.Context context) {
		this.police = context.getTextRenderer();
	}

	@Override
	public void render(WallScreenBlockEntity écran, float tickDelta, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière, int recouvrement) {
		if (écran.getWorld() == null) {
			return;
		}
		Direction face = écran.getCachedState().get(WallScreenBlock.FACING);
		if (!WallScreenBlock.estOrigine(écran.getWorld(), écran.getPos(), face)) {
			return;
		}
		List<String> lignes = new ArrayList<>(écran.lignes());
		while (!lignes.isEmpty() && lignes.getLast().isBlank()) {
			lignes.removeLast();
		}
		if (lignes.isEmpty()) {
			return;
		}

		int[] taille = WallScreenBlock.taille(écran.getWorld(), écran.getPos(), face);
		int largeurTexte = 1;
		for (String ligne : lignes) {
			largeurTexte = Math.max(largeurTexte, this.police.getWidth(ligne));
		}
		float échelle = Math.min(ÉCHELLE_MAX, Math.min(
				(taille[0] - 2 * MARGE) / largeurTexte,
				(taille[1] - 2 * MARGE) / (lignes.size() * (float) this.police.fontHeight)));

		matrices.push();
		// On se ramène au cas d'un écran tourné vers le sud, puis au coin
		// supérieur gauche de l'ensemble, vu de face.
		matrices.translate(0.5, 0.5, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-face.asRotation()));
		matrices.translate(-0.5 + MARGE, 0.5 - MARGE, -0.5 + DEVANT);
		matrices.scale(échelle, -échelle, échelle);

		Matrix4f matrice = matrices.peek().getPositionMatrix();
		float y = 0.0F;
		for (String ligne : lignes) {
			int couleur = ligne.startsWith(">") ? TITRE : TEXTE;
			this.police.draw(ligne, 0.0F, y, couleur, false, matrice, tampons,
					TextRenderer.TextLayerType.POLYGON_OFFSET, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
			y += this.police.fontHeight;
		}
		matrices.pop();
	}

	/** Le texte déborde du bloc qui le porte : sans cela, il disparaîtrait en bord d'écran. */
	@Override
	public boolean rendersOutsideBoundingBox(WallScreenBlockEntity écran) {
		return true;
	}
}
