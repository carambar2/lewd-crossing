package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.lab.StasisPodBlockEntity;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Le tube de stase, côté monde.
 *
 * <p>Il ne fait rien de lui-même : tout ce qui compte est dans son bloc-entité,
 * qui retient qui l'occupe. Le bloc n'est là que pour le porter et le montrer.
 */
public class StasisPodBlock extends LabBlock implements BlockEntityProvider {
	public static final MapCodec<StasisPodBlock> CODEC = createCodec(StasisPodBlock::new);

	/** Vrai quand le tube garde quelqu'un : son vitrage s'éclaire. */
	public static final net.minecraft.state.property.BooleanProperty OCCUPIED =
			net.minecraft.state.property.BooleanProperty.of("occupied");

	public StasisPodBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(OCCUPIED, false));
	}

	@Override
	protected void appendProperties(
			net.minecraft.state.StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(OCCUPIED);
	}

	@Override
	public MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new StasisPodBlockEntity(pos, state);
	}
}
