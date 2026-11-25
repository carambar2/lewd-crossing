package dev.pyrofetch.jn1.shop;

import dev.pyrofetch.jn1.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;

import java.util.List;

/**
 * Le catalogue des compagnes, tenu à part des marchandises.
 *
 * <p>Une compagne ne s'achète pas comme un pain : elle n'a pas de pile d'objets
 * à montrer, mais un modèle à présenter, et son achat lève un drapeau au lieu
 * de remplir l'inventaire. Les deux catalogues partagent la monnaie et le
 * chemin réseau, rien d'autre.
 *
 * @param id         identifiant stable, celui qui transite sur le réseau
 * @param type       l'entité à présenter, et à faire venir une fois acquise
 * @param price      en émeraudes
 * @param unlockFlag le drapeau posé sur la progression du joueur
 * @param previewScale taille au présentoir. Les trois sont présentées à la
 *                     même échelle : un modèle plus grand doit paraître plus
 *                     grand, pas être ramené à la taille des autres. Le champ
 *                     reste par compagne pour le jour où l'une d'elles sortira
 *                     vraiment du cadre.
 */
public record GirlEntry(String id, EntityType<? extends LivingEntity> type, int price,
                        String unlockFlag, float previewScale) {
	public static final String FLAG_ELLIE = "unlocked_ellie";

	private static final List<GirlEntry> CATALOGUE = List.of(
			new GirlEntry("jenny", ModEntities.JENNY, 24, ShopEntry.FLAG_JENNY, 0.72F),
			new GirlEntry("ellie", ModEntities.ELLIE, 30, FLAG_ELLIE, 0.72F));

	public static List<GirlEntry> catalogue() {
		return CATALOGUE;
	}

	public static GirlEntry byId(String id) {
		for (GirlEntry entry : CATALOGUE) {
			if (entry.id().equals(id)) {
				return entry;
			}
		}
		return null;
	}

	public String translationKey() {
		return "girl.jn1." + this.id;
	}
}
