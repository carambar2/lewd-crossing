package dev.pyrofetch.jn1.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pyrofetch.jn1.client.terminal.NoteSession;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
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
 *
 * <p>26.3 : les mains à la première personne sont dessinées par
 * {@code FirstPersonHandsAndItemsRenderer}.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class HeldItemMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void jn1$hideHandsWhileReading(float tickDelta, PoseStack matrices, SubmitNodeCollector envoi,
	                                       PlayerRenderState joueur, FirstPersonHandsAndItemsRenderState état,
	                                       CallbackInfo info) {
		if (NoteSession.isActive() || dev.pyrofetch.jn1.client.camera.CameraSession.isActive()) {
			info.cancel();
		}
	}
}
