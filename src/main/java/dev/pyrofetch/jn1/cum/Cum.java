package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

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
		if (!(fille.getWorld() instanceof ServerWorld monde)) {
			return;
		}
		fille.addTraces(1);
		bruit(monde, fille.getBlockPos());
		// Ce qui retombe : deux à quatre traces, groupées sous elle et un peu
		// devant (là où est son bassin), au sol — ou un cran plus bas si elle est
		// sur une dalle.
		Random hasard = fille.getRandom();
		double angle = Math.toRadians(fille.getBodyYaw());
		double devantX = -Math.sin(angle), devantZ = Math.cos(angle);
		int voulues = 2 + hasard.nextInt(3);
		int posées = 0;
		for (int essai = 0; essai < 16 && posées < voulues; essai++) {
			double x = fille.getX() + devantX * 0.45 + hasard.nextGaussian() * 0.55;
			double z = fille.getZ() + devantZ * 0.45 + hasard.nextGaussian() * 0.55;
			BlockPos là = BlockPos.ofFloored(x, fille.getY() + 0.1, z);
			if (poserTrace(monde, là, Direction.DOWN) || poserTrace(monde, là.down(), Direction.DOWN)) {
				posées++;
			}
		}
	}

	/**
	 * Pose une trace dans la case {@code pos}, collée à sa face {@code côté}
	 * (le bloc porteur est de ce côté). Une case qui en porte déjà prend une
	 * face de plus. Rend faux si rien n'a pu être posé.
	 */
	public static boolean poserTrace(World monde, BlockPos pos, Direction côté) {
		BlockState ici = monde.getBlockState(pos);
		MultifaceGrowthBlock trace = (MultifaceGrowthBlock) ModBlocks.CUM_TRACE;
		BlockState départ;
		if (ici.isOf(ModBlocks.CUM_TRACE)) {
			if (MultifaceGrowthBlock.hasDirection(ici, côté)) {
				return false;
			}
			départ = ici;
		} else if (ici.isAir() || (ici.isReplaceable() && ici.getFluidState().isEmpty())) {
			départ = ModBlocks.CUM_TRACE.getDefaultState();
		} else {
			return false;
		}
		// Du frais sur une trace sèche la ravive : elle redevient fraîche.
		BlockState posé = trace.withDirection(départ.with(CumTraceBlock.SEC, false), monde, pos, côté);
		if (posé == null) {
			return false;
		}
		if (!monde.isClient()) {
			monde.setBlockState(pos, posé, Block.NOTIFY_ALL);
			bruit(monde, pos);
		}
		return true;
	}

	static void bruit(World monde, BlockPos pos) {
		monde.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.6F, 1.3F);
	}
}
