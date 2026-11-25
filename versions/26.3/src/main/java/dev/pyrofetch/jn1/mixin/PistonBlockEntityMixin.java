package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pendant qu'il rentre, le socle en mouvement reconstitue la tête qui recule
 * (forme de collision, choc des entités). Pour notre socle, notre tête.
 */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonBlockEntityMixin {
	@Shadow
	private BlockState movedState;

	@Redirect(method = {"getCollisionRelatedBlockState", "getCollisionShape"}, at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
			target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
	private Block jn1$tête() {
		return this.movedState.is(ModBlocks.CUMMY_PISTON) ? ModBlocks.CUMMY_PISTON_HEAD : Blocks.PISTON_HEAD;
	}
}
