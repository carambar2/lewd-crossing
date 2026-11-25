package dev.pyrofetch.jn1.client.morph;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pyrofetch.jn1.client.render.CompanionShading;
import java.util.function.BiConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Les objets sont relevés à l'image
 * ({@link #addRenderData}), puis dessinés sur l'os, que GeckoLib 5 présente à
 * son pivot : on revient d'abord au repère de l'os entier, celui où GeckoLib 4
 * appelait le calque.
 */
public class MorphHeldItemLayer extends GeoRenderLayer<MorphAnimatable, AbstractClientPlayer, MorphRenderer.État> {
	private static final String AVANT_BRAS_DROIT = "lowerArmR";
	private static final String AVANT_BRAS_GAUCHE = "lowerArmL";
	/** Deux pixels en avant de la main, comme le jeu. */
	private static final float EN_AVANT = -0.125F;

	private final ItemModelResolver résolveur;

	public MorphHeldItemLayer(MorphRenderer rendu, ItemModelResolver résolveur) {
		super(rendu);
		this.résolveur = résolveur;
	}

	@Override
	public void addRenderData(MorphAnimatable animatable, @Nullable AbstractClientPlayer joueur, MorphRenderer.État état,
	                          float partialTick) {
		état.objetDroit.clear();
		état.objetGauche.clear();
		if (joueur == null) {
			return;
		}
		this.relever(état.objetDroit, joueur, HumanoidArm.RIGHT);
		this.relever(état.objetGauche, joueur, HumanoidArm.LEFT);
	}

	private void relever(ItemStackRenderState état, AbstractClientPlayer joueur, HumanoidArm côté) {
		ItemStack objet = joueur.getMainArm() == côté ? joueur.getMainHandItem() : joueur.getOffhandItem();
		if (!objet.isEmpty()) {
			this.résolveur.updateForLiving(état, objet, côté == HumanoidArm.RIGHT
					? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, joueur);
		}
	}

	@Override
	public void addPerBoneRender(RenderPassInfo<MorphRenderer.État> info,
	                             BiConsumer<GeoBone, PerBoneRender<MorphRenderer.État>> consumer) {
		if (!info.willRender()) {
			return;
		}
		this.main(info, consumer, AVANT_BRAS_DROIT, info.renderState().objetDroit);
		this.main(info, consumer, AVANT_BRAS_GAUCHE, info.renderState().objetGauche);
	}

	private void main(RenderPassInfo<MorphRenderer.État> info,
	                  BiConsumer<GeoBone, PerBoneRender<MorphRenderer.État>> consumer,
	                  String nom, ItemStackRenderState objet) {
		if (objet.isEmpty()) {
			return;
		}
		info.model().getBone(nom).ifPresent(os -> {
			GeoCube avantBras = plusGrandCube(os);
			if (avantBras == null) {
				return;
			}
			consumer.accept(os, (passe, bone, renderTasks) -> {
				PoseStack poseStack = passe.poseStack();
				// GeckoLib 5 place la pile au pivot de l'os ; GeckoLib 4, après lui.
				bone.translateAwayFromPivotPoint(poseStack);
				avantBras.translateToPivotPoint(poseStack);
				avantBras.rotate(poseStack);
				avantBras.translateAwayFromPivotPoint(poseStack);

				float[] boîte = bornes(avantBras);
				poseStack.translate((boîte[0] + boîte[3]) / 2, boîte[1], (boîte[2] + boîte[5]) / 2);
				poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
				poseStack.translate(0.0F, 0.0F, EN_AVANT);
				poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
				poseStack.rotate(Axis.YP.rotationDegrees(180.0F));

				objet.submit(poseStack, renderTasks, passe.packedLight(), OverlayTexture.NO_OVERLAY,
						passe.renderState().outlineColor);
			});
		});
	}

	/** L'avant-bras lui-même : chez Ellie, l'os porte aussi des manchettes. */
	@Nullable
	private static GeoCube plusGrandCube(GeoBone bone) {
		GeoCube meilleur = null;
		float volume = -1;
		for (GeoCube cube : CompanionShading.cubes(bone)) {
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
				b[0] = Math.min(b[0], sommet.posX());
				b[1] = Math.min(b[1], sommet.posY());
				b[2] = Math.min(b[2], sommet.posZ());
				b[3] = Math.max(b[3], sommet.posX());
				b[4] = Math.max(b[4], sommet.posY());
				b[5] = Math.max(b[5], sommet.posZ());
			}
		}
		return b;
	}
}
