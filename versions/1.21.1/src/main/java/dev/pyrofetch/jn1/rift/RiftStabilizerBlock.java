package dev.pyrofetch.jn1.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Le stabilisateur de faille (0.83.8). Un pylône
 * sur socle, deux bobines de cuivre, une pointe lumineuse ; il se pose dans les
 * six sens, pointe vers le dehors de la face visée, comme une baguette de
 * l'End. Le client tire de sa pointe un faisceau vers la faille la plus proche
 * ({@code RiftStabilizerRenderer}) : un faux-semblant, la faille n'en dépend
 * pas — c'est la brèche qui la tient.
 */
public class RiftStabilizerBlock extends FacingBlock implements BlockEntityProvider {
	public static final MapCodec<RiftStabilizerBlock> CODEC = createCodec(RiftStabilizerBlock::new);
	private static final VoxelShape[] FORMES = new VoxelShape[6];

	static {
		for (Direction d : Direction.values()) {
			FORMES[d.getId()] = switch (d) {
				case UP -> Block.createCuboidShape(1, 0, 1, 15, 16, 15);
				case DOWN -> Block.createCuboidShape(1, 0, 1, 15, 16, 15);
				case NORTH, SOUTH -> Block.createCuboidShape(1, 1, 0, 15, 15, 16);
				default -> Block.createCuboidShape(0, 1, 1, 16, 15, 15);
			};
		}
	}

	public RiftStabilizerBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.UP));
	}

	@Override
	protected MapCodec<? extends FacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getSide());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return FORMES[state.get(FACING).getId()];
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new RiftStabilizerBlockEntity(pos, state);
	}
}
