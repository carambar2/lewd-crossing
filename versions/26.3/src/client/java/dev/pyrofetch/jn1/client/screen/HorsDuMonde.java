package dev.pyrofetch.jn1.client.screen;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.Entity;

/**
 * Les copies d'entités dessinées hors du monde (portraits, aperçus du terminal,
 * Gaster dans sa boutique). En 26.3, leur rendu lit l'identifiant de l'entité
 * (graine des objets tenus, état d'animation de GeckoLib) : une entité jamais
 * ajoutée au monde n'en a pas, et le jeu plante. Chacune reçoit le sien,
 * négatif, pour ne jamais croiser celui d'une vraie entité.
 */
public final class HorsDuMonde {
	private static final AtomicInteger PROCHAIN = new AtomicInteger(-1_000_000);

	private HorsDuMonde() {
	}

	public static <T extends Entity> T identifier(T entité) {
		entité.setId(PROCHAIN.getAndDecrement());
		return entité;
	}
}
