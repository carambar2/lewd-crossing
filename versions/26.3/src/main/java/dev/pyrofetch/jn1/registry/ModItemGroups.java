package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Comparator;

public final class ModItemGroups {
	public static final ResourceKey<CreativeModeTab> MAIN_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, JN1.id("main"));

	private ModItemGroups() {
	}

	/**
	 * Un tableau qui porte sa variante, comme ceux de l'onglet du jeu : posé,
	 * il prend celle-ci au lieu d'en tirer une au hasard.
	 */
	public static ItemStack affiche(Holder<PaintingVariant> variante) {
		// 26.3 : la variante est un composant de l'objet (comme dans l'onglet du
		// jeu), et non plus une donnée d'entité.
		ItemStack tableau = new ItemStack(Items.PAINTING);
		tableau.set(DataComponents.PAINTING_VARIANT, variante);
		return tableau;
	}

	public static void register() {
		CreativeModeTab group = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(ModItems.TAB_ICON))
				.title(Component.translatable("itemGroup.jn1.main"))
				.displayItems((context, entries) -> {
					for (Item item : ModItems.all()) {
						entries.accept(item);
					}
					for (Item item : ModItems.blockItems()) {
						entries.accept(item);
					}
					// Les plats « au cum » : chaque nourriture du jeu et du mod, déjà
					// nappée — sans quoi on ne les trouvait nulle part, la recette
					// spéciale n'ayant pas de place au livre de recettes.
					BuiltInRegistries.ITEM.stream()
							.filter(item -> item.components().has(DataComponents.FOOD))
							.filter(item -> item != ModItems.DRIED_CUM)
							.filter(item -> {
								String espace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
								return espace.equals("minecraft") || espace.equals(JN1.MOD_ID);
							})
							.forEach(item -> entries.accept(dev.pyrofetch.jn1.cum.AuCumRecipe.auCum(new ItemStack(item))));
					// Les affiches sont des tableaux du jeu, déclarés en données
					// (data/jn1/painting_variant) : on les lit dans le registre
					// plutôt que d'en tenir une seconde liste ici.
					context.holders().lookup(Registries.PAINTING_VARIANT).ifPresent(tableaux ->
							tableaux.listElements()
									.filter(entrée -> entrée.key().identifier().getNamespace().equals(JN1.MOD_ID))
									.sorted(Comparator.comparing(entrée -> entrée.key().identifier().getPath()))
									.forEach(entrée -> entries.accept(affiche(entrée))));
				})
				.build();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY, group);
		JN1.LOGGER.debug("Onglet créatif enregistré.");
	}
}
