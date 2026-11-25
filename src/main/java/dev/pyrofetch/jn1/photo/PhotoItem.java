package dev.pyrofetch.jn1.photo;

import dev.pyrofetch.jn1.registry.ModComponents;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * Le tirage : un objet qui porte le numéro et le cadrage d'une photo de
 * l'archive. Un tirage sans numéro est encore à développer.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoItem extends Item {
	public PhotoItem(Settings settings) {
		super(settings);
	}

	public static int numéro(ItemStack pile) {
		return pile.getOrDefault(ModComponents.PHOTO_ID, 0);
	}

	public static PhotoFormat format(ItemStack pile) {
		return pile.getOrDefault(ModComponents.PHOTO_FORMAT, PhotoFormat.CARRÉ);
	}

	public static boolean estDéveloppée(ItemStack pile) {
		return numéro(pile) > 0;
	}

	public static ItemStack tirage(Item objet, int numéro, PhotoFormat format) {
		ItemStack pile = new ItemStack(objet);
		pile.set(ModComponents.PHOTO_ID, numéro);
		pile.set(ModComponents.PHOTO_FORMAT, format);
		return pile;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable TooltipContext context, List<Text> tooltip, TooltipType type) {
		PhotoFormat format = format(stack);
		tooltip.add(
			Text.translatable(format.translationKey())
				.append(Text.translatable(format.cases() > 1 ? "photo.jn1.frames" : "photo.jn1.frame",
						format.casesEnLargeur(), format.casesEnHauteur()))
				.formatted(Formatting.GRAY)
		);
		if (!estDéveloppée(stack)) {
			tooltip.add(Text.translatable("photo.jn1.blank").formatted(Formatting.DARK_GRAY));
		}
	}
}
