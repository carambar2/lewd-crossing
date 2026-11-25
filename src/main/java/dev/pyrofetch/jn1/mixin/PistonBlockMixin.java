package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Le Cummy piston pousse sa propre tête.
 *
 * <p>{@code PistonBlock.move} nomme {@code Blocks.PISTON_HEAD} trois fois : la
 * tête qu'on retire au retrait, celle qui naît à la sortie, et le voisinage
 * qu'on prévient. Pour notre socle, c'est notre tête.
 */
@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {
	@Redirect(method = "move", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
			target = "Lnet/minecraft/block/Blocks;PISTON_HEAD:Lnet/minecraft/block/Block;"))
	private Block jn1$tête() {
		return (Object) this == ModBlocks.CUMMY_PISTON ? ModBlocks.CUMMY_PISTON_HEAD : Blocks.PISTON_HEAD;
	}

	/** Sorti, il ne se laisse pas pousser — comme les pistons du jeu, que la méthode nomme. */
	@Inject(method = "isMovable", at = @At("HEAD"), cancellable = true)
	private static void jn1$sortiImmobile(BlockState state, World world, BlockPos pos, Direction direction,
	                                      boolean canBreak, Direction pistonDir, CallbackInfoReturnable<Boolean> cir) {
		if (state.isOf(ModBlocks.CUMMY_PISTON) && state.get(PistonBlock.EXTENDED)) {
			cir.setReturnValue(false);
		}
	}
}
