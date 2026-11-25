package dev.pyrofetch.jn1.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import dev.pyrofetch.jn1.lab.LabDimension;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Un article du magasin.
 *
 * <p>Le catalogue est partagé entre client et serveur : le premier l'affiche,
 * le second le revalide. Rien n'est envoyé d'un côté que l'autre ne puisse
 * vérifier — un client modifié ne peut donc pas s'offrir ce qu'il veut.
 *
 * <p>Deux guichets le lisent : Shop.exe, sur le terminal, et le distributeur.
 * Le second vend moins, et certains articles ne se trouvent que dans la
 * dimension du vide — la matière noire en premier lieu, qui n'existe que là-bas.
 * Ces restrictions sont tenues ici, et revérifiées par le serveur à l'achat.
 *
 * @param unlockFlag drapeau posé à l'achat, pour les déblocages définitifs ;
 *                   {@code null} pour une marchandise ordinaire
 * @param guichets   où l'article est proposé
 * @param videSeul   vrai si l'article n'est vendu que dans la dimension du vide
 */
public record ShopEntry(String id, ItemStack stack, int price, String unlockFlag,
                        Set<Guichet> guichets, boolean videSeul) {
	/** Ce qui sert de monnaie. */
	public static final net.minecraft.item.Item CURRENCY = Items.EMERALD;

	public static final String FLAG_JENNY = "unlocked_jenny";

	/** Là où l'on achète. */
	public enum Guichet { TERMINAL, DISTRIBUTEUR }

	private static final Set<Guichet> PARTOUT = EnumSet.allOf(Guichet.class);
	private static final Set<Guichet> TERMINAL_SEUL = EnumSet.of(Guichet.TERMINAL);

	private static final List<ShopEntry> CATALOGUE = List.of(
			new ShopEntry("computer", new ItemStack(
					Registries.ITEM.get(Identifier.of("jn1", "computer"))), 6, null, TERMINAL_SEUL, false),
			new ShopEntry("capsule", new ItemStack(ModItems.CAPSULE), 12, null, TERMINAL_SEUL, false),
			new ShopEntry("bread", new ItemStack(Items.BREAD, 8), 1, null, PARTOUT, false),
			new ShopEntry("iron", new ItemStack(Items.IRON_INGOT, 4), 3, null, PARTOUT, false),
			new ShopEntry("diamond", new ItemStack(Items.DIAMOND), 9, null, PARTOUT, false),
			new ShopEntry("lantern", new ItemStack(Items.LANTERN, 4), 2, null, PARTOUT, false),
			new ShopEntry("camera", new ItemStack(ModItems.CAMERA), 18, null, TERMINAL_SEUL, false),
			new ShopEntry("photo_frame", new ItemStack(
					Registries.ITEM.get(Identifier.of("jn1", "photo_frame")), 4), 2, null, PARTOUT, false),
			new ShopEntry("notebook", dev.pyrofetch.jn1.notebook.FieldNotebook.create(), 1, null, PARTOUT, false),
			new ShopEntry("dark_matter", new ItemStack(ModItems.DARK_MATTER), 2, null, PARTOUT, true)
	);

	/** Tout le catalogue, sans filtre : c'est ce que relit le serveur. */
	public static List<ShopEntry> catalogue() {
		return CATALOGUE;
	}

	/** Ce qu'un guichet propose, là où il se trouve. */
	public static List<ShopEntry> catalogue(Guichet guichet, World monde) {
		return CATALOGUE.stream().filter(entry -> entry.proposé(guichet, monde)).toList();
	}

	public static ShopEntry byId(String id) {
		return CATALOGUE.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
	}

	/** Vrai si ce guichet, dans ce monde, vend cet article. */
	public boolean proposé(Guichet guichet, World monde) {
		if (!this.guichets.contains(guichet)) {
			return false;
		}
		return !this.videSeul || monde.getRegistryKey().equals(LabDimension.LAB);
	}

	public boolean isUnlock() {
		return this.unlockFlag != null;
	}

	/** Clé de traduction du libellé affiché en boutique. */
	public String translationKey() {
		return "shop.jn1." + this.id;
	}
}
