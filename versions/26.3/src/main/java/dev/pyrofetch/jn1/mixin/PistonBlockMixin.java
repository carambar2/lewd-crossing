package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
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
@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockMixin {
	@Redirect(method = "moveBlocks", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
			target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
	private Block jn1$tête() {
		return (Object) this == ModBlocks.CUMMY_PISTON ? ModBlocks.CUMMY_PISTON_HEAD : Blocks.PISTON_HEAD;
	}

	/** Sorti, il ne se laisse pas pousser — comme les pistons du jeu, que la méthode nomme. */
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private static void jn1$sortiImmobile(BlockState state, Level world, BlockPos pos, Direction direction,
	                                      boolean canBreak, Direction pistonDir, CallbackInfoReturnable<Boolean> cir) {
		if (state.is(ModBlocks.CUMMY_PISTON) && state.getValue(PistonBaseBlock.EXTENDED)) {
			cir.setReturnValue(false);
		}
	}
}
