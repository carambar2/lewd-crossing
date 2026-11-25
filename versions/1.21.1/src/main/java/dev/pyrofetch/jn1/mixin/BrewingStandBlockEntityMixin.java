package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.brewing.Brassage;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nos potions tiennent dans les emplacements à fioles de l'alambic, par entonnoir aussi. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
	@Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
	private void jn1$fiole(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (slot < 3 && Brassage.estPotion(stack) && ((Inventory) (Object) this).getStack(slot).isEmpty()) {
			cir.setReturnValue(true);
		}
	}
}
