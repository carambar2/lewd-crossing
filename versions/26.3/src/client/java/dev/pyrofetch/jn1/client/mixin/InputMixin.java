package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.interaction.HugSession;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
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
 *
 * <p>26.3 : les touches sont un enregistrement ({@code Input}) et l'élan un
 * vecteur ({@code moveVector}) ; on les remet à zéro, l'accroupi et le sprint
 * gardés tels quels, comme en 1.21.1.
 */
@Mixin(KeyboardInput.class)
public abstract class InputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void jn1$holdStillDuringHug(CallbackInfo ci) {
		if (!HugSession.isHeld() && !dev.pyrofetch.jn1.client.camera.CameraSession.bloqueLeDéplacement()) {
			return;
		}
		Input touches = this.keyPresses;
		this.keyPresses = new Input(false, false, false, false, false, touches.shift(), touches.sprint());
		this.moveVector = Vec2.ZERO;
	}
}
