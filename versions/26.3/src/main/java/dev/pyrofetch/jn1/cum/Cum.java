package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ce qui arrive quand une scène « cu » arrive à son terme.
 *
 * <p>À chaque boucle achevée d'une scène dont le nom finit par {@code cu}
 * ({@code b_cu}, {@code d_cu}, {@code p_cu}), la fille prend un niveau de
 * traces. Ce qui retombe autour d'elle va dans l'extracteur le plus proche
 * s'il y en a un qui n'est pas plein ; sinon, des traces se posent au sol à ses
 * pieds : deux à quatre, groupées sous elle et un peu devant.
 */
public final class Cum {

	private Cum() {
	}

	public static void jaillir(CompanionEntity fille) {
		if (!(fille.level() instanceof ServerLevel monde)) {
			return;
		}
		fille.addTraces(1);
		bruit(monde, fille.blockPosition());
		// Ce qui retombe : deux à quatre traces, groupées sous elle et un peu
		// devant (là où est son bassin), au sol — ou un cran plus bas si elle est
		// sur une dalle.
		RandomSource hasard = fille.getRandom();
		double angle = Math.toRadians(fille.getVisualRotationYInDegrees());
		double devantX = -Math.sin(angle), devantZ = Math.cos(angle);
		int voulues = 2 + hasard.nextInt(3);
		int posées = 0;
		for (int essai = 0; essai < 16 && posées < voulues; essai++) {
			double x = fille.getX() + devantX * 0.45 + hasard.nextGaussian() * 0.55;
			double z = fille.getZ() + devantZ * 0.45 + hasard.nextGaussian() * 0.55;
			BlockPos là = BlockPos.containing(x, fille.getY() + 0.1, z);
			if (poserTrace(monde, là, Direction.DOWN) || poserTrace(monde, là.below(), Direction.DOWN)) {
				posées++;
			}
		}
	}

	/**
	 * Pose une trace dans la case {@code pos}, collée à sa face {@code côté}
	 * (le bloc porteur est de ce côté). Une case qui en porte déjà prend une
	 * face de plus. Rend faux si rien n'a pu être posé.
	 */
	public static boolean poserTrace(Level monde, BlockPos pos, Direction côté) {
		BlockState ici = monde.getBlockState(pos);
		MultifaceBlock trace = (MultifaceBlock) ModBlocks.CUM_TRACE;
		BlockState départ;
		if (ici.is(ModBlocks.CUM_TRACE)) {
			if (MultifaceBlock.hasFace(ici, côté)) {
				return false;
			}
			départ = ici;
		} else if (ici.isAir() || (ici.canBeReplaced() && ici.getFluidState().isEmpty())) {
			départ = ModBlocks.CUM_TRACE.defaultBlockState();
		} else {
			return false;
		}
		// Du frais sur une trace sèche la ravive : elle redevient fraîche.
		BlockState posé = trace.getStateForPlacement(départ.setValue(CumTraceBlock.SEC, false), monde, pos, côté);
		if (posé == null) {
			return false;
		}
		if (!monde.isClientSide()) {
			monde.setBlock(pos, posé, Block.UPDATE_ALL);
			bruit(monde, pos);
		}
		return true;
	}

	static void bruit(Level monde, BlockPos pos) {
		monde.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.3F);
	}
}
