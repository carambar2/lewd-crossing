package dev.pyrofetch.jn1.rift;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Le projecteur holographique : un disque bas, cerclé de lumière, qui fait
 * paraître Aura, en veille, au-dessus de lui ({@link AuraProjectorBlockEntity}).
 *
 * <p>C'est lui qu'on pose dans le bâtiment de la faille, là où elle doit
 * attendre qu'on la trouve.
 */
public class AuraProjectorBlock extends Block implements BlockEntityProvider {
	public static final MapCodec<AuraProjectorBlock> CODEC = createCodec(AuraProjectorBlock::new);
	/** La hauteur du disque, en blocs : trois pixels. */
	public static final double HAUTEUR = 3.0 / 16.0;
	private static final VoxelShape FORME = Block.createCuboidShape(1, 0, 1, 15, 3, 15);

	public AuraProjectorBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return FORME;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new AuraProjectorBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (world.isClient() || type != ModBlockEntities.AURA_PROJECTOR) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<AuraProjectorBlockEntity>) AuraProjectorBlockEntity::tick;
	}

	/** Un grain de lumière monte de temps en temps du disque. */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(3) != 0) {
			return;
		}
		world.addParticle(new DustParticleEffect(new Vector3f(0.45F, 0.9F, 1.0F), 0.5F),
				pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + HAUTEUR + 0.02,
				pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.03, 0.0);
	}
}
