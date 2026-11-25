package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;

/**
 * La tenue du vide.
 *
 * <p>Ce n'est pas une armure : elle protège à peine mieux que le cuir. Ce
 * qu'elle apporte, c'est de pouvoir nager dans l'air de la dimension du vide
 * (voir {@link dev.pyrofetch.jn1.lab.VoidSwim}), et seulement portée en entier.
 *
 * <p>Ses textures de couche sont {@code textures/models/armor/void_suit_layer_1}
 * et {@code _layer_2}, peintes par {@code tools/gen_void_suit.py}.
 */
public final class ModArmorMaterials {
	public static final RegistryEntry<ArmorMaterial> VOID_SUIT = Registry.registerReference(
			Registries.ARMOR_MATERIAL, JN1.id("void_suit"), new ArmorMaterial(
					defense(1, 2, 3, 1),
					15,
					SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
					() -> Ingredient.ofItems(ModItems.DARK_MATTER),
					List.of(new ArmorMaterial.Layer(JN1.id("void_suit"))),
					0.0F,
					0.0F));

	private ModArmorMaterials() {
	}

	private static EnumMap<ArmorItem.Type, Integer> defense(int bottes, int jambières, int torse, int casque) {
		EnumMap<ArmorItem.Type, Integer> carte = new EnumMap<>(ArmorItem.Type.class);
		carte.put(ArmorItem.Type.BOOTS, bottes);
		carte.put(ArmorItem.Type.LEGGINGS, jambières);
		carte.put(ArmorItem.Type.CHESTPLATE, torse);
		carte.put(ArmorItem.Type.HELMET, casque);
		carte.put(ArmorItem.Type.BODY, torse);
		return carte;
	}

	public static void register() {
		JN1.LOGGER.debug("Matériaux d'armure enregistrés.");
	}
}
