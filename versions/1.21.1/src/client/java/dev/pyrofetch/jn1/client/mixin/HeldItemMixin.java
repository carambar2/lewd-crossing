package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.terminal.NoteSession;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Range les mains pendant qu'on lit une note.
 *
 * <p>La vue vient se poser à quelques dizaines de centimètres de la feuille, et
 * le bras du joueur reste dessiné au premier plan, en travers de la page. Devant
 * un terminal la main sur le clavier a du sens ; penché sur un papier, non.
 *
 * <p>Même chose l'œil au viseur de l'appareil photo : la main tiendrait
 * l'appareil en travers de ce qu'on cadre.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemMixin {
	@Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"
			+ "Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
			at = @At("HEAD"), cancellable = true)
	private void jn1$hideHandsWhileReading(float tickDelta, MatrixStack matrices,
			VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player,
			int light, CallbackInfo info) {
		if (NoteSession.isActive() || dev.pyrofetch.jn1.client.camera.CameraSession.isActive()) {
			info.cancel();
		}
	}
}
