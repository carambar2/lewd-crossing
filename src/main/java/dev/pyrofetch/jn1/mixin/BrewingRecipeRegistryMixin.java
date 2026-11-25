package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.brewing.Brassage;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.BrewingRecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** L'alambic connaît nos recettes (voir {@link Brassage}). */
@Mixin(BrewingRecipeRegistry.class)
public abstract class BrewingRecipeRegistryMixin {
	@Inject(method = "isValidIngredient", at = @At("HEAD"), cancellable = true)
	private void jn1$ingrédient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Brassage.estIngrédient(stack)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "hasRecipe", at = @At("HEAD"), cancellable = true)
	private void jn1$recette(ItemStack base, ItemStack ingrédient, CallbackInfoReturnable<Boolean> cir) {
		if (Brassage.trouver(base, ingrédient) != null) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "craft", at = @At("HEAD"), cancellable = true)
	private void jn1$brasser(ItemStack ingrédient, ItemStack base, CallbackInfoReturnable<ItemStack> cir) {
		Brassage.Recette r = Brassage.trouver(base, ingrédient);
		if (r != null) {
			cir.setReturnValue(new ItemStack(r.résultat()));
		}
	}
}
