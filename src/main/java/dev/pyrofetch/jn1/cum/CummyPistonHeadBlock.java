package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * La tête du Cummy piston : celle du piston collant, sa plaque nappée de cum.
 *
 * <p>Le jeu ne connaît qu'une tête, {@code Blocks.PISTON_HEAD}, et la nomme en
 * dur partout où elle naît ou se reconnaît. Les mixins du piston
 * ({@code PistonBlockMixin}, {@code PistonHeadBlockMixin},
 * {@code PistonBlockEntityMixin} et, côté client,
 * {@code PistonBlockEntityRendererMixin}) y substituent celle-ci quand le
 * socle est le nôtre. Elle est toujours collante : ses deux types ont le même
 * modèle. Un end rod posé sur elle devient un dildo, comme sur le socle rentré.
 */
public class CummyPistonHeadBlock extends PistonHeadBlock {
	public CummyPistonHeadBlock(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
		return new ItemStack(ModBlocks.CUMMY_PISTON);
	}

	@Override
	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
	                              boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
		if (world.getBlockState(pos).isOf(this)) {
			DildoBlock.convertir(world, pos.offset(state.get(FACING)), state.get(FACING));
		}
	}
}
