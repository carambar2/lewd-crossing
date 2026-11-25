package dev.pyrofetch.jn1.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionGesture;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Le cadeau qu'une compagne vient de recevoir, tenu à la main : dessiné sur
 * l'os {@code items}, le temps du geste de réception. C'est l'objet même
 * qu'on lui a donné ({@link CompanionEntity#getGift()}, suivi par le
 * serveur) ; jusqu'en 0.56.3, une émeraude quel que soit le cadeau.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> L'objet est relevé à l'image
 * ({@link #addRenderData}), puis dessiné sur l'os. GeckoLib 4 posait l'objet
 * en appliquant <em>deux fois</em> la rotation de l'os (celle de la
 * préparation de sa matrice, puis {@code translateAndRotateMatrixForBone}) ;
 * GeckoLib 5 ne l'applique qu'une : on refait la seconde, sans quoi le cadeau
 * changerait de place dans sa main — l'os porte une forte rotation de repos
 * (−90°, −37,5°, −92,25°), sur laquelle {@code jn1_gift} a été réglé.
 */
public class GiftLayer<T extends CompanionEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
	private static final String ANCHOR = "items";
	/**
	 * Un bloc tenu est dessiné comme dans la main d'un joueur, plus gros que sa
	 * tête à elle (vu avec un bloc d'émeraude) : réduit d'autant.
	 */
	private static final float BLOC = 0.6F;

	/** Le cadeau relevé à l'image, et si c'est un bloc. */
	private record Cadeau(ItemStackRenderState objet, boolean bloc) {
	}

	private static final DataTicket<Cadeau> CADEAU = DataTicket.create("jn1_cadeau", Cadeau.class);

	private final ItemModelResolver résolveur;

	public GiftLayer(CompanionRenderer<T> renderer, ItemModelResolver résolveur) {
		super(renderer);
		this.résolveur = résolveur;
	}

	@Nullable
	protected ItemStack getStack(T animatable) {
		if (animatable.getGesture() != CompanionGesture.RECEIVE) {
			return null;
		}
		ItemStack cadeau = animatable.getGift();
		return cadeau.isEmpty() ? null : cadeau;
	}

	@Override
	public void addRenderData(T animatable, @Nullable Void relatedObject, LivingEntityRenderState état, float partialTick) {
		ItemStack cadeau = this.getStack(animatable);
		if (cadeau != null) {
			état.addGeckolibData(CADEAU, new Cadeau(RenderUtil.createRenderStateForItem(cadeau, this.résolveur,
					ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, animatable), cadeau.getItem() instanceof BlockItem));
		}
	}

	@Override
	public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info,
	                             BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
		Cadeau cadeau = info.getGeckolibData(CADEAU);
		if (!info.willRender() || cadeau == null || cadeau.objet().isEmpty()) {
			return;
		}
		info.model().getBone(ANCHOR).ifPresent(os -> consumer.accept(os, (passe, bone, renderTasks) -> {
			PoseStack pile = passe.poseStack();
			// La pile est au pivot de l'os, sa rotation appliquée une fois :
			// la seconde de GeckoLib 4.
			float rotX = bone.baseRotX();
			float rotY = bone.baseRotY();
			float rotZ = bone.baseRotZ();
			if (bone.frameSnapshot != null) {
				rotX += bone.frameSnapshot.getRotX();
				rotY += bone.frameSnapshot.getRotY();
				rotZ += bone.frameSnapshot.getRotZ();
			}
			RenderUtil.optionalRotateZYX(pile, rotZ, rotY, rotX);
			if (cadeau.bloc()) {
				pile.scale(BLOC, BLOC, BLOC);
			}
			cadeau.objet().submit(pile, renderTasks, passe.packedLight(), passe.packedOverlay(),
					passe.renderState().outlineColor);
		}));
	}
}
