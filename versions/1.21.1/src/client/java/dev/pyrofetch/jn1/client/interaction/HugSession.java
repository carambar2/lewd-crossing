package dev.pyrofetch.jn1.client.interaction;

/**
 * Le temps que dure une étreinte, côté client.
 *
 * <p>Pendant ce temps le joueur ne commande plus ses jambes : une compagne qui
 * vient l'enlacer pendant qu'il s'éloigne en courant ne donnerait rien de bon.
 * La caméra reste libre, en revanche — la lui prendre aussi donne le mal de mer,
 * et il faut bien pouvoir regarder.
 */
public final class HugSession {
	private static int remaining;

	private HugSession() {
	}

	public static void begin(int ticks) {
		remaining = Math.max(0, ticks);
	}

	/** Appelé à chaque tick client ; rend la main de lui-même à la fin. */
	public static void tick() {
		if (remaining > 0) {
			remaining--;
		}
	}

	public static boolean isHeld() {
		return remaining > 0;
	}

	/** À la déconnexion : on ne garde pas un joueur figé d'une partie à l'autre. */
	public static void reset() {
		remaining = 0;
	}
}
