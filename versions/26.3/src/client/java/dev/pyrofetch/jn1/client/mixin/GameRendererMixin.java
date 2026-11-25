package dev.pyrofetch.jn1.client.mixin;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.pyrofetch.jn1.client.render.SceneView;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
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
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void jn1$sansMainDansLaScène(CameraRenderState caméra, PlayerRenderState joueur, GpuTextureView profondeur,
	                                       CallbackInfo ci) {
		if (SceneView.firstPerson() || dev.pyrofetch.jn1.client.gaster.GasterAdieu.actif()) {
			ci.cancel();
		}
	}
}
