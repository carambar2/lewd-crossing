package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.jouets.PogoClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * La jauge du dildo pogo à la place de la barre d'expérience, comme le saut
 * du cheval : l'expérience et son niveau se taisent pendant ce temps.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "shouldRenderExperience", at = @At("HEAD"), cancellable = true)
	private void jn1$pasDExpérienceSurLePogo(CallbackInfoReturnable<Boolean> cir) {
		if (PogoClient.jaugeAffichée()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "renderMainHud", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/InGameHud;shouldRenderExperience()Z"))
	private void jn1$jaugeDuPogo(DrawContext contexte, RenderTickCounter compteur, CallbackInfo ci) {
		if (PogoClient.jaugeAffichée()) {
			PogoClient.dessinerJauge(contexte, contexte.getScaledWindowWidth() / 2 - 91);
		}
	}
}
