package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.rift.Contamination;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * La douche du sas : une pomme fixée au plafond, qui lave la contamination.
 *
 * <p>Un clic dessous, et l'on ressort propre. C'est le seul remède, avec le lait
 * et le temps (voir {@link Contamination}). Les filles qui se tiennent sous la
 * pomme y perdent leurs traces de cum : c'est le seul moyen de les retirer.
 */
public class DeconShowerBlock extends Block {
	public static final MapCodec<DeconShowerBlock> CODEC = createCodec(DeconShowerBlock::new);

	/** La tuyauterie et la pomme, accrochées sous le bloc du dessus. */
	private static final VoxelShape FORME = VoxelShapes.union(
			Block.createCuboidShape(7, 12, 7, 9, 16, 9),
			Block.createCuboidShape(4, 10, 4, 12, 12, 12));

	public DeconShowerBlock(Settings settings) {
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
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world instanceof ServerWorld monde)) {
			return ActionResult.SUCCESS;
		}
		boolean lavé = Contamination.laver(player);
		// Les filles sous la pomme ressortent sans traces.
		for (dev.pyrofetch.jn1.entity.CompanionEntity fille : monde.getEntitiesByClass(
				dev.pyrofetch.jn1.entity.CompanionEntity.class,
				new net.minecraft.util.math.Box(pos.down(3)).union(new net.minecraft.util.math.Box(pos)).expand(0.4, 0, 0.4),
				fille -> fille.getTraces() > 0)) {
			fille.setTraces(0);
			lavé = true;
		}
		monde.spawnParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() - 0.8, pos.getZ() + 0.5,
				60, 0.35, 0.9, 0.35, 0.0);
		monde.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() - 1.6, pos.getZ() + 0.5,
				8, 0.4, 0.3, 0.4, 0.01);
		monde.playSound(null, pos, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.BLOCKS, 1.0F, 1.6F);
		monde.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.3F, 1.8F);
		player.sendMessage(Text.translatable(lavé
				? "block.jn1.decon_shower.washed"
				: "block.jn1.decon_shower.clean"), true);
		return ActionResult.CONSUME;
	}

	/** Une goutte de temps en temps : on la reconnaît de loin. */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(4) == 0) {
			world.addParticle(ParticleTypes.DRIPPING_WATER,
					pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
		}
	}
}
