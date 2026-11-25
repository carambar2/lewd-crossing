package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.lab.LabDimension;
import dev.pyrofetch.jn1.lab.VoidFall;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Le passage d'une faille au vide.
 *
 * <p>Le vide double la surface : on y débouche aux mêmes x et z que la faille,
 * en plein vide, au-dessus des laboratoires. Sans la tenue, on tombe, et le
 * fond recrache au bout de quelques secondes ({@link VoidFall}). Avec, on nage
 * — et c'est le lidar qui dit vers où, puisque le laboratoire le plus proche
 * est celui de la maison la plus proche, qui peut être loin.
 *
 * <p>Le retour est noté à trois pas de la faille, sur le sol, et pas dans la
 * faille elle-même — on y retomberait en ressortant.
 */
public final class RiftTravel {
	/**
	 * La hauteur où l'on débouche : au-dessus des laboratoires, qui montent de
	 * 40 à 61, pour ne jamais apparaître dans l'un d'eux.
	 */
	private static final double HAUTEUR = 68.0;
	private static final double DISPERSION = 8.0;
	/** À combien de pas de la faille on est rendu. */
	private static final int ÉCART_DU_RETOUR = 3;

	private RiftTravel() {
	}

	public static void traverser(ServerPlayer joueur, RiftEntity faille) {
		ServerLevel monde = joueur.level();
		RandomSource alea = joueur.getRandom();

		BlockPos retour = retour(monde, faille, alea);
		monde.playSound(null, faille.blockPosition(), ModSounds.RIFT_ENTER, SoundSource.PLAYERS, 1.0F, 1.0F);

		Vec3 arrivée = new Vec3(faille.getX(), HAUTEUR + alea.nextDouble() * DISPERSION, faille.getZ());

		ModAttachments.setRiftCooldown(joueur, VoidFall.RÉPIT);
		LabDimension.enterThroughRift(joueur, arrivée, joueur.getYRot(),
				monde.dimension().identifier().toString(), retour);
		joueur.level().playSound(null, joueur.blockPosition(), ModSounds.RIFT_ENTER,
				SoundSource.PLAYERS, 0.8F, 0.7F);
	}

	/**
	 * Où ressortir : à trois pas de la faille, sur un sol où l'on tient debout,
	 * dans une direction au hasard.
	 *
	 * <p>On cherche à la hauteur de la faille, pas en surface : celles des sites
	 * sont sous terre, et le relevé du terrain rendait le toit du bunker. Si rien
	 * ne convient, on se rabat sur ses pieds à elle — le répit couvrira le temps
	 * de s'en écarter.
	 */
	private static BlockPos retour(ServerLevel monde, RiftEntity faille, RandomSource alea) {
		BlockPos base = faille.blockPosition();
		Direction premier = Direction.Plane.HORIZONTAL.getRandomDirection(alea);
		for (int i = 0; i < 4; i++) {
			Direction sens = Direction.from2DDataValue(premier.get2DDataValue() + i);
			BlockPos colonne = base.relative(sens, ÉCART_DU_RETOUR);
			for (int dy : new int[] {0, 1, -1, 2, -2}) {
				BlockPos pieds = colonne.above(dy);
				if (monde.getBlockState(pieds.below()).isRedstoneConductor(monde, pieds.below())
						&& monde.isEmptyBlock(pieds) && monde.isEmptyBlock(pieds.above())) {
					return pieds;
				}
			}
		}
		return base;
	}
}
