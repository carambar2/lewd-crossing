package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pyrofetch.jn1.block.WallScreenBlock;
import dev.pyrofetch.jn1.block.WallScreenBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
 *
 * <p>En 26.3, les lignes et l'échelle sont relevées à l'image ({@link État}),
 * puis le texte est soumis.
 */
public class WallScreenRenderer implements BlockEntityRenderer<WallScreenBlockEntity, WallScreenRenderer.État> {
	private static final int TEXTE = 0xFF9AF0C8;
	private static final int TITRE = 0xFF7FE0FF;
	/** Marge, en blocs, entre le bord de l'ensemble et le texte. */
	private static final float MARGE = WallScreenBlock.MARGE;
	/** Au plus quatre lignes par bloc : au-delà, un mot tiendrait tout un écran. */
	private static final float ÉCHELLE_MAX = 1.0F / 44.0F;
	/** Épaisseur de la dalle : le texte se pose juste devant. */
	private static final float DEVANT = 2.0F / 16.0F + 0.004F;

	/** Ce qu'il faut écrire : rien si ce n'est pas l'écran d'origine, ou s'il est vide. */
	public static class État extends BlockEntityRenderState {
		final List<String> lignes = new ArrayList<>();
		Direction face = Direction.NORTH;
		float échelle;
	}

	private final Font police;

	public WallScreenRenderer(BlockEntityRendererProvider.Context context) {
		this.police = context.font();
	}

	@Override
	public État createRenderState() {
		return new État();
	}

	@Override
	public void extractRenderState(WallScreenBlockEntity écran, État état, float tickDelta, Vec3 caméra,
	                               ModelFeatureRenderer.@Nullable CrumblingOverlay cassure) {
		BlockEntityRenderer.super.extractRenderState(écran, état, tickDelta, caméra, cassure);
		état.lignes.clear();
		if (écran.getLevel() == null) {
			return;
		}
		Direction face = écran.getBlockState().getValue(WallScreenBlock.FACING);
		if (!WallScreenBlock.estOrigine(écran.getLevel(), écran.getBlockPos(), face)) {
			return;
		}
		List<String> lignes = new ArrayList<>(écran.lignes());
		while (!lignes.isEmpty() && lignes.getLast().isBlank()) {
			lignes.removeLast();
		}
		if (lignes.isEmpty()) {
			return;
		}

		int[] taille = WallScreenBlock.taille(écran.getLevel(), écran.getBlockPos(), face);
		int largeurTexte = 1;
		for (String ligne : lignes) {
			largeurTexte = Math.max(largeurTexte, this.police.width(ligne));
		}
		état.échelle = Math.min(ÉCHELLE_MAX, Math.min(
				(taille[0] - 2 * MARGE) / largeurTexte,
				(taille[1] - 2 * MARGE) / (lignes.size() * (float) this.police.lineHeight)));
		état.face = face;
		état.lignes.addAll(lignes);
	}

	@Override
	public void submit(État état, PoseStack matrices, SubmitNodeCollector tâches, CameraRenderState caméra) {
		if (état.lignes.isEmpty()) {
			return;
		}
		float échelle = état.échelle;
		matrices.pushPose();
		// On se ramène au cas d'un écran tourné vers le sud, puis au coin
		// supérieur gauche de l'ensemble, vu de face.
		matrices.translate(0.5, 0.5, 0.5);
		matrices.rotate(Axis.YP.rotationDegrees(-état.face.toYRot()));
		matrices.translate(-0.5 + MARGE, 0.5 - MARGE, -0.5 + DEVANT);
		matrices.scale(échelle, -échelle, échelle);

		float y = 0.0F;
		for (String ligne : état.lignes) {
			int couleur = ligne.startsWith(">") ? TITRE : TEXTE;
			tâches.submitText(matrices, 0.0F, y, Component.literal(ligne).getVisualOrderText(), false,
					Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, couleur, 0, 0);
			y += this.police.lineHeight;
		}
		matrices.popPose();
	}

	/** Le texte déborde du bloc qui le porte : sans cela, il disparaîtrait en bord d'écran. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
