package dev.pyrofetch.jn1.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
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
 *
 * <p>26.3 : les mains à la première personne sont dessinées par
 * {@code FirstPersonHandsAndItemsRenderer} ; on se glisse juste avant l'envoi de
 * l'objet au rendu. Ces mains sont toujours celles du joueur local.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class PleasureHandMixin {
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void jn1$vaEtVient(PlayerRenderState étatJoueur, FirstPersonHandsAndItemsRenderState état, float tickDelta,
	                           float pitch, InteractionHand main, float swingProgress, ItemStack pile,
	                           float equipProgress, PoseStack matrices, SubmitNodeCollector envoi, int light,
	                           CallbackInfo ci) {
		LocalPlayer joueur = Minecraft.getInstance().player;
		if (joueur == null || !joueur.isUsingItem() || joueur.getUsedItemHand() != main
				|| !(pile.is(ModItems.MASTURBATOR) || pile.getItem() instanceof dev.pyrofetch.jn1.cum.DildoItem)) {
			return;
		}
		boolean droite = (main == InteractionHand.MAIN_HAND) == (joueur.getMainArm() == HumanoidArm.RIGHT);
		float côté = droite ? 1.0F : -1.0F;
		float temps = joueur.getTicksUsingItem() + tickDelta;
		float course = (float) Math.sin(temps * 0.85F);
		// Vers le bas et le centre de l'écran, puis couché vers l'avant.
		matrices.translate(-côté * 0.18F, -0.32F + course * 0.11F, -0.08F + course * 0.05F);
		matrices.rotate(Axis.XP.rotationDegrees(-55.0F));
		matrices.rotate(Axis.ZP.rotationDegrees(côté * 8.0F * course));
	}
}
