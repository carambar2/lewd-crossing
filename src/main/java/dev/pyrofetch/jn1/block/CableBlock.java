package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

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
public class CableBlock extends ConnectingBlock implements net.minecraft.block.Waterloggable {
	public static final MapCodec<CableBlock> CODEC = createCodec(CableBlock::new);

	/** Le rayon du tronçon : six seizièmes de bloc, comme le cœur du modèle. */
	public static final float RAYON = 3.0F / 16.0F;

	public CableBlock(Settings settings) {
		super(RAYON, settings);
		BlockState base = this.getStateManager().getDefaultState()
				.with(Properties.WATERLOGGED, false);
		for (Direction côté : Direction.values()) {
			base = base.with(FACING_PROPERTIES.get(côté), false);
		}
		this.setDefaultState(base);
	}

	@Override
	protected MapCodec<? extends ConnectingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, Properties.WATERLOGGED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		WorldView monde = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState état = this.getDefaultState().with(Properties.WATERLOGGED,
				context.getWorld().getFluidState(pos).getFluid() == Fluids.WATER);
		for (Direction côté : Direction.values()) {
			BlockPos voisin = pos.offset(côté);
			état = état.with(FACING_PROPERTIES.get(côté),
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
	protected BlockState getStateForNeighborUpdate(BlockState état, Direction côté,
			BlockState voisin, WorldAccess monde, BlockPos pos, BlockPos posVoisin) {
		if (état.get(Properties.WATERLOGGED)) {
			monde.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(monde));
		}
		return état.with(FACING_PROPERTIES.get(côté),
				seRaccorde(voisin, monde, posVoisin, côté));
	}

	/**
	 * Un câble se lie à un autre câble, et se termine dans une face pleine.
	 *
	 * <p>Le second cas est ce qui distingue un conduit d'une simple clôture : un
	 * tuyau qui bute contre une machine doit y entrer, pas s'arrêter à un
	 * cheveu du bloc.
	 */
	private static boolean seRaccorde(BlockState voisin, BlockView monde, BlockPos pos,
			Direction côté) {
		if (voisin.getBlock() instanceof CableBlock) {
			return true;
		}
		return voisin.isSideSolidFullSquare(monde, pos, côté.getOpposite());
	}

	/**
	 * Tourner ou retourner une structure doit emporter les raccords avec elle :
	 * sans cela, un câble tourné d'un quart de tour garde ses bras vers les
	 * anciens points cardinaux et ne rejoint plus rien.
	 */
	@Override
	protected BlockState rotate(BlockState état, net.minecraft.util.BlockRotation rotation) {
		return transposer(état, rotation::rotate);
	}

	@Override
	protected BlockState mirror(BlockState état, net.minecraft.util.BlockMirror miroir) {
		return transposer(état, miroir::apply);
	}

	/** Les quatre bras horizontaux, chacun reporté sur la direction transformée. */
	private static BlockState transposer(BlockState état,
	                                     java.util.function.UnaryOperator<Direction> vers) {
		BlockState transformé = état;
		for (Direction côté : Direction.Type.HORIZONTAL) {
			transformé = transformé.with(FACING_PROPERTIES.get(vers.apply(côté)),
					état.get(FACING_PROPERTIES.get(côté)));
		}
		return transformé;
	}

	@Override
	protected VoxelShape getCameraCollisionShape(BlockState état, BlockView monde,
			BlockPos pos, ShapeContext contexte) {
		return this.getOutlineShape(état, monde, pos, contexte);
	}

	@Override
	protected FluidState getFluidState(BlockState état) {
		return état.get(Properties.WATERLOGGED)
				? Fluids.WATER.getStill(false)
				: super.getFluidState(état);
	}
}
