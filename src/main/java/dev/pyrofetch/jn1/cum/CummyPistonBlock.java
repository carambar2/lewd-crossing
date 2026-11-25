package dev.pyrofetch.jn1.cum;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Le Cummy piston : un piston collant du jeu, au cum séché. Un end rod posé
 * sur sa face collante devient un dildo ({@link DildoBlock#convertir}).
 */
public class CummyPistonBlock extends PistonBlock {
	public CummyPistonBlock(Settings settings) {
		super(true, settings);
	}

	@Override
	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
	                              boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
		if (world.getBlockState(pos).isOf(this) && !state.get(EXTENDED)) {
			DildoBlock.convertir(world, pos.offset(state.get(FACING)), state.get(FACING));
		}
	}
}
