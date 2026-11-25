package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le projecteur holographique : un disque bas, cerclé de lumière, qui fait
 * paraître Aura, en veille, au-dessus de lui ({@link AuraProjectorBlockEntity}).
 *
 * <p>C'est lui qu'on pose dans le bâtiment de la faille, là où elle doit
 * attendre qu'on la trouve.
 */
public class AuraProjectorBlock extends Block implements EntityBlock {
	/** La hauteur du disque, en blocs : trois pixels. */
	public static final double HAUTEUR = 3.0 / 16.0;
	private static final VoxelShape FORME = Block.box(1, 0, 1, 15, 3, 15);

	public AuraProjectorBlock(Properties settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return FORME;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AuraProjectorBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		if (world.isClientSide() || type != ModBlockEntities.AURA_PROJECTOR) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<AuraProjectorBlockEntity>) AuraProjectorBlockEntity::tick;
	}

	/** Un grain de lumière monte de temps en temps du disque. */
	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) {
			return;
		}
		world.addParticle(new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1.0F, 0.45F, 0.9F, 1.0F), 0.5F),
				pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + HAUTEUR + 0.02,
				pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.03, 0.0);
	}
}
