package dev.pyrofetch.jn1.dialogue;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Ce qui a provoqué l'évaluation du dialogue. */
public enum DialogueTrigger implements StringRepresentable {
	/** Le tick d'ambiance : elle parle d'elle-même. */
	AMBIENT("ambient"),
	/** Le joueur a cliqué sur elle. */
	INTERACT("interact"),
	/** Le joueur vient de lui donner un objet. */
	GIFT("gift"),
	/** Déclenché par le mod : un câlin, le fouet, une fin de scène… (champ {@code event}). */
	EVENT("event"),
	/** Une interaction de son bouton « Talk » (champs {@code talk} et {@code outcome}). */
	TALK("talk"),
	/** N'importe lequel des cas ci-dessus. Valeur par défaut d'une entrée. */
	ANY("any");

	public static final Codec<DialogueTrigger> CODEC = StringRepresentable.fromEnum(DialogueTrigger::values);

	private final String name;

	DialogueTrigger(String name) {
		this.name = name;
	}

	/** Une entrée déclarée {@code ANY} accepte tout ; sinon il faut l'égalité. */
	public boolean accepts(DialogueTrigger actual) {
		return this == ANY || this == actual;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
