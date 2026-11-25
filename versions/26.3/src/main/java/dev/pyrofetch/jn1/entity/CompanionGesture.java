package dev.pyrofetch.jn1.entity;

/**
 * Gestes ponctuels, joués par-dessus la posture.
 *
 * <p>Chacun correspond à une animation du modèle et se joue pendant une durée
 * fixe, après quoi elle revient à {@link #NONE}.
 */
public enum CompanionGesture {
	/** Rien : le contrôleur rend la main à la posture. */
	NONE(null, 0),
	/** Elle salue. Première rencontre seulement : l'animation ne boucle pas. */
	WAVE(CompanionKind.Role.WAVE, 70),
	/** Retrouvailles : elle fait signe de venir quand on revient après une absence. */
	GREET(CompanionKind.Role.GREET, 105),
	/** On lui donne quelque chose : elle l'examine avant de le ranger. */
	RECEIVE(CompanionKind.Role.RECEIVE, 47),
	/** On l'a frappée. Elle ne peut pas être blessée, alors elle rend le coup. */
	SWIPE_A(CompanionKind.Role.SWIPE_A, 10),
	SWIPE_B(CompanionKind.Role.SWIPE_B, 14),
	SWIPE_C(CompanionKind.Role.SWIPE_C, 14),
	/** Confiance au maximum : elle se lâche. */
	DANCE(CompanionKind.Role.DANCE, 215),
	/** Le câlin, débloqué à l'affection maximale. */
	HUG(CompanionKind.Role.HUG, 152);

	private static final CompanionGesture[] VALUES = values();
	private static final CompanionGesture[] SWIPES = {SWIPE_A, SWIPE_B, SWIPE_C};

	private final CompanionKind.Role role;
	private final int durationTicks;

	CompanionGesture(CompanionKind.Role role, int durationTicks) {
		this.role = role;
		this.durationTicks = durationTicks;
	}

	/** Le rôle à demander à la compagne ; toutes ne savent pas le tenir. */
	public CompanionKind.Role role() {
		return this.role;
	}

	public int durationTicks() {
		return this.durationTicks;
	}

	public static CompanionGesture byId(int id) {
		return VALUES[Math.floorMod(id, VALUES.length)];
	}

	public static CompanionGesture randomSwipe(net.minecraft.util.RandomSource random) {
		return SWIPES[random.nextInt(SWIPES.length)];
	}

	public int id() {
		return this.ordinal();
	}
}
