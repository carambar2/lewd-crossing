package dev.pyrofetch.jn1.morph;

import dev.pyrofetch.jn1.entity.CompanionKind;
import org.jetbrains.annotations.Nullable;

/**
 * Les gestes qu'un joueur transformé peut faire, vus de tous.
 *
 * <p>Chacun emprunte le rôle correspondant des compagnes : une fille qui ne
 * sait pas le tenir ({@link CompanionKind#animation} vide) ne le propose pas.
 * Un geste cesse au bout de sa durée, ou dès que le joueur se déplace ;
 * s'asseoir n'a pas de fin d'elle-même.
 */
public enum MorphGesture {
	WAVE(CompanionKind.Role.WAVE, 70),
	GREET(CompanionKind.Role.GREET, 105),
	DANCE(CompanionKind.Role.DANCE, 215),
	SIT(CompanionKind.Role.SIT, -1);

	private final CompanionKind.Role role;
	private final int durationTicks;

	MorphGesture(CompanionKind.Role role, int durationTicks) {
		this.role = role;
		this.durationTicks = durationTicks;
	}

	public CompanionKind.Role role() {
		return this.role;
	}

	/** Durée en ticks ; négative pour un geste qui dure tant qu'on ne bouge pas. */
	public int durationTicks() {
		return this.durationTicks;
	}

	public boolean loops() {
		return this.durationTicks < 0;
	}

	public String id() {
		return this.name().toLowerCase(java.util.Locale.ROOT);
	}

	@Nullable
	public static MorphGesture byId(String id) {
		for (MorphGesture geste : values()) {
			if (geste.id().equals(id)) {
				return geste;
			}
		}
		return null;
	}
}
