package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.entity.LivingEntity;
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
	/** Le succès « Bon appétit » : un plat au cum avalé. */
	@Inject(method = "eatFood", at = @At("HEAD"))
	private void jn1$platAuCum(net.minecraft.world.World monde, net.minecraft.item.ItemStack pile,
	                           net.minecraft.component.type.FoodComponent nourriture,
	                           org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.item.ItemStack> cir) {
		if (!monde.isClient() && (Object) this instanceof net.minecraft.entity.player.PlayerEntity joueur
				&& dev.pyrofetch.jn1.cum.AuCumRecipe.déjàAuCum(pile)) {
			dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "au_cum");
			// Le point de plus. Un plat d'avant 0.56.7 le porte déjà dans sa
			// nourriture (plus que celle de l'objet) : rien à ajouter.
			net.minecraft.component.type.FoodComponent défaut =
					pile.getItem().getComponents().get(net.minecraft.component.DataComponentTypes.FOOD);
			if (défaut == null || nourriture.nutrition() <= défaut.nutrition()) {
				joueur.getHungerManager().add(dev.pyrofetch.jn1.cum.AuCumRecipe.BONUS, 0.0F);
			}
		}
	}

	/** Sur le dildo pogo, le ressort absorbe la chute. */
	@Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
	private void jn1$ressort(float distance, float multiplicateur, net.minecraft.entity.damage.DamageSource source,
	                         org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof net.minecraft.entity.player.PlayerEntity joueur
				&& dev.pyrofetch.jn1.jouets.DildoPogoItem.amortit(joueur)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "knockDownwards", at = @At("HEAD"), cancellable = true)
	private void jn1$pasDePlongée(CallbackInfo ci) {
		if (VoidSwim.nage((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
