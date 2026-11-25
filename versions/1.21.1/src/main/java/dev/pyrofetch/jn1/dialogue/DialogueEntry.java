package dev.pyrofetch.jn1.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Une entrée de dialogue telle qu'écrite dans un datapack.
 *
 * @param priority plus haut = passe avant. Le moteur ne garde que le sommet.
 * @param cooldown en ticks, par joueur, avant que l'entrée puisse ressortir.
 * @param once     si vrai, l'entrée n'est jouée qu'une seule fois par joueur, et
 *                 ça survit à la déconnexion comme à la mort.
 */
public record DialogueEntry(
		String id,
		int priority,
		int cooldown,
		boolean once,
		DialogueConditions conditions,
		List<DialogueLine> lines
) {
	public static final Codec<DialogueEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("id").forGetter(DialogueEntry::id),
			Codec.INT.optionalFieldOf("priority", 0).forGetter(DialogueEntry::priority),
			Codec.INT.optionalFieldOf("cooldown", 0).forGetter(DialogueEntry::cooldown),
			Codec.BOOL.optionalFieldOf("once", false).forGetter(DialogueEntry::once),
			DialogueConditions.CODEC.optionalFieldOf("conditions", DialogueConditions.ALWAYS)
					.forGetter(DialogueEntry::conditions),
			DialogueLine.CODEC.listOf().optionalFieldOf("lines", List.of()).forGetter(DialogueEntry::lines)
	).apply(instance, DialogueEntry::new));

	public DialogueEntry {
		lines = List.copyOf(lines);
	}

	public boolean isPlayable() {
		return this.lines.stream().anyMatch(line -> !line.isEmpty());
	}

	/**
	 * Les délais distincts présents dans l'entrée, dans l'ordre chronologique.
	 * Chaque palier est un groupe de variantes entre lesquelles on tirera.
	 */
	public List<Integer> delaySteps() {
		List<Integer> steps = new ArrayList<>();
		for (DialogueLine line : this.lines) {
			if (!steps.contains(line.delayTicks())) {
				steps.add(line.delayTicks());
			}
		}
		steps.sort(Integer::compareTo);
		return steps;
	}

	public List<DialogueLine> linesAt(int delay) {
		return this.lines.stream().filter(line -> line.delayTicks() == delay).toList();
	}
}
