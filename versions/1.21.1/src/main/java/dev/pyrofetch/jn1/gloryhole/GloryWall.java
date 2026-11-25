package dev.pyrofetch.jn1.gloryhole;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionScenes;
import dev.pyrofetch.jn1.entity.JennyMode;
import java.util.Comparator;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Ce que fait la cloison à trou.
 *
 * <p>La scène est celle d'une fille à genoux devant {@code steve} (les scènes
 * {@code b_}) : sa bouche y est à 13 pixels du sol — mesuré sur
 * {@code b_su}. Le trou est percé là, dans un panneau mince : il suffit de la
 * poser à {@link #ÉCART} du panneau, de l'autre côté du joueur, tournée vers lui, et
 * {@code steve} se trouve du côté du joueur. Le joueur prend sa place
 * ({@link CompanionEntity#joinScene}) : la caméra passe par les yeux du
 * mannequin, de son côté de la cloison.
 *
 * <p>La scène : {@code b_gh}, écrite pour la cloison (les mains à plat de part
 * et d'autre du trou), pour qui la connaît ; {@code b_su} sinon.
 */
public final class GloryWall {
	public static final String SCÈNE = "b_gh";
	public static final String SECOURS = "b_su";
	/**
	 * De sa position au milieu du panneau : son visage, au plus loin dans
	 * {@code b_su}, s'arrête contre lui ; {@code steve} est reculé d'autant dans
	 * {@code b_gh}, et seul {@code co} passe le trou (mesuré).
	 */
	public static final double ÉCART = 15.5 / 16.0;
	/** Portée dans laquelle on cherche une fille. */
	private static final double PORTÉE = 12.0;

	private GloryWall() {
	}

	/** Le clic d'un joueur, sur la moitié basse {@code bas}. */
	static void utiliser(ServerPlayerEntity joueur, BlockPos bas, BlockState état) {
		ServerWorld monde = joueur.getServerWorld();
		Direction normale = état.get(GloryWallBlock.FACING);
		// Le côté du joueur ; elle va de l'autre.
		Vec3d centre = Vec3d.ofBottomCenter(bas);
		Vec3d n = Vec3d.of(normale.getVector());
		Direction versElle = joueur.getPos().subtract(centre).dotProduct(n) >= 0 ? normale.getOpposite() : normale;

		if (joueur.isSneaking()) {
			CompanionEntity là = installée(monde, bas);
			if (là != null) {
				là.stopScene();
				joueur.sendMessage(Text.translatable("block.jn1.glory_wall.stopped"), true);
			}
			return;
		}
		CompanionEntity fille = monde.getEntitiesByClass(CompanionEntity.class,
						new Box(bas).expand(PORTÉE), f -> (!f.hasScene() || installée(monde, bas) == f)
								&& !f.isCuffed() && !f.isLeashed() && !f.estEnVeille() && f.peutQuitterSaPlace() && f.joueLesScènes())
				.stream()
				.min(Comparator.comparingDouble(f -> joueur.getUuid().equals(f.getCompanionUuid())
						? f.squaredDistanceTo(centre) - 10000 : f.squaredDistanceTo(centre)))
				.orElse(null);
		if (fille == null) {
			joueur.sendMessage(Text.translatable("block.jn1.glory_wall.nobody"), true);
			return;
		}
		String scène = installer(fille, bas, normale, versElle);
		if (scène == null) {
			joueur.sendMessage(Text.translatable("block.jn1.glory_wall.unknown", fille.getName()), true);
			return;
		}
		fille.setSceneOwner(joueur);
		fille.joinScene(joueur);
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "cloison");
	}

	/**
	 * Pose la fille côté {@code versElle} du panneau, à genoux face au trou, et
	 * lui fait jouer la scène. Rend le nom de la scène, ou {@code null} si elle
	 * n'en connaît aucune. Aussi appelé par la commande de test.
	 */
	@Nullable
	public static String installer(CompanionEntity fille, BlockPos bas, Direction normale, Direction versElle) {
		String scène = CompanionScenes.knows(fille.kind(), SCÈNE) ? SCÈNE
				: CompanionScenes.knows(fille.kind(), SECOURS) ? SECOURS : null;
		if (scène == null) {
			return null;
		}
		Vec3d centre = Vec3d.ofBottomCenter(bas);
		Vec3d là = centre.add(Vec3d.of(versElle.getVector()).multiply(ÉCART));
		float lacet = versElle.getOpposite().asRotation();
		fille.stopScene();
		fille.getNavigation().stop();
		fille.setMode(JennyMode.IDLE);
		fille.refreshPositionAndAngles(là.x, bas.getY(), là.z, lacet, 0.0F);
		fille.setBodyYaw(lacet);
		fille.setHeadYaw(lacet);
		fille.setAnchorPos(fille.getBlockPos());
		fille.playScene(scène);
		return scène;
	}

	/** La fille qui joue déjà une scène devant ce panneau, s'il y en a une. */
	@Nullable
	private static CompanionEntity installée(ServerWorld monde, BlockPos bas) {
		// Les siennes seulement : pas une danseuse voisine.
		return monde.getEntitiesByClass(CompanionEntity.class, new Box(bas).expand(1.0, 0.5, 1.0),
				f -> f.hasScene() && !f.getScene().startsWith("jn1_"))
				.stream().findFirst().orElse(null);
	}
}
