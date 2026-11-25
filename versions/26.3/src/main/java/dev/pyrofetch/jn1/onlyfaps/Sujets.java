package dev.pyrofetch.jn1.onlyfaps;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.photo.PhotoPose;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

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

	public static List<Sujet> relever(ServerPlayer photographe, PhotoPose pose) {
		if (pose == null || pose.vide()
				|| pose.position().distanceToSqr(photographe.position()) > ÉCART_MAX * ÉCART_MAX) {
			return new ArrayList<>();
		}
		return relever(photographe.level(), pose);
	}

	/**
	 * Le relevé lui-même, sans contrôle du photographe : le viseur du client
	 * s'en sert pour annoncer qui sera sur la photo, avec le même calcul.
	 */
	public static List<Sujet> relever(Level monde, PhotoPose pose) {
		List<Sujet> sujets = new ArrayList<>();
		if (pose == null || pose.vide()) {
			return sujets;
		}
		Vec3 œil = pose.position();
		Vec3 avant = Vec3.directionFromRotation(pose.tangage(), pose.lacet());
		Vec3 droite = avant.cross(new Vec3(0, 1, 0));
		if (droite.lengthSqr() < 1.0E-6) {
			// Droit vers le haut ou le bas : n'importe quelle droite fait l'affaire.
			droite = new Vec3(1, 0, 0);
		}
		droite = droite.normalize();
		Vec3 haut = droite.cross(avant).normalize();

		AABB zone = new AABB(œil, œil).inflate(PORTÉE);
		for (CompanionEntity fille : monde.getEntitiesOfClass(CompanionEntity.class, zone, CompanionEntity::isAlive)) {
			AABB boîte = fille.getBoundingBox();
			Vec3 centre = boîte.getCenter();
			Vec3[] points = {
					new Vec3(centre.x, boîte.minY + 0.15, centre.z),
					centre,
					fille.getEyePosition()};
			boolean vue = false;
			for (Vec3 point : points) {
				if (dansLeCadre(point.subtract(œil), avant, droite, haut, pose) && àDécouvert(monde, œil, point)) {
					vue = true;
					break;
				}
			}
			if (!vue) {
				continue;
			}
			double profondeur = Math.max(0.1, centre.subtract(œil).dot(avant));
			float cadrage = (float) Math.min(1.5, boîte.getYsize() / (2 * profondeur * pose.ouvertureV()));
			var sorte = fille.kind();
			String tenue = fille.getOutfit();
			sujets.add(new Sujet(sorte.id(), tenue, Math.max(0, sorte.outfits().indexOf(tenue)), sorte.outfits().size(),
					fille.getScene(), fille.getTraces(), fille.isCuffed(), cadrage));
		}
		// La mieux cadrée d'abord : c'est elle, le sujet.
		sujets.sort(Comparator.comparingDouble(Sujet::cadrage).reversed());
		return sujets;
	}

	private static boolean dansLeCadre(Vec3 d, Vec3 avant, Vec3 droite, Vec3 haut, PhotoPose pose) {
		double z = d.dot(avant);
		if (z < 0.2) {
			return false;
		}
		return Math.abs(d.dot(droite)) / z <= pose.ouvertureH()
				&& Math.abs(d.dot(haut)) / z <= pose.ouvertureV();
	}

	private static boolean àDécouvert(Level monde, Vec3 œil, Vec3 point) {
		var touché = monde.clip(new ClipContext(œil, point, ClipContext.Block.VISUAL,
				ClipContext.Fluid.NONE, CollisionContext.empty()));
		return touché.getType() == HitResult.Type.MISS
				|| touché.getLocation().distanceToSqr(œil) >= œil.distanceToSqr(point) - 0.05;
	}
}
