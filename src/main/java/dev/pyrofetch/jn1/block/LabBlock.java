package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.Direction;

/**
 * Le mobilier du laboratoire : paillasses, baies, tubes, conduits.
 *
 * <p>Tous se ressemblent au point de ne différer que par leur texture et leur
 * lumière propre. Une seule classe suffit donc : ce qui les distingue tient
 * dans le modèle et dans les réglages passés au constructeur, pas dans le code.
 */
public class LabBlock extends HorizontalFacingBlock {
	public static final MapCodec<LabBlock> CODEC = createCodec(LabBlock::new);

	public LabBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** La face avant regarde le joueur qui pose le bloc. */
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}
}
