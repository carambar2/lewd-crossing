package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Comparator;

public final class ModItemGroups {
	public static final RegistryKey<ItemGroup> MAIN_KEY =
			RegistryKey.of(RegistryKeys.ITEM_GROUP, JN1.id("main"));

	private ModItemGroups() {
	}

	/**
	 * Un tableau qui porte sa variante, comme ceux de l'onglet du jeu : posé,
	 * il prend celle-ci au lieu d'en tirer une au hasard.
	 */
	public static ItemStack affiche(Identifier variante) {
		NbtCompound données = new NbtCompound();
		données.putString("id", "minecraft:painting");
		données.putString("variant", variante.toString());
		ItemStack tableau = new ItemStack(Items.PAINTING);
		tableau.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(données));
		return tableau;
	}

	public static void register() {
		ItemGroup group = FabricItemGroup.builder()
				.icon(() -> new ItemStack(ModItems.TAB_ICON))
				.displayName(Text.translatable("itemGroup.jn1.main"))
				.entries((context, entries) -> {
					for (Item item : ModItems.all()) {
						entries.add(item);
					}
					for (Item item : ModItems.blockItems()) {
						entries.add(item);
					}
					// Les plats « au cum » : chaque nourriture du jeu et du mod, déjà
					// nappée — sans quoi on ne les trouvait nulle part, la recette
					// spéciale n'ayant pas de place au livre de recettes.
					Registries.ITEM.stream()
							.filter(item -> item.getComponents().contains(DataComponentTypes.FOOD))
							.filter(item -> item != ModItems.DRIED_CUM)
							.filter(item -> {
								String espace = Registries.ITEM.getId(item).getNamespace();
								return espace.equals("minecraft") || espace.equals(JN1.MOD_ID);
							})
							.forEach(item -> entries.add(dev.pyrofetch.jn1.cum.AuCumRecipe.auCum(new ItemStack(item))));
					// Les affiches sont des tableaux du jeu, déclarés en données
					// (data/jn1/painting_variant) : on les lit dans le registre
					// plutôt que d'en tenir une seconde liste ici.
					context.lookup().getOptionalWrapper(RegistryKeys.PAINTING_VARIANT).ifPresent(tableaux ->
							tableaux.streamEntries()
									.map(entrée -> entrée.registryKey().getValue())
									.filter(id -> id.getNamespace().equals(JN1.MOD_ID))
									.sorted(Comparator.comparing(Identifier::getPath))
									.forEach(id -> entries.add(affiche(id))));
				})
				.build();

		Registry.register(Registries.ITEM_GROUP, MAIN_KEY, group);
		JN1.LOGGER.debug("Onglet créatif enregistré.");
	}
}
