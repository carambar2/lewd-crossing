package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * Des planches clouées en travers, plaquées contre la face qu'on vise.
 *
 * <p>Elle se pose comme un levier : contre un mur, au sol ou au plafond, sur
 * n'importe laquelle des six faces d'un bloc voisin. C'est ce qui la rend utile
 * pour condamner une ouverture — une porte, une fenêtre, une trappe — sans
 * occuper la case qu'elle barre.
 *
 * <p>Elle ne bloque personne : sa forme sert au clic et à la vue, pas à la
 * collision. Une barricade qui arrête le joueur ferait de chaque pose un piège,
 * alors qu'elle est là pour se voir.
 */
public class BarricadeBlock extends WallMountedBlock implements net.minecraft.block.Waterloggable {
	public static final MapCodec<BarricadeBlock> CODEC = createCodec(BarricadeBlock::new);

	/** Son épaisseur : les lattes droites (2) et celles en croix posées dessus (1,5). */
	private static final double ÉPAISSEUR = 3.5;

	private static final VoxelShape SOL = Block.createCuboidShape(0, 0, 0, 16, ÉPAISSEUR, 16);
	private static final VoxelShape PLAFOND = Block.createCuboidShape(0, 16 - ÉPAISSEUR, 0, 16, 16, 16);
	private static final VoxelShape MUR_NORD = Block.createCuboidShape(0, 0, 0, 16, 16, ÉPAISSEUR);
	private static final VoxelShape MUR_SUD = Block.createCuboidShape(0, 0, 16 - ÉPAISSEUR, 16, 16, 16);
	private static final VoxelShape MUR_OUEST = Block.createCuboidShape(0, 0, 0, ÉPAISSEUR, 16, 16);
	private static final VoxelShape MUR_EST = Block.createCuboidShape(16 - ÉPAISSEUR, 0, 0, 16, 16, 16);

	public BarricadeBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACE, BlockFace.WALL)
				.with(FACING, Direction.NORTH)
				.with(Properties.WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends WallMountedBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, Properties.WATERLOGGED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState état = super.getPlacementState(context);
		if (état == null) {
			return null;
		}
		return état.with(Properties.WATERLOGGED,
				context.getWorld().getFluidState(context.getBlockPos()).getFluid() == Fluids.WATER);
	}

	/**
	 * La planche se colle du côté de son appui.
	 *
	 * <p>Posée sur un mur, {@code FACING} dit vers où elle regarde ; c'est donc
	 * le côté opposé qui porte, et la plaque doit s'y plaquer.
	 */
	@Override
	protected VoxelShape getOutlineShape(BlockState état, BlockView monde, BlockPos pos,
			ShapeContext contexte) {
		return switch (état.get(FACE)) {
			case FLOOR -> SOL;
			case CEILING -> PLAFOND;
			case WALL -> switch (état.get(FACING)) {
				case NORTH -> MUR_SUD;
				case SOUTH -> MUR_NORD;
				case WEST -> MUR_EST;
				default -> MUR_OUEST;
			};
		};
	}

	/** On la traverse : elle condamne à l'œil, pas au corps. */
	@Override
	protected VoxelShape getCollisionShape(BlockState état, BlockView monde, BlockPos pos,
			ShapeContext contexte) {
		return VoxelShapes.empty();
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState état, Direction côté,
			BlockState voisin, WorldAccess monde, BlockPos pos, BlockPos posVoisin) {
		if (état.get(Properties.WATERLOGGED)) {
			monde.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(monde));
		}
		return super.getStateForNeighborUpdate(état, côté, voisin, monde, pos, posVoisin);
	}

	@Override
	protected FluidState getFluidState(BlockState état) {
		return état.get(Properties.WATERLOGGED)
				? Fluids.WATER.getStill(false)
				: super.getFluidState(état);
	}
}
