package dev.pyrofetch.jn1.gloryhole;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionScenes;
import dev.pyrofetch.jn1.entity.JennyMode;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
	static void utiliser(ServerPlayer joueur, BlockPos bas, BlockState état) {
		ServerLevel monde = joueur.level();
		Direction normale = état.getValue(GloryWallBlock.FACING);
		// Le côté du joueur ; elle va de l'autre.
		Vec3 centre = Vec3.atBottomCenterOf(bas);
		Vec3 n = Vec3.atLowerCornerOf(normale.getUnitVec3i());
		Direction versElle = joueur.position().subtract(centre).dot(n) >= 0 ? normale.getOpposite() : normale;

		if (joueur.isShiftKeyDown()) {
			CompanionEntity là = installée(monde, bas);
			if (là != null) {
				là.stopScene();
				joueur.sendOverlayMessage(Component.translatable("block.jn1.glory_wall.stopped"));
			}
			return;
		}
		CompanionEntity fille = monde.getEntitiesOfClass(CompanionEntity.class,
						new AABB(bas).inflate(PORTÉE), f -> (!f.hasScene() || installée(monde, bas) == f)
								&& !f.isCuffed() && !f.isLeashed() && !f.estEnVeille() && f.peutQuitterSaPlace() && f.joueLesScènes())
				.stream()
				.min(Comparator.comparingDouble(f -> joueur.getUUID().equals(f.getCompanionUuid())
						? f.distanceToSqr(centre) - 10000 : f.distanceToSqr(centre)))
				.orElse(null);
		if (fille == null) {
			joueur.sendOverlayMessage(Component.translatable("block.jn1.glory_wall.nobody"));
			return;
		}
		String scène = installer(fille, bas, normale, versElle);
		if (scène == null) {
			joueur.sendOverlayMessage(Component.translatable("block.jn1.glory_wall.unknown", fille.getName()));
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
		Vec3 centre = Vec3.atBottomCenterOf(bas);
		Vec3 là = centre.add(Vec3.atLowerCornerOf(versElle.getUnitVec3i()).scale(ÉCART));
		float lacet = versElle.getOpposite().toYRot();
		fille.stopScene();
		fille.getNavigation().stop();
		fille.setMode(JennyMode.IDLE);
		fille.snapTo(là.x, bas.getY(), là.z, lacet, 0.0F);
		fille.setYBodyRot(lacet);
		fille.setYHeadRot(lacet);
		fille.setAnchorPos(fille.blockPosition());
		fille.playScene(scène);
		return scène;
	}

	/** La fille qui joue déjà une scène devant ce panneau, s'il y en a une. */
	@Nullable
	private static CompanionEntity installée(ServerLevel monde, BlockPos bas) {
		// Les siennes seulement : pas une danseuse voisine.
		return monde.getEntitiesOfClass(CompanionEntity.class, new AABB(bas).inflate(1.0, 0.5, 1.0),
				f -> f.hasScene() && !f.getScene().startsWith("jn1_"))
				.stream().findFirst().orElse(null);
	}
}
