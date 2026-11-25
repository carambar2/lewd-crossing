package dev.pyrofetch.jn1.onlyfaps;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.photo.PhotoPose;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Qui est sur une photo : le serveur le relève lui-même, à la réception, depuis
 * la pose que le client a jointe ({@link PhotoPose}) — pas d'après ce que le
 * client prétendrait.
 *
 * <p>Une fille y est si l'un de trois points de son corps (pieds, buste,
 * tête) tombe dans le cadre et qu'aucun bloc ne le cache. La pose est relevée
 * à l'instant du rendu, les filles à la réception, quelques ticks plus tard :
 * assez près pour un portrait, pas pour une course.
 */
public final class Sujets {
	/** Au-delà, une fille n'est plus qu'un point sur la photo. */
	private static final double PORTÉE = 40.0;
	/** La pose ne vaut que près du photographe (le trépied compris). */
	private static final double ÉCART_MAX = 64.0;

	private Sujets() {
	}

	public static List<Sujet> relever(ServerPlayerEntity photographe, PhotoPose pose) {
		if (pose == null || pose.vide()
				|| pose.position().squaredDistanceTo(photographe.getPos()) > ÉCART_MAX * ÉCART_MAX) {
			return new ArrayList<>();
		}
		return relever(photographe.getServerWorld(), pose);
	}

	/**
	 * Le relevé lui-même, sans contrôle du photographe : le viseur du client
	 * s'en sert pour annoncer qui sera sur la photo, avec le même calcul.
	 */
	public static List<Sujet> relever(World monde, PhotoPose pose) {
		List<Sujet> sujets = new ArrayList<>();
		if (pose == null || pose.vide()) {
			return sujets;
		}
		Vec3d œil = pose.position();
		Vec3d avant = Vec3d.fromPolar(pose.tangage(), pose.lacet());
		Vec3d droite = avant.crossProduct(new Vec3d(0, 1, 0));
		if (droite.lengthSquared() < 1.0E-6) {
			// Droit vers le haut ou le bas : n'importe quelle droite fait l'affaire.
			droite = new Vec3d(1, 0, 0);
		}
		droite = droite.normalize();
		Vec3d haut = droite.crossProduct(avant).normalize();

		Box zone = new Box(œil, œil).expand(PORTÉE);
		for (CompanionEntity fille : monde.getEntitiesByClass(CompanionEntity.class, zone, CompanionEntity::isAlive)) {
			Box boîte = fille.getBoundingBox();
			Vec3d centre = boîte.getCenter();
			Vec3d[] points = {
					new Vec3d(centre.x, boîte.minY + 0.15, centre.z),
					centre,
					fille.getEyePos()};
			boolean vue = false;
			for (Vec3d point : points) {
				if (dansLeCadre(point.subtract(œil), avant, droite, haut, pose) && àDécouvert(monde, œil, point)) {
					vue = true;
					break;
				}
			}
			if (!vue) {
				continue;
			}
			double profondeur = Math.max(0.1, centre.subtract(œil).dotProduct(avant));
			float cadrage = (float) Math.min(1.5, boîte.getLengthY() / (2 * profondeur * pose.ouvertureV()));
			var sorte = fille.kind();
			String tenue = fille.getOutfit();
			sujets.add(new Sujet(sorte.id(), tenue, Math.max(0, sorte.outfits().indexOf(tenue)), sorte.outfits().size(),
					fille.getScene(), fille.getTraces(), fille.isCuffed(), cadrage));
		}
		// La mieux cadrée d'abord : c'est elle, le sujet.
		sujets.sort(Comparator.comparingDouble(Sujet::cadrage).reversed());
		return sujets;
	}

	private static boolean dansLeCadre(Vec3d d, Vec3d avant, Vec3d droite, Vec3d haut, PhotoPose pose) {
		double z = d.dotProduct(avant);
		if (z < 0.2) {
			return false;
		}
		return Math.abs(d.dotProduct(droite)) / z <= pose.ouvertureH()
				&& Math.abs(d.dotProduct(haut)) / z <= pose.ouvertureV();
	}

	private static boolean àDécouvert(World monde, Vec3d œil, Vec3d point) {
		var touché = monde.raycast(new RaycastContext(œil, point, RaycastContext.ShapeType.VISUAL,
				RaycastContext.FluidHandling.NONE, ShapeContext.absent()));
		return touché.getType() == HitResult.Type.MISS
				|| touché.getPos().squaredDistanceTo(œil) >= œil.squaredDistanceTo(point) - 0.05;
	}
}
