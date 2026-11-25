package dev.pyrofetch.jn1.gloryhole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La cloison à trou : un panneau mince de deux blocs de haut, percé d'un trou
 * rond à hauteur de bouche d'une fille à genoux.
 *
 * <p>Un clic droit d'un côté fait passer la fille la plus proche de l'autre
 * côté, à genoux face au trou, et le joueur prend la place de {@code steve}
 * ({@link GloryWall}). Accroupi, le clic arrête la scène.
 *
 * <p>Deux blocs comme une porte ou le distributeur : la moitié basse porte le
 * trou, et c'est elle qu'on désigne. Le panneau fait deux pixels d'épaisseur,
 * au milieu du bloc, dans l'axe de {@link #FACING}.
 */
public class GloryWallBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	// 26.3 : les blocs n'ont plus de codec (`codec()` a disparu).

	private static final VoxelShape NORD_SUD = Block.box(0, 0, 7, 16, 16, 9);
	private static final VoxelShape EST_OUEST = Block.box(7, 0, 0, 9, 16, 16);

	public GloryWallBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
	}


	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORD_SUD : EST_OUEST;
	}

	/** Le panneau se pose face au joueur, s'il y a la place pour la moitié haute. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level world = context.getLevel();
		// 26.3 : `getMaxY` est le dernier bloc posable (c'était `getMaxBuildHeight() - 1`).
		if (pos.getY() >= world.getMaxY() || !world.getBlockState(pos.above()).canBeReplaced(context)) {
			return null;
		}
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			return true;
		}
		BlockState dessous = world.getBlockState(pos.below());
		return dessous.is(this) && dessous.getValue(HALF) == DoubleBlockHalf.LOWER;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, net.minecraft.world.level.ScheduledTickAccess ticks,
	                                 BlockPos pos, Direction direction, BlockPos posVoisin, BlockState voisin,
	                                 net.minecraft.util.RandomSource aléa) {
		DoubleBlockHalf moitié = state.getValue(HALF);
		boolean versLAutre = direction == (moitié == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
		if (versLAutre && (!voisin.is(this) || voisin.getValue(HALF) == moitié)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, world, ticks, pos, direction, posVoisin, voisin, aléa);
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		// À main nue non plus : le bas, retiré par voisinage, aurait rendu l'objet sans l'outil.
		if (!world.isClientSide() && (player.isCreative() || !player.hasCorrectToolForDrops(state))
				&& state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos bas = pos.below();
			BlockState dessous = world.getBlockState(bas);
			if (dessous.is(this) && dessous.getValue(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlock(bas, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				world.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, bas, Block.getId(dessous));
			}
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer joueur) {
			BlockPos bas = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
			GloryWall.utiliser(joueur, bas, world.getBlockState(bas));
		}
		return InteractionResult.CONSUME;
	}
}
