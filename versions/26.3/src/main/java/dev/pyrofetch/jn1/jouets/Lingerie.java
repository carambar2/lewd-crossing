package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModItems;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Les sous-vêtements qu'une fille laisse tomber quand elle passe à une tenue
 * plus légère — plus loin dans la liste de ses tenues ({@code CompanionKind},
 * de la plus habillée à la plus nue).
 *
 * <p>Quand on sait ce que porte chaque tenue ({@link #PORTÉS} : celles de
 * Jenny, dont les sous-vêtements sont dessinés), elle lâche exactement ce
 * qu'elle retire : rien en passant en sous-vêtements, le soutien-gorge en
 * gardant sa culotte, la culotte en se mettant nue. Ailleurs, vers la
 * dernière tenue elle laisse tout, vers une autre plus légère l'un des deux.
 * Une fois par jour du jeu et par fille : sinon on les cultivait en changeant
 * de tenue en boucle.
 *
 * <p>La pièce porte le nom de la fille (« Culotte de Jenny ») et sa couleur,
 * dans ses données ({@link #FILLE}) : le client la teinte d'après elle.
 */
public final class Lingerie {
	public static final String FILLE = "jn1_fille";
	public static final String COULEUR = "jn1_couleur";
	private static final Map<String, Integer> COULEURS = Map.of(
			"jenny", 0xF27AAE, "ellie", 0x2A2430, "aura", 0x5ACDF5);

	private static final int CULOTTE = 1;
	private static final int SOUTIEN_GORGE = 2;
	/** Ce que chaque tenue porte dessous, quand on le sait (voir tools/gen_dessous.py). */
	private static final Map<String, Integer> PORTÉS = Map.ofEntries(
			Map.entry("jenny", CULOTTE | SOUTIEN_GORGE), Map.entry("jenny3", CULOTTE | SOUTIEN_GORGE),
			Map.entry("jenny4", CULOTTE), Map.entry("jenny2", 0));

	private Lingerie() {
	}

	/** Le changement de tenue est fait : ce qui tombe, s'il y a lieu. Côté serveur. */
	public static void changer(CompanionEntity fille, String avant, String après,
	                           @org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player joueur) {
		List<String> tenues = fille.kind().outfits();
		int i = tenues.indexOf(avant), j = tenues.indexOf(après);
		if (i < 0 || j <= i) {
			return;
		}
		boolean nue = j == tenues.size() - 1;
		Integer portésAvant = PORTÉS.get(avant), portésAprès = PORTÉS.get(après);
		int retirés;
		if (portésAvant != null && portésAprès != null) {
			retirés = portésAvant & ~portésAprès;
		} else {
			retirés = nue ? CULOTTE | SOUTIEN_GORGE : fille.getRandom().nextBoolean() ? CULOTTE : SOUTIEN_GORGE;
		}
		// Rien à retirer ne compte pas comme la pièce du jour.
		if (retirés == 0 || !fille.peutLâcherLingerie()) {
			return;
		}
		if ((retirés & CULOTTE) != 0) {
			lâcher(fille, ModItems.PANTIES, joueur);
		}
		if ((retirés & SOUTIEN_GORGE) != 0) {
			lâcher(fille, ModItems.BRA, joueur);
		}
		if (nue && joueur != null) {
			dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "deshabillee");
		}
	}

	public static ItemStack pièce(Item objet, CompanionEntity fille) {
		ItemStack pile = new ItemStack(objet);
		String clé = objet == ModItems.BRA ? "item.jn1.bra.of" : "item.jn1.panties.of";
		pile.set(DataComponents.ITEM_NAME, Component.translatable(clé, fille.getName()));
		int couleur = COULEURS.getOrDefault(fille.kind().id(), 0xF27AAE);
		CustomData.update(DataComponents.CUSTOM_DATA, pile, nbt -> {
			nbt.putString(FILLE, fille.kind().id());
			nbt.putInt(COULEUR, couleur);
		});
		return pile;
	}

	/** La couleur d'une pièce, pour la teinte du client. */
	public static int couleur(ItemStack pile) {
		CustomData données = pile.get(DataComponents.CUSTOM_DATA);
		return données != null ? données.copyTag().getIntOr(COULEUR, 0xF27AAE) : 0xF27AAE;
	}

	/**
	 * Elle la lance devant elle, vers le joueur s'il est là : tombée à ses pieds,
	 * la pièce disparaissait dans ses jambes.
	 */
	private static void lâcher(CompanionEntity fille, Item objet,
	                           @org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player joueur) {
		net.minecraft.world.phys.Vec3 devant;
		if (joueur != null && joueur.distanceToSqr(fille) > 0.01) {
			devant = joueur.position().subtract(fille.position()).multiply(1, 0, 1).normalize();
		} else {
			double lacet = Math.toRadians(fille.getVisualRotationYInDegrees());
			devant = new net.minecraft.world.phys.Vec3(-Math.sin(lacet), 0, Math.cos(lacet));
		}
		double écart = (fille.getRandom().nextDouble() - 0.5) * 0.08;
		ItemEntity tombée = new ItemEntity(fille.level(),
				fille.getX() + devant.x * 0.4, fille.getY() + 1.1, fille.getZ() + devant.z * 0.4,
				pièce(objet, fille));
		tombée.setDeltaMovement(devant.x * 0.2 - devant.z * écart, 0.22, devant.z * 0.2 + devant.x * écart);
		tombée.setDefaultPickUpDelay();
		fille.level().addFreshEntity(tombée);
	}
}
