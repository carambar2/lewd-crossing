package dev.pyrofetch.jn1.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le succès « Bon appétit » : un plat au cum avalé, et son point de
 * nourriture en plus ({@link dev.pyrofetch.jn1.cum.AuCumRecipe#BONUS}).
 *
 * <p>En 1.21.1, c'était un crochet sur {@code LivingEntity.eat} ; en 26.3, la
 * nourriture passe par le composant de consommation, et c'est
 * {@code FoodProperties.onConsume} qui nourrit le joueur.
 */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
	@Inject(method = "onConsume", at = @At("HEAD"))
	private void jn1$platAuCum(Level monde, LivingEntity mangeur, ItemStack pile, Consumable consommation,
	                           CallbackInfo ci) {
		if (!monde.isClientSide() && mangeur instanceof Player joueur
				&& dev.pyrofetch.jn1.cum.AuCumRecipe.déjàAuCum(pile)) {
			dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "au_cum");
			FoodProperties nourriture = (FoodProperties) (Object) this;
			FoodProperties défaut = pile.getItem().components().get(DataComponents.FOOD);
			if (défaut == null || nourriture.nutrition() <= défaut.nutrition()) {
				joueur.getFoodData().eat(dev.pyrofetch.jn1.cum.AuCumRecipe.BONUS, 0.0F);
			}
		}
	}
}
