package dev.pyrofetch.jn1.gloryhole;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;

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
public class GloryWallBlock extends HorizontalFacingBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
	public static final MapCodec<GloryWallBlock> CODEC = createCodec(GloryWallBlock::new);

	private static final VoxelShape NORD_SUD = Block.createCuboidShape(0, 0, 7, 16, 16, 9);
	private static final VoxelShape EST_OUEST = Block.createCuboidShape(7, 0, 0, 9, 16, 16);

	public GloryWallBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH).with(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(FACING).getAxis() == Direction.Axis.Z ? NORD_SUD : EST_OUEST;
	}

	/** Le panneau se pose face au joueur, s'il y a la place pour la moitié haute. */
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockPos pos = context.getBlockPos();
		World world = context.getWorld();
		if (pos.getY() >= world.getTopY() - 1 || !world.getBlockState(pos.up()).canReplace(context)) {
			return null;
		}
		return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		if (state.get(HALF) == DoubleBlockHalf.LOWER) {
			return true;
		}
		BlockState dessous = world.getBlockState(pos.down());
		return dessous.isOf(this) && dessous.get(HALF) == DoubleBlockHalf.LOWER;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState voisin,
	                                               WorldAccess world, BlockPos pos, BlockPos posVoisin) {
		DoubleBlockHalf moitié = state.get(HALF);
		boolean versLAutre = direction == (moitié == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
		if (versLAutre && (!voisin.isOf(this) || voisin.get(HALF) == moitié)) {
			return Blocks.AIR.getDefaultState();
		}
		return super.getStateForNeighborUpdate(state, direction, voisin, world, pos, posVoisin);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		// À main nue non plus : le bas, retiré par voisinage, aurait rendu l'objet sans l'outil.
		if (!world.isClient() && (player.isCreative() || !player.canHarvest(state))
				&& state.get(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos bas = pos.down();
			BlockState dessous = world.getBlockState(bas);
			if (dessous.isOf(this) && dessous.get(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlockState(bas, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
				world.syncWorldEvent(player, WorldEvents.BLOCK_BROKEN, bas, Block.getRawIdFromState(dessous));
			}
		}
		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity joueur) {
			BlockPos bas = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
			GloryWall.utiliser(joueur, bas, world.getBlockState(bas));
		}
		return ActionResult.CONSUME;
	}
}
