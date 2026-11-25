package dev.pyrofetch.jn1.lab;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * La matière noire.
 *
 * <p>Un objet ordinaire, à l'infobulle près : une ligne en clair qui dit à quoi
 * elle sert, et une seconde dans l'alphabet de la table d'enchantement. Celle-là
 * ne sert à rien. Elle est là parce que le vide est ce genre d'endroit.
 */
public class DarkMatterItem extends Item {
	private static final Style ILLISIBLE = Style.EMPTY
			.withFont(Identifier.ofVanilla("alt"))
			.withColor(Formatting.DARK_GRAY);

	public DarkMatterItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.jn1.dark_matter.tooltip").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.jn1.dark_matter.whisper").setStyle(ILLISIBLE));
	}
}
