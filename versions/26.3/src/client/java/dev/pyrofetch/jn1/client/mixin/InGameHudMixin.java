package dev.pyrofetch.jn1.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.pyrofetch.jn1.client.jouets.PogoClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * La jauge du dildo pogo à la place de la barre d'expérience, comme le saut
 * du cheval : l'expérience et son niveau se taisent pendant ce temps.
 *
 * <p>26.3 : la barre sous la barre d'objets est une « barre contextuelle »
 * (expérience, saut de monture, repères) ; tant que la jauge est affichée, on
 * dessine la jauge à la place de son fond, on tait son contenu et le niveau
 * d'expérience.
 */
@Mixin(Hud.class)
public abstract class InGameHudMixin {
	@WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
	private void jn1$jaugeDuPogo(ContextualBar barre, GuiGraphicsExtractor contexte, DeltaTracker compteur,
	                             Operation<Void> original) {
		if (PogoClient.jaugeAffichée()) {
			PogoClient.dessinerJauge(contexte, contexte.guiWidth() / 2 - 91);
		} else {
			original.call(barre, contexte, compteur);
		}
	}

	@WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
	private void jn1$pasDeBarreSurLePogo(ContextualBar barre, GuiGraphicsExtractor contexte, DeltaTracker compteur,
	                                     Operation<Void> original) {
		if (!PogoClient.jaugeAffichée()) {
			original.call(barre, contexte, compteur);
		}
	}

	@WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
	private void jn1$pasDeNiveauSurLePogo(GuiGraphicsExtractor contexte, Font police, int niveau,
	                                      Operation<Void> original) {
		if (!PogoClient.jaugeAffichée()) {
			original.call(contexte, police, niveau);
		}
	}
}
