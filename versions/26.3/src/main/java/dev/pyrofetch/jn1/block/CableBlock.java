package dev.pyrofetch.jn1.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un conduit qui se raccorde de lui-même, dans les six directions.
 *
 * <p>Il n'y a pas d'entité de bloc là-dessous, et c'est délibéré : ce que le
 * câble a besoin de retenir — quels côtés sont raccordés — tient dans six
 * booléens, donc dans l'état de bloc, que le jeu stocke déjà et synchronise
 * sans rien coûter. Une entité de bloc par tronçon voudrait dire un objet Java,
 * une entrée dans le chunk et une passe de sauvegarde pour chaque bout de
 * tuyau ; sur une installation de plusieurs centaines de blocs, cela se paie.
 * Le jour où un câble devra transporter quelque chose — un fluide, une charge,
 * un signal avec une valeur — l'entité deviendra nécessaire et viendra se
 * greffer là-dessus sans que la géométrie change.
 *
 * <p>Le raccordement suit la règle des vitres, étendue à la verticale : le
 * câble se lie à ses semblables, et se termine dans toute face pleine — un mur,
 * une machine. C'est ce qui lui permet d'entrer dans un appareil au lieu de
 * s'arrêter net devant.
 */
public class CableBlock extends PipeBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {

	/** Le rayon du tronçon : six seizièmes de bloc, comme le cœur du modèle. */
	public static final float RAYON = 3.0F / 16.0F;

	public CableBlock(Properties settings) {
		super(RAYON, settings);
		BlockState base = this.getStateDefinition().any()
				.setValue(BlockStateProperties.WATERLOGGED, false);
		for (Direction côté : Direction.values()) {
			base = base.setValue(PROPERTY_BY_DIRECTION.get(côté), false);
		}
		this.registerDefaultState(base);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, BlockStateProperties.WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		LevelReader monde = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState état = this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,
				context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
		for (Direction côté : Direction.values()) {
			BlockPos voisin = pos.relative(côté);
			état = état.setValue(PROPERTY_BY_DIRECTION.get(côté),
					seRaccorde(monde.getBlockState(voisin), monde, voisin, côté));
		}
		return état;
	}

	/**
	 * Le voisin change : on refait le seul côté concerné.
	 *
	 * <p>Recalculer les six reviendrait à interroger cinq blocs qui n'ont pas
	 * bougé, à chaque pose et à chaque casse alentour.
	 */
	@Override
	protected BlockState updateShape(BlockState état, LevelReader monde, ScheduledTickAccess ticks,
			BlockPos pos, Direction côté, BlockPos posVoisin, BlockState voisin, RandomSource random) {
		if (état.getValue(BlockStateProperties.WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(monde));
		}
		return état.setValue(PROPERTY_BY_DIRECTION.get(côté),
				seRaccorde(voisin, monde, posVoisin, côté));
	}

	/**
	 * Un câble se lie à un autre câble, et se termine dans une face pleine.
	 *
	 * <p>Le second cas est ce qui distingue un conduit d'une simple clôture : un
	 * tuyau qui bute contre une machine doit y entrer, pas s'arrêter à un
	 * cheveu du bloc.
	 */
	private static boolean seRaccorde(BlockState voisin, BlockGetter monde, BlockPos pos,
			Direction côté) {
		if (voisin.getBlock() instanceof CableBlock) {
			return true;
		}
		return voisin.isFaceSturdy(monde, pos, côté.getOpposite());
	}

	/**
	 * Tourner ou retourner une structure doit emporter les raccords avec elle :
	 * sans cela, un câble tourné d'un quart de tour garde ses bras vers les
	 * anciens points cardinaux et ne rejoint plus rien.
	 */
	@Override
	protected BlockState rotate(BlockState état, net.minecraft.world.level.block.Rotation rotation) {
		return transposer(état, rotation::rotate);
	}

	@Override
	protected BlockState mirror(BlockState état, net.minecraft.world.level.block.Mirror miroir) {
		return transposer(état, miroir::mirror);
	}

	/** Les quatre bras horizontaux, chacun reporté sur la direction transformée. */
	private static BlockState transposer(BlockState état,
	                                     java.util.function.UnaryOperator<Direction> vers) {
		BlockState transformé = état;
		for (Direction côté : Direction.Plane.HORIZONTAL) {
			transformé = transformé.setValue(PROPERTY_BY_DIRECTION.get(vers.apply(côté)),
					état.getValue(PROPERTY_BY_DIRECTION.get(côté)));
		}
		return transformé;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState état, BlockGetter monde,
			BlockPos pos, CollisionContext contexte) {
		return this.getShape(état, monde, pos, contexte);
	}

	@Override
	protected FluidState getFluidState(BlockState état) {
		return état.getValue(BlockStateProperties.WATERLOGGED)
				? Fluids.WATER.getSource(false)
				: super.getFluidState(état);
	}
}
