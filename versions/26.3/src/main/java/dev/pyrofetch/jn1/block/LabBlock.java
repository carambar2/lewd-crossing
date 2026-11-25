package dev.pyrofetch.jn1.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Le mobilier du laboratoire : paillasses, baies, tubes, conduits.
 *
 * <p>Tous se ressemblent au point de ne différer que par leur texture et leur
 * lumière propre. Une seule classe suffit donc : ce qui les distingue tient
 * dans le modèle et dans les réglages passés au constructeur, pas dans le code.
 */
public class LabBlock extends HorizontalDirectionalBlock {

	public LabBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** La face avant regarde le joueur qui pose le bloc. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
}
