package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pendant qu'il rentre, le socle en mouvement reconstitue la tête qui recule
 * (forme de collision, choc des entités). Pour notre socle, notre tête.
 */
@Mixin(PistonBlockEntity.class)
public abstract class PistonBlockEntityMixin {
	@Shadow
	private BlockState pushedBlock;

	@Redirect(method = {"getHeadBlockState", "getCollisionShape"}, at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
			target = "Lnet/minecraft/block/Blocks;PISTON_HEAD:Lnet/minecraft/block/Block;"))
	private Block jn1$tête() {
		return this.pushedBlock.isOf(ModBlocks.CUMMY_PISTON) ? ModBlocks.CUMMY_PISTON_HEAD : Blocks.PISTON_HEAD;
	}
}
