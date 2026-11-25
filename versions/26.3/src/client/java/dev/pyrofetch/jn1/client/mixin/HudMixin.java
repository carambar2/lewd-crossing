package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.camera.CameraSession;
import dev.pyrofetch.jn1.client.camera.Viewfinder;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le viseur remplace l'interface du jeu tant qu'il est ouvert.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 *
 * <p>26.3 : l'interface en jeu (l'ancien {@code Gui}) s'appelle {@code Hud}, et
 * se relève dans {@code extractRenderState}.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
	@Shadow
	private boolean isHidden;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void jn1$viewfinderTakesOver(GuiGraphicsExtractor contexte, DeltaTracker compteur, CallbackInfo info) {
		if (!dev.pyrofetch.jn1.client.gaster.GasterAdieu.actif() && !CameraSession.isActive()) {
			return;
		}
		// Ce que le jeu fait en tête de méthode, et que le reste du rendu lit
		// (F1 : la main, le contour du bloc visé) : on le garde.
		net.minecraft.client.Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState.isHudHidden = this.isHidden;
		// L'adieu de Gaster : plus d'interface, l'image se dérègle.
		if (dev.pyrofetch.jn1.client.gaster.GasterAdieu.actif()) {
			dev.pyrofetch.jn1.client.gaster.GasterAdieu.render(contexte, compteur.getGameTimeDeltaPartialTick(false));
			info.cancel();
			return;
		}
		if (CameraSession.isActive()) {
			Viewfinder.render(contexte, compteur.getGameTimeDeltaPartialTick(false));
			info.cancel();
		}
	}
}
