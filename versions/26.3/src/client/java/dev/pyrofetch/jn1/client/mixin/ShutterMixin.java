package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.camera.CameraSession;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pendant la prise de vue, le clic gauche déclenche au lieu de frapper, et ni
 * le minage ni l'usage d'objet ne passent.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
@Mixin(Minecraft.class)
public abstract class ShutterMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void jn1$shutterInsteadOfSwing(CallbackInfoReturnable<Boolean> info) {
		if (CameraSession.verrouilleLeClic()) {
			info.setReturnValue(false);
		} else {
			if (CameraSession.isActive()) {
				CameraSession.déclencher();
				info.setReturnValue(false);
			}
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void jn1$noMiningThroughTheViewfinder(boolean breaking, CallbackInfo info) {
		if (CameraSession.isActive() || CameraSession.verrouilleLeClic()) {
			info.cancel();
		}
	}

	/** Au trépied, et en main tant que le clic droit qui a ouvert le viseur n'est pas relâché. */
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void jn1$noBlindUseFromTheStand(CallbackInfo info) {
		if (CameraSession.bloqueLUsage()) {
			info.cancel();
		}
	}
}
