package dev.pyrofetch.jn1.morph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pyrofetch.jn1.entity.CompanionKind;
import org.jetbrains.annotations.Nullable;

/**
 * En qui le joueur est transformé.
 *
 * <p>Permanente : elle survit à la déconnexion et à la mort, et ne cesse que
 * si le joueur l'annule lui-même.
 *
 * @param kind      l'identifiant de la compagne ({@link CompanionKind#id()}),
 *                  vide s'il est lui-même
 * @param outfit    la tenue, par le nom de son modèle ; vide pour celle par
 *                  défaut
 * @param pending   il a bu la potion et n'a pas encore choisi : c'est ce qui
 *                  l'autorise à se transformer une première fois
 * @param boobs     niveau de la potion de poitrine ({@link dev.pyrofetch.jn1.shape.Shapes})
 * @param booty     niveau de la potion de croupe ; tous deux gardés d'une
 *                  fille à l'autre et quand il redevient lui-même
 */
public record MorphState(String kind, String outfit, boolean pending, int boobs, int booty) {
	public static final MorphState NONE = new MorphState("", "", false);

	public MorphState(String kind, String outfit, boolean pending) {
		this(kind, outfit, pending, 0, 0);
	}

	/** La même apparence, avec d'autres formes. */
	public MorphState withShape(int boobs, int booty) {
		return new MorphState(this.kind, this.outfit, this.pending, boobs, booty);
	}

	/** Une autre apparence, aux mêmes formes. */
	public MorphState with(String kind, String outfit, boolean pending) {
		return new MorphState(kind, outfit, pending, this.boobs, this.booty);
	}

	/**
	 * « alternate » est lu pour les sauvegardes d'avant 0.40, où il n'y avait
	 * qu'une seconde tenue ; il n'est plus écrit.
	 */
	public static final Codec<MorphState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("kind", "").forGetter(MorphState::kind),
			Codec.STRING.optionalFieldOf("outfit", "").forGetter(MorphState::outfit),
			Codec.BOOL.optionalFieldOf("alternate", false).forGetter(état -> false),
			Codec.BOOL.optionalFieldOf("pending", false).forGetter(MorphState::pending),
			Codec.INT.optionalFieldOf("boobs", 0).forGetter(MorphState::boobs),
			Codec.INT.optionalFieldOf("booty", 0).forGetter(MorphState::booty)
	).apply(instance, MorphState::lire));

	private static MorphState lire(String kind, String outfit, boolean alternate, boolean pending, int boobs, int booty) {
		CompanionKind compagne = CompanionKind.byId(kind);
		if (outfit.isEmpty() && alternate && compagne != null) {
			outfit = compagne.legacyAlternate();
		}
		return new MorphState(kind, outfit, pending, boobs, booty);
	}

	public boolean morphed() {
		return this.companion() != null;
	}

	/** La tenue portée : toujours l'une de celles de sa compagne. */
	public String currentOutfit() {
		CompanionKind compagne = this.companion();
		return compagne == null ? "" : compagne.outfit(this.outfit);
	}

	/** La compagne dont il a pris l'apparence, ou {@code null}. */
	@Nullable
	public CompanionKind companion() {
		return CompanionKind.byId(this.kind);
	}
}
