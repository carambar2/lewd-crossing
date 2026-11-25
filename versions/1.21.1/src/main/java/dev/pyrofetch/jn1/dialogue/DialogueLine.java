package dev.pyrofetch.jn1.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Une réplique.
 *
 * <p>{@code translate} est la voie normale : le texte vit dans {@code lang/},
 * donc il reste traduisible. {@code text} écrit la chaîne telle quelle, ce qui
 * dépanne pour prototyper un pack sans toucher aux fichiers de langue.
 *
 * <p>{@code delay_ticks} découpe l'entrée en temps : toutes les lignes qui
 * partagent le même délai sont des variantes entre lesquelles on tire au poids,
 * et les délais différents s'enchaînent. C'est ce qui permet « elle lance une
 * vanne, puis deux secondes plus tard elle dit autre chose ».
 */
public record DialogueLine(Optional<String> translate, Optional<String> text, int weight, int delayTicks,
                           Optional<Identifier> font, Optional<Integer> color) {
	public static final Codec<DialogueLine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("translate").forGetter(DialogueLine::translate),
			Codec.STRING.optionalFieldOf("text").forGetter(DialogueLine::text),
			Codec.INT.optionalFieldOf("weight", 1).forGetter(DialogueLine::weight),
			Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(DialogueLine::delayTicks),
			Identifier.CODEC.optionalFieldOf("font").forGetter(DialogueLine::font),
			Codec.INT.optionalFieldOf("color").forGetter(DialogueLine::color)
	).apply(instance, DialogueLine::new));

	public DialogueLine {
		weight = Math.max(1, weight);
		delayTicks = Math.max(0, delayTicks);
	}

	public Text toText() {
		MutableText base = this.translate
				.map(Text::translatable)
				.orElseGet(() -> Text.literal(this.text.orElse("")));

		// « minecraft:alt » est la police de la table d'enchantement : l'alphabet
		// des Bâtisseurs, que plus personne ne lit.
		return base.styled(style -> {
			Style styled = style;
			if (this.font.isPresent()) {
				styled = styled.withFont(this.font.get());
			}
			if (this.color.isPresent()) {
				styled = styled.withColor(this.color.get());
			}
			return styled;
		});
	}

	public boolean isEmpty() {
		return this.translate.isEmpty() && this.text.isEmpty();
	}
}
