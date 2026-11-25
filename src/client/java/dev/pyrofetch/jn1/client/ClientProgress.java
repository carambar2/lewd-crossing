package dev.pyrofetch.jn1.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Copie client des drapeaux, envoyée par le serveur : c'est eux qui décident de
 * ce que le terminal propose. L'affection n'y est plus, étant propre à chaque
 * compagne ; elle arrive avec l'écran qui la montre. Toute décision reste
 * serveur.
 */
public final class ClientProgress {
	private static Set<String> flags = Set.of();

	private ClientProgress() {
	}

	public static void update(List<String> newFlags) {
		flags = new LinkedHashSet<>(newFlags);
	}

	public static boolean hasFlag(String flag) {
		return flags.contains(flag);
	}

	public static Set<String> flags() {
		return flags;
	}

	public static void reset() {
		flags = Set.of();
	}
}
