package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.camera.CameraSession;
import dev.pyrofetch.jn1.client.camera.Viewfinder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le viseur remplace l'interface du jeu tant qu'il est ouvert.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
@Mixin(InGameHud.class)
public abstract class HudMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void jn1$viewfinderTakesOver(DrawContext contexte, RenderTickCounter compteur, CallbackInfo info) {
		// L'adieu de Gaster : plus d'interface, l'image se dérègle.
		if (dev.pyrofetch.jn1.client.gaster.GasterAdieu.actif()) {
			dev.pyrofetch.jn1.client.gaster.GasterAdieu.render(contexte, compteur.getTickDelta(false));
			info.cancel();
			return;
		}
		if (CameraSession.isActive()) {
			Viewfinder.render(contexte, compteur.getTickDelta(false));
			info.cancel();
		}
	}
}
