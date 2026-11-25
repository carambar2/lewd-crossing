package dev.pyrofetch.jn1.client.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.pyrofetch.jn1.client.lab.VoidSky;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le ciel du vide, à la place de celui du jeu (26.3).
 *
 * <p>En 1.21.1, Fabric laissait poser un ciel par dimension
 * ({@code DimensionRenderingRegistry.registerSkyRenderer}) ; ce registre
 * n'existe plus. Le ciel du jeu est désormais une passe du graphe de rendu
 * ({@code addSkyPass}), avant celle du terrain : on y met la nôtre, aux mêmes
 * conditions que celle du jeu (ni sous la neige poudreuse ou la lave, ni
 * aveuglé), et l'on tait celle du jeu.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyMixin {
	@Shadow
	@Final
	private LevelTargetBundle targets;

	@Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
	private void jn1$cielDuVide(FrameGraphBuilder graphe, CameraRenderState caméra, GpuBufferSlice brume,
	                            CallbackInfo ci) {
		if (!VoidSky.actif()) {
			return;
		}
		ci.cancel();
		FogType milieu = caméra.fogType;
		if (milieu == FogType.POWDER_SNOW || milieu == FogType.LAVA
				|| caméra.entityRenderState.doesMobEffectBlockSky) {
			return;
		}
		FramePass passe = graphe.addPass("jn1_void_sky");
		this.targets.main = passe.readsAndWrites(this.targets.main);
		passe.executes(VoidSky::dessiner);
	}
}
