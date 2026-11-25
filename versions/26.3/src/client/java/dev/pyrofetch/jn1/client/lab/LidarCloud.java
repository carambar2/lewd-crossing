package dev.pyrofetch.jn1.client.lab;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Le relief que relève le lidar autour du joueur, pour sa vue 3D : un nuage de
 * points, un par bloc à l'air libre dans une sphère de {@link #RAYON} blocs.
 *
 * <p>Tout se fait côté client, dans le monde que le client a déjà : le serveur
 * n'envoie que les échos. Un relevé part à chaque impulsion et se fait par
 * tranches, quelques-unes par tick, pour ne pas figer une image ; le nuage
 * affiché reste le précédent jusqu'à ce que le nouveau soit complet.
 *
 * <p>Seuls les blocs qui touchent l'air comptent : l'intérieur d'un mur ne se
 * voit pas, et le compter noierait les formes. Un chunk que le client n'a pas
 * se lit comme de l'air — il n'en sort rien.
 */
public final class LidarCloud {
	/** La portée du relevé, en blocs : c'est aussi le bord de la vue 3D. */
	public static final int RAYON = 24;
	/** Au-delà, les plus lointains sont écartés : l'écran n'en montrerait qu'une tache. */
	private static final int POINTS_AU_PLUS = 4000;
	/** Des tranches est-ouest par tick : 49 tranches, un relevé en treize ticks. */
	private static final int TRANCHES_PAR_TICK = 4;

	private static long[] points = new long[0];
	private static LongArrayList enCours;
	private static BlockPos centre;
	private static int tranche;

	private LidarCloud() {
	}

	/** Le dernier relevé complet, en positions de blocs empaquetées ({@link BlockPos#asLong}). */
	public static long[] points() {
		return points;
	}

	/** Commence un relevé autour de ce point. Un relevé en cours est abandonné. */
	public static void relever(BlockPos où) {
		centre = où.immutable();
		enCours = new LongArrayList();
		tranche = 0;
	}

	/** Oublie tout : sorti du vide, le relief d'ici serait faux ailleurs. */
	public static void oublier() {
		points = new long[0];
		enCours = null;
	}

	/** Avance le relevé en cours de quelques tranches. */
	public static void tick(ClientLevel monde) {
		if (enCours == null) {
			return;
		}
		BlockPos.MutableBlockPos ici = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos voisin = new BlockPos.MutableBlockPos();
		int r2 = RAYON * RAYON;
		for (int n = 0; n < TRANCHES_PAR_TICK && tranche <= 2 * RAYON; n++, tranche++) {
			int dx = tranche - RAYON;
			for (int dy = -RAYON; dy <= RAYON; dy++) {
				for (int dz = -RAYON; dz <= RAYON; dz++) {
					if (dx * dx + dy * dy + dz * dz > r2) {
						continue;
					}
					ici.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					if (monde.getBlockState(ici).isAir() || !àLAir(monde, ici, voisin)) {
						continue;
					}
					enCours.add(ici.asLong());
				}
			}
		}
		if (tranche > 2 * RAYON) {
			terminer();
		}
	}

	private static boolean àLAir(ClientLevel monde, BlockPos pos, BlockPos.MutableBlockPos voisin) {
		for (Direction côté : Direction.values()) {
			if (monde.getBlockState(voisin.setWithOffset(pos, côté)).isAir()) {
				return true;
			}
		}
		return false;
	}

	private static void terminer() {
		long[] relevé = enCours.toLongArray();
		if (relevé.length > POINTS_AU_PLUS) {
			BlockPos c = centre;
			LongArrays.quickSort(relevé, (a, b) -> Double.compare(
					BlockPos.of(a).distSqr(c), BlockPos.of(b).distSqr(c)));
			relevé = java.util.Arrays.copyOf(relevé, POINTS_AU_PLUS);
		}
		points = relevé;
		enCours = null;
	}
}
