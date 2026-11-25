package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.rift.Contamination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La douche du sas : une pomme fixée au plafond, qui lave la contamination.
 *
 * <p>Un clic dessous, et l'on ressort propre. C'est le seul remède, avec le lait
 * et le temps (voir {@link Contamination}). Les filles qui se tiennent sous la
 * pomme y perdent leurs traces de cum : c'est le seul moyen de les retirer.
 */
public class DeconShowerBlock extends Block {

	/** La tuyauterie et la pomme, accrochées sous le bloc du dessus. */
	private static final VoxelShape FORME = Shapes.or(
			Block.box(7, 12, 7, 9, 16, 9),
			Block.box(4, 10, 4, 12, 12, 12));

	public DeconShowerBlock(Properties settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return FORME;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(world instanceof ServerLevel monde)) {
			return InteractionResult.SUCCESS;
		}
		boolean lavé = Contamination.laver(player);
		// Les filles sous la pomme ressortent sans traces.
		for (dev.pyrofetch.jn1.entity.CompanionEntity fille : monde.getEntitiesOfClass(
				dev.pyrofetch.jn1.entity.CompanionEntity.class,
				new net.minecraft.world.phys.AABB(pos.below(3)).minmax(new net.minecraft.world.phys.AABB(pos)).inflate(0.4, 0, 0.4),
				fille -> fille.getTraces() > 0)) {
			fille.setTraces(0);
			lavé = true;
		}
		monde.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() - 0.8, pos.getZ() + 0.5,
				60, 0.35, 0.9, 0.35, 0.0);
		monde.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() - 1.6, pos.getZ() + 0.5,
				8, 0.4, 0.3, 0.4, 0.01);
		monde.playSound(null, pos, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.BLOCKS, 1.0F, 1.6F);
		monde.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.8F);
		player.sendOverlayMessage(Component.translatable(lavé
				? "block.jn1.decon_shower.washed"
				: "block.jn1.decon_shower.clean"));
		return InteractionResult.CONSUME;
	}

	/** Une goutte de temps en temps : on la reconnaît de loin. */
	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			world.addParticle(ParticleTypes.DRIPPING_WATER,
					pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
		}
	}
}
