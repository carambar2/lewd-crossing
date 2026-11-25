package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le geste du plaisir, à la première personne : l'objet descend au bas de
 * l'écran, couché vers l'avant, et va et vient le long de son axe — deux
 * allers par seconde et demie. Les objets n'ont pas de geste à eux
 * ({@code UseAction.NONE}) : celui de la brosse, qu'ils empruntaient, le
 * balayait de côté.
 */
@Mixin(HeldItemRenderer.class)
public abstract class PleasureHandMixin {
	@Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;"
					+ "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Z"
					+ "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
	private void jn1$vaEtVient(AbstractClientPlayerEntity joueur, float tickDelta, float pitch, Hand main,
	                           float swingProgress, ItemStack pile, float equipProgress, MatrixStack matrices,
	                           VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (!joueur.isUsingItem() || joueur.getActiveHand() != main
				|| !(pile.isOf(ModItems.MASTURBATOR) || pile.getItem() instanceof dev.pyrofetch.jn1.cum.DildoItem)) {
			return;
		}
		boolean droite = (main == Hand.MAIN_HAND) == (joueur.getMainArm() == Arm.RIGHT);
		float côté = droite ? 1.0F : -1.0F;
		float temps = joueur.getItemUseTime() + tickDelta;
		float course = (float) Math.sin(temps * 0.85F);
		// Vers le bas et le centre de l'écran, puis couché vers l'avant.
		matrices.translate(-côté * 0.18F, -0.32F + course * 0.11F, -0.08F + course * 0.05F);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(côté * 8.0F * course));
	}
}
