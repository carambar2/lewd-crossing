package dev.pyrofetch.jn1.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le stabilisateur de faille (0.83.8). Un pylône
 * sur socle, deux bobines de cuivre, une pointe lumineuse ; il se pose dans les
 * six sens, pointe vers le dehors de la face visée, comme une baguette de
 * l'End. Le client tire de sa pointe un faisceau vers la faille la plus proche
 * ({@code RiftStabilizerRenderer}) : un faux-semblant, la faille n'en dépend
 * pas — c'est la brèche qui la tient.
 */
public class RiftStabilizerBlock extends DirectionalBlock implements EntityBlock {
	private static final VoxelShape[] FORMES = new VoxelShape[6];

	static {
		for (Direction d : Direction.values()) {
			FORMES[d.get3DDataValue()] = switch (d) {
				case UP -> Block.box(1, 0, 1, 15, 16, 15);
				case DOWN -> Block.box(1, 0, 1, 15, 16, 15);
				case NORTH, SOUTH -> Block.box(1, 1, 0, 15, 15, 16);
				default -> Block.box(0, 1, 1, 16, 15, 15);
			};
		}
	}

	public RiftStabilizerBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getClickedFace());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return FORMES[state.getValue(FACING).get3DDataValue()];
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RiftStabilizerBlockEntity(pos, state);
	}
}
