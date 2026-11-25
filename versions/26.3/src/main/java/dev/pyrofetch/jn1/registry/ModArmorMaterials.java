package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * La tenue du vide.
 *
 * <p>Ce n'est pas une armure : elle protège à peine mieux que le cuir. Ce
 * qu'elle apporte, c'est de pouvoir nager dans l'air de la dimension du vide
 * (voir {@link dev.pyrofetch.jn1.lab.VoidSwim}), et seulement portée en entier.
 *
 * <p>26.3 : le matériau n'est plus enregistré (depuis 1.21.2, c'est un simple
 * enregistrement de données). Ses couches dessinées sont décrites par
 * l'équipement {@code assets/jn1/equipment/void_suit.json} (couches
 * {@code humanoid} et {@code humanoid_leggings}, planches sous
 * {@code textures/entity/equipment/}), à la place de
 * {@code textures/models/armor/void_suit_layer_1} et {@code _layer_2}. On le
 * répare avec la matière noire : l'étiquette {@code jn1:repairs_void_suit}.
 */
public final class ModArmorMaterials {
	/** Ce qui la répare : la matière noire ({@code data/jn1/tags/item/repairs_void_suit.json}). */
	public static final TagKey<Item> REPAIRS_VOID_SUIT = TagKey.create(Registries.ITEM, JN1.id("repairs_void_suit"));

	/** Son allure portée : {@code assets/jn1/equipment/void_suit.json}. */
	public static final ResourceKey<EquipmentAsset> VOID_SUIT_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, JN1.id("void_suit"));

	public static final ArmorMaterial VOID_SUIT = new ArmorMaterial(
			15,
			defense(1, 2, 3, 1),
			15,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			0.0F,
			0.0F,
			REPAIRS_VOID_SUIT,
			VOID_SUIT_ASSET);

	private ModArmorMaterials() {
	}

	private static Map<ArmorType, Integer> defense(int bottes, int jambières, int torse, int casque) {
		EnumMap<ArmorType, Integer> carte = new EnumMap<>(ArmorType.class);
		carte.put(ArmorType.BOOTS, bottes);
		carte.put(ArmorType.LEGGINGS, jambières);
		carte.put(ArmorType.CHESTPLATE, torse);
		carte.put(ArmorType.HELMET, casque);
		carte.put(ArmorType.BODY, torse);
		return carte;
	}

	public static void register() {
		JN1.LOGGER.debug("Matériaux d'armure enregistrés.");
	}
}
