package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.render.SceneView;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * À la place de steve, on voit son corps par ses yeux : la main du joueur,
 * dessinée à part par le jeu devant la caméra, flotterait par-dessus.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
	private void jn1$sansMainDansLaScène(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
		if (SceneView.firstPerson() || dev.pyrofetch.jn1.client.gaster.GasterAdieu.actif()) {
			ci.cancel();
		}
	}
}
