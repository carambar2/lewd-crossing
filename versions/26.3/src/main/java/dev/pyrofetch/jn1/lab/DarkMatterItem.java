package dev.pyrofetch.jn1.lab;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * La matière noire.
 *
 * <p>Un objet ordinaire, à l'infobulle près : une ligne en clair qui dit à quoi
 * elle sert, et une seconde dans l'alphabet de la table d'enchantement. Celle-là
 * ne sert à rien. Elle est là parce que le vide est ce genre d'endroit.
 */
public class DarkMatterItem extends Item {
	private static final Style ILLISIBLE = Style.EMPTY
			.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.withDefaultNamespace("alt")))
			.withColor(ChatFormatting.DARK_GRAY);

	public DarkMatterItem(Properties settings) {
		super(settings);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
	                            java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		tooltip.accept(Component.translatable("item.jn1.dark_matter.tooltip").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.jn1.dark_matter.whisper").setStyle(ILLISIBLE));
	}
}
