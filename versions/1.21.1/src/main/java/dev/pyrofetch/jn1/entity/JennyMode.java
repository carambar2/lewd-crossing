package dev.pyrofetch.jn1.entity;

/**
 * Ce que Jenny est en train de faire. Synchronisé au client via le DataTracker
 * pour que le renderer choisisse la bonne animation.
 */
public enum JennyMode {
	/** Elle reste où elle est. Assise si elle s'ennuie. */
	IDLE,
	/** Elle suit son compagnon actuel. */
	FOLLOW,
	/** Elle vagabonde dans un rayon autour de son point d'ancrage. */
	WANDER,
	/** Elle marche devant et attend que le joueur suive. */
	LEAD;

	private static final JennyMode[] VALUES = values();

	public static JennyMode byId(int id) {
		return VALUES[Math.floorMod(id, VALUES.length)];
	}

	public int id() {
		return ordinal();
	}
}
