package dev.pyrofetch.jn1.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Ce qu'un distributeur met en vitrine, qu'il suive le catalogue ou qu'on
 * l'ait garni à la main.
 *
 * @param id      ce que l'achat désigne : l'identifiant du catalogue
 *                ({@code bread}), ou {@code #3} pour le rayon 3 d'une machine
 *                garnie à la main
 * @param place   la case de la vitrine, de gauche à droite puis de haut en bas
 * @param libellé ce que le message d'achat nomme
 */
public record VendingOffer(String id, int place, ItemStack stack, int price, Component libellé) {
	/** Préfixe des identifiants d'une machine garnie à la main. */
	public static final String RAYON = "#";

	/** Un article du catalogue, à sa place dans la liste. */
	public static VendingOffer of(ShopEntry entry, int place) {
		return new VendingOffer(entry.id(), place, entry.stack(), entry.price(),
				Component.translatable(entry.translationKey()));
	}

	/** Un rayon garni à la main : nommé comme l'objet, avec sa quantité. */
	public static VendingOffer rayon(int rayon, ItemStack stack, int price) {
		Component nom = stack.getCount() > 1
				? Component.empty().append(stack.getHoverName()).append(" x" + stack.getCount())
				: stack.getHoverName();
		return new VendingOffer(RAYON + rayon, rayon, stack, price, nom);
	}
}
