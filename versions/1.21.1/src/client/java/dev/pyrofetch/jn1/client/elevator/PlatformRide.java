package dev.pyrofetch.jn1.client.elevator;

import dev.pyrofetch.jn1.lift.PlatformEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Le joueur debout sur une plateforme en route, porté par elle.
 *
 * <p>C'est le client qui fait bouger son joueur : le serveur ne saurait pas le
 * déplacer d'un tick à l'autre sans le faire trembler. À la fin de chaque pas
 * du joueur, s'il se tient sur la plateforme — ou juste au-dessus, ou juste
 * dedans, elle ayant bougé —, on le repose dessus, à la hauteur qu'elle aura à
 * la fin de ce tick ({@link PlatformEntity#dessusEnFinDeTick}). Un saut, qui
 * l'emporte plus haut, n'est pas retenu ; un joueur qui vole n'est pas porté.
 */
public final class PlatformRide {
	private static final double DESSOUS = 0.7;
	private static final double DESSUS = 0.3;

	private PlatformRide() {
	}

	public static void porter(ClientPlayerEntity joueur) {
		if (joueur.getAbilities().flying || joueur.hasVehicle() || joueur.isSpectator()) {
			return;
		}
		PlatformEntity plateforme = PlatformEntity.sous(joueur);
		if (plateforme == null) {
			return;
		}
		double dessus = plateforme.dessusEnFinDeTick();
		double écart = joueur.getY() - dessus;
		Vec3d vitesse = joueur.getVelocity();
		if (écart < -DESSOUS || écart > DESSUS || vitesse.y > 0.15) {
			return;
		}
		joueur.setPosition(joueur.getX(), dessus, joueur.getZ());
		joueur.setVelocity(vitesse.x, 0.0, vitesse.z);
		joueur.setOnGround(true);
		joueur.fallDistance = 0.0F;
	}
}
