package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionGesture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Le cadeau qu'une compagne vient de recevoir, tenu à la main : dessiné sur
 * l'os {@code items}, le temps du geste de réception. C'est l'objet même
 * qu'on lui a donné ({@link CompanionEntity#getGift()}, suivi par le
 * serveur) ; jusqu'en 0.56.3, une émeraude quel que soit le cadeau.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class GiftLayer<T extends CompanionEntity> extends BlockAndItemGeoLayer<T> {
	private static final String ANCHOR = "items";
	/**
	 * Un bloc tenu est dessiné comme dans la main d'un joueur, plus gros que sa
	 * tête à elle (vu avec un bloc d'émeraude) : réduit d'autant.
	 */
	private static final float BLOC = 0.6F;

	public GiftLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, T animatable) {
		if (animatable.getGesture() != CompanionGesture.RECEIVE || !ANCHOR.equals(bone.getName())) {
			return null;
		}
		ItemStack cadeau = animatable.getGift();
		return cadeau.isEmpty() ? null : cadeau;
	}

	protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
		return ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
	}

	@Override
	protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, T animatable,
	                                  VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
		if (stack.getItem() instanceof BlockItem) {
			poseStack.push();
			poseStack.scale(BLOC, BLOC, BLOC);
			super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
			poseStack.pop();
		} else {
			super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		}
	}
}
