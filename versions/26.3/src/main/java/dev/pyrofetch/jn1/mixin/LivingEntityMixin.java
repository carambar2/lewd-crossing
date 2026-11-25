package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pas de plongée du jeu dans le vide.
 *
 * <p>Le client croit un nageur du vide dans l'eau le temps de ses tests de
 * sprint ({@code ClientPlayerEntityMixin}) ; il lui applique donc aussi la
 * plongée de l'eau quand il s'accroupit. La descente est déjà comptée par
 * {@link VoidSwim#avancer}, à la même vitesse que la montée : celle du jeu la
 * doublerait.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	// Le succès « Bon appétit » (un plat au cum avalé) est passé dans FoodPropertiesMixin :
	// en 26.3, LivingEntity.eat n'existe plus, c'est FoodProperties.onConsume qui nourrit.

	/** Sur le dildo pogo, le ressort absorbe la chute. */
	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void jn1$ressort(double distance, float multiplicateur, net.minecraft.world.damagesource.DamageSource source,
	                         org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof net.minecraft.world.entity.player.Player joueur
				&& dev.pyrofetch.jn1.jouets.DildoPogoItem.amortit(joueur)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "goDownInWater", at = @At("HEAD"), cancellable = true)
	private void jn1$pasDePlongée(CallbackInfo ci) {
		if (VoidSwim.nage((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
