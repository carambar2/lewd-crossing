package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.brewing.Brassage;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nos potions tiennent dans les emplacements à fioles de l'alambic, à la main. */
@Mixin(targets = "net.minecraft.screen.BrewingStandScreenHandler$PotionSlot")
public abstract class BrewingPotionSlotMixin {
	@Inject(method = "matches", at = @At("HEAD"), cancellable = true)
	private static void jn1$fiole(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Brassage.estPotion(stack)) {
			cir.setReturnValue(true);
		}
	}
}
