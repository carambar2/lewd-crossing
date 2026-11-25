package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.interaction.HugSession;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coupe les commandes de déplacement pendant une étreinte, et quand l'appareil
 * photo le demande (derrière le viseur, ou sur le trépied).
 *
 * <p>L'annulation se fait après coup, une fois les touches lues, plutôt qu'en
 * empêchant la lecture : les touches relâchées entre-temps sont ainsi prises en
 * compte, et le joueur ne se retrouve pas à courir tout seul quand la main lui
 * revient.
 */
@Mixin(KeyboardInput.class)
public abstract class InputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void jn1$holdStillDuringHug(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		if (!HugSession.isHeld() && !dev.pyrofetch.jn1.client.camera.CameraSession.bloqueLeDéplacement()) {
			return;
		}
		Input self = (Input) (Object) this;
		self.movementForward = 0.0F;
		self.movementSideways = 0.0F;
		self.pressingForward = false;
		self.pressingBack = false;
		self.pressingLeft = false;
		self.pressingRight = false;
		self.jumping = false;
	}
}
