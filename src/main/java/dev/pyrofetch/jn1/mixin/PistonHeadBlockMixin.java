package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Une tête ne tient que sur son socle : le jeu l'accepte sur un piston ou un
 * piston collant, selon son type. La nôtre ne tient que sur le Cummy piston,
 * et lui ne porte que la nôtre. Sans cela, la tête sortie se détacherait au
 * premier voisin mis à jour.
 */
@Mixin(PistonHeadBlock.class)
public abstract class PistonHeadBlockMixin {
	@Inject(method = "isAttached", at = @At("HEAD"), cancellable = true)
	private void jn1$cummy(BlockState tête, BlockState socle, CallbackInfoReturnable<Boolean> cir) {
		boolean nôtre = tête.isOf(ModBlocks.CUMMY_PISTON_HEAD);
		if (nôtre || socle.isOf(ModBlocks.CUMMY_PISTON)) {
			cir.setReturnValue(nôtre && socle.isOf(ModBlocks.CUMMY_PISTON)
					&& socle.get(PistonBlock.EXTENDED)
					&& socle.get(PistonBlock.FACING) == tête.get(PistonHeadBlock.FACING));
		}
	}
}
