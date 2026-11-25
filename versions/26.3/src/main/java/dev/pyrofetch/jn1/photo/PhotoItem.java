package dev.pyrofetch.jn1.photo;

import dev.pyrofetch.jn1.registry.ModComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * Le tirage : un objet qui porte le numéro et le cadrage d'une photo de
 * l'archive. Un tirage sans numéro est encore à développer.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoItem extends Item {
	public PhotoItem(Properties settings) {
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
	public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		PhotoFormat format = format(stack);
		tooltip.accept(
			Component.translatable(format.translationKey())
				.append(Component.translatable(format.cases() > 1 ? "photo.jn1.frames" : "photo.jn1.frame",
						format.casesEnLargeur(), format.casesEnHauteur()))
				.withStyle(ChatFormatting.GRAY)
		);
		if (!estDéveloppée(stack)) {
			tooltip.accept(Component.translatable("photo.jn1.blank").withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
