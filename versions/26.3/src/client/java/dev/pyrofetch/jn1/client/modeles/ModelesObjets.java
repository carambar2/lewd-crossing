package dev.pyrofetch.jn1.client.modeles;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.client.color.item.ItemTintSources;

/**
 * Ce que les définitions d'objets du mod ({@code assets/jn1/items/*.json},
 * écrites par {@code outils/convertir_ressources.py}) nomment et que le jeu ne
 * connaît pas de lui-même.
 *
 * <p>À appeler au début de {@code JN1Client.onInitializeClient}, avant le
 * premier chargement des ressources : un type inconnu fait refuser toute la
 * définition (l'objet s'affiche alors en damier).
 *
 * <p>Ne sont pas ici les modèles spéciaux {@code jn1:capsule} et
 * {@code jn1:lidar} ({@code "type": "minecraft:special"}), qui vont avec leur
 * rendu ({@code CapsuleItemRenderer}, {@code LidarItemRenderer}) :
 * {@code SpecialModelRenderers.ID_MAPPER.put(JN1.id("capsule"), …)}.
 */
public final class ModelesObjets {
	private ModelesObjets() {
	}

	public static void enregistrer() {
		// La lingerie : la couleur de la fille, lue dans les données de la pièce.
		ItemTintSources.ID_MAPPER.put(JN1.id("lingerie"), TeinteLingerie.MAP_CODEC);
	}
}
