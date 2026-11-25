package dev.pyrofetch.jn1.cum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le Cummy piston : un piston collant du jeu, au cum séché. Un end rod posé
 * sur sa face collante devient un dildo ({@link DildoBlock#convertir}).
 */
public class CummyPistonBlock extends PistonBaseBlock {
	public CummyPistonBlock(Properties settings) {
		super(true, settings);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock,
	                              @org.jetbrains.annotations.Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean notify) {
		// 26.3 : le voisin n'est plus désigné par sa position, mais par une orientation.
		super.neighborChanged(state, world, pos, sourceBlock, orientation, notify);
		if (world.getBlockState(pos).is(this) && !state.getValue(EXTENDED)) {
			DildoBlock.convertir(world, pos.relative(state.getValue(FACING)), state.getValue(FACING));
		}
	}
}
