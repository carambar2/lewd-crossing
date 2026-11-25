package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.lab.StasisPodBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le tube de stase, côté monde.
 *
 * <p>Il ne fait rien de lui-même : tout ce qui compte est dans son bloc-entité,
 * qui retient qui l'occupe. Le bloc n'est là que pour le porter et le montrer.
 */
public class StasisPodBlock extends LabBlock implements EntityBlock {

	/** Vrai quand le tube garde quelqu'un : son vitrage s'éclaire. */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty OCCUPIED =
			net.minecraft.world.level.block.state.properties.BooleanProperty.create("occupied");

	public StasisPodBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(OCCUPIED, false));
	}

	@Override
	protected void createBlockStateDefinition(
			net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OCCUPIED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StasisPodBlockEntity(pos, state);
	}
}
