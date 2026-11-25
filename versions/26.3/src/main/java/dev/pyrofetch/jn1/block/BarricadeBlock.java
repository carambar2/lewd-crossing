package dev.pyrofetch.jn1.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
public class BarricadeBlock extends FaceAttachedHorizontalDirectionalBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {

	/** Son épaisseur : les lattes droites (2) et celles en croix posées dessus (1,5). */
	private static final double ÉPAISSEUR = 3.5;

	private static final VoxelShape SOL = Block.box(0, 0, 0, 16, ÉPAISSEUR, 16);
	private static final VoxelShape PLAFOND = Block.box(0, 16 - ÉPAISSEUR, 0, 16, 16, 16);
	private static final VoxelShape MUR_NORD = Block.box(0, 0, 0, 16, 16, ÉPAISSEUR);
	private static final VoxelShape MUR_SUD = Block.box(0, 0, 16 - ÉPAISSEUR, 16, 16, 16);
	private static final VoxelShape MUR_OUEST = Block.box(0, 0, 0, ÉPAISSEUR, 16, 16);
	private static final VoxelShape MUR_EST = Block.box(16 - ÉPAISSEUR, 0, 0, 16, 16, 16);

	public BarricadeBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACE, AttachFace.WALL)
				.setValue(FACING, Direction.NORTH)
				.setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, BlockStateProperties.WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState état = super.getStateForPlacement(context);
		if (état == null) {
			return null;
		}
		return état.setValue(BlockStateProperties.WATERLOGGED,
				context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	/**
	 * La planche se colle du côté de son appui.
	 *
	 * <p>Posée sur un mur, {@code FACING} dit vers où elle regarde ; c'est donc
	 * le côté opposé qui porte, et la plaque doit s'y plaquer.
	 */
	@Override
	protected VoxelShape getShape(BlockState état, BlockGetter monde, BlockPos pos,
			CollisionContext contexte) {
		return switch (état.getValue(FACE)) {
			case FLOOR -> SOL;
			case CEILING -> PLAFOND;
			case WALL -> switch (état.getValue(FACING)) {
				case NORTH -> MUR_SUD;
				case SOUTH -> MUR_NORD;
				case WEST -> MUR_EST;
				default -> MUR_OUEST;
			};
		};
	}

	/** On la traverse : elle condamne à l'œil, pas au corps. */
	@Override
	protected VoxelShape getCollisionShape(BlockState état, BlockGetter monde, BlockPos pos,
			CollisionContext contexte) {
		return Shapes.empty();
	}

	@Override
	protected BlockState updateShape(BlockState état, LevelReader monde, ScheduledTickAccess ticks,
			BlockPos pos, Direction côté, BlockPos posVoisin, BlockState voisin, RandomSource random) {
		if (état.getValue(BlockStateProperties.WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(monde));
		}
		return super.updateShape(état, monde, ticks, pos, côté, posVoisin, voisin, random);
	}

	@Override
	protected FluidState getFluidState(BlockState état) {
		return état.getValue(BlockStateProperties.WATERLOGGED)
				? Fluids.WATER.getSource(false)
				: super.getFluidState(état);
	}
}
