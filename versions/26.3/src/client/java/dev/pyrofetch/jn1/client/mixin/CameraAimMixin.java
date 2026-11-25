package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.camera.CameraSession;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sur le trépied, la souris oriente l'objectif et non le joueur.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
@Mixin(Entity.class)
public abstract class CameraAimMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void jn1$aimTheCameraInstead(double cursorDeltaX, double cursorDeltaY, CallbackInfo info) {
		if (CameraSession.surPied()) {
			if ((Object) this == Minecraft.getInstance().player) {
				CameraSession.tourner(cursorDeltaX, cursorDeltaY);
				info.cancel();
			}
		}
	}
}
