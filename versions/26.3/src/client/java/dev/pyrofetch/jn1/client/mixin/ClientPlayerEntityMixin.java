package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Le sprint en l'air, dans le vide.
 *
 * <p>Le client coupe le sprint d'un nageur qui ne touche plus l'eau, et refuse
 * de le lancer en l'air hors de l'eau. Pour lui, un nageur du vide est donc
 * « dans l'eau » — mais seulement le temps de ces tests-là, dans
 * {@code tickMovement} : ni la noyade, ni le brouillard, ni la physique de
 * l'eau n'en voient rien. Le même détournement déclencherait la plongée de
 * l'eau quand on s'accroupit ; {@code LivingEntityMixin} l'annule.
 *
 * <p>26.3 : ces tests ont quitté {@code aiStep} pour de petites méthodes
 * ({@code shouldStopSwimSprinting}, {@code canStartSprinting}) ; on les y suit.
 * La plongée, elle, est restée dans {@code aiStep}.
 */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
	@Redirect(method = {"aiStep", "shouldStopSwimSprinting"}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z"))
	private boolean jn1$toucheLEau(LocalPlayer self) {
		return VoidSwim.nage(self) || self.isInWater();
	}

	@Redirect(method = "canStartSprinting", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z"))
	private boolean jn1$sousLEau(LocalPlayer self) {
		return VoidSwim.nage(self) || self.isUnderWater();
	}

	/**
	 * Ni rampé, ni accroupi : pleine vitesse.
	 *
	 * <p>Hors de l'eau, la pose de nage est celle du joueur qui rampe sous un
	 * bloc, et le jeu divise alors la saisie par trois. C'est ce qui rendait le
	 * sprint aussi lent que le pas. S'accroupir, dans le vide, fait descendre :
	 * cela ne doit pas non plus freiner l'avance.
	 */
	/** Debout sur une plateforme d'ascenseur en route, on la suit : voir {@code PlatformRide}. */
	@Inject(method = "aiStep", at = @At("TAIL"))
	private void jn1$portéParLaCabine(CallbackInfo ci) {
		dev.pyrofetch.jn1.client.elevator.PlatformRide.porter((LocalPlayer) (Object) this);
	}

	@Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
	private void jn1$pasRalenti(CallbackInfoReturnable<Boolean> cir) {
		if (VoidSwim.nage((LocalPlayer) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
