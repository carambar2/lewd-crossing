package dev.pyrofetch.jn1.client.morph;

import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

/**
 * Les bras du joueur transformé quand il se sert d'un objet : arc bandé,
 * bouclier levé, trident armé, arbalète chargée ou tenue, longue-vue, corne,
 * pinceau.
 *
 * <p>Les angles sont ceux du joueur du jeu ({@code BipedEntityModel},
 * {@code CrossbowPosing}), passés dans les axes de GeckoLib : ses modèles ont
 * x et y inversés par rapport à ceux du jeu, si bien que tangage et lacet y
 * changent de signe et que le roulis reste. C'est la même correspondance que
 * pour la tête ({@link dev.pyrofetch.jn1.client.render.CompanionModel#turnHead}),
 * dont on reprend ici les angles, déjà convertis par GeckoLib.
 *
 * <p>Un bras posé <em>remplace</em> l'animation — l'arc se bande bras tendu,
 * quoi que fasse la marche — et son avant-bras revient droit : sa rotation
 * de repos, que le cube compense. Le roulis de repos des épaules (les bras un
 * peu écartés) est retiré, comme chez le joueur.
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Ce que fait le joueur est relevé à
 * l'image ({@link Bras#relever}), le rendu ne voyant plus que son état ; et un
 * os ne porte plus sa rotation entière mais l'écart à sa rotation de repos :
 * un angle à <em>fixer</em> s'écrit donc moins la rotation de repos de l'os,
 * et « revenir au repos », c'est un écart nul.
 */
final class MorphArmPoses {
	private static final float PI = (float) Math.PI;

	private MorphArmPoses() {
	}

	/**
	 * Ce que le joueur fait de ses mains, relevé à l'image.
	 *
	 * @param usage         l'objet en usage, ou rien
	 * @param actifDroit    le bras qui s'en sert est le droit
	 * @param chargéePrincipale une arbalète chargée en main principale
	 * @param chargéeSecondaire en main secondaire
	 */
	record Bras(boolean droitier, @org.jetbrains.annotations.Nullable ItemUseAnimation usage, boolean actifDroit,
	            boolean accroupi, boolean chargéePrincipale, boolean chargéeSecondaire) {
		static Bras relever(Player joueur) {
			boolean droitier = joueur.getMainArm() == HumanoidArm.RIGHT;
			ItemStack principal = joueur.getMainHandItem();
			ItemStack secondaire = joueur.getOffhandItem();
			return new Bras(droitier,
					joueur.isUsingItem() ? joueur.getUseItem().getUseAnimation() : null,
					(joueur.getUsedItemHand() == InteractionHand.MAIN_HAND) == droitier,
					joueur.isCrouching(),
					principal.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(principal),
					secondaire.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(secondaire));
		}
	}

	/**
	 * Pose les bras selon ce que le joueur fait de ses mains.
	 *
	 * @param lacetTête   l'écart de la tête au corps, en degrés, au signe des os de GeckoLib
	 * @param tangageTête le tangage de la tête, au même signe
	 */
	static void apply(BoneSnapshots processor, Bras joueur, float lacetTête, float tangageTête) {
		float lacet = lacetTête * Mth.DEG_TO_RAD;
		float tangage = tangageTête * Mth.DEG_TO_RAD;
		boolean droitier = joueur.droitier();

		if (joueur.usage() != null) {
			boolean actifDroit = joueur.actifDroit();
			ItemUseAnimation usage = joueur.usage();
			switch (usage) {
				case BOW -> {
					// Les deux bras visent ; celui qui ne tient pas l'arc croise
					// vers l'autre pour tirer la corde.
					poser(processor, "armR", PI / 2 + tangage, lacet + (actifDroit ? 0.1F : 0.5F));
					poser(processor, "armL", PI / 2 + tangage, lacet + (actifDroit ? -0.5F : -0.1F));
				}
				case CROSSBOW -> {
					// La recharge : les deux bras à mi-hauteur, devant.
					poser(processor, "armR", 0.97F, lacet * 0.5F + (actifDroit ? 0.8F : -0.4F));
					poser(processor, "armL", 0.97F, lacet * 0.5F + (actifDroit ? 0.4F : -0.8F));
				}
				case BLOCK -> poser(processor, actifDroit ? "armR" : "armL", 0.9424779F, actifDroit ? 0.5235988F : -0.5235988F);
				// Le trident armé (le « SPEAR » de 1.21.1 ; la lance de 26.3 est autre chose).
				case TRIDENT -> poser(processor, actifDroit ? "armR" : "armL", PI, 0);
				case SPYGLASS -> poser(processor, actifDroit ? "armR" : "armL",
						Mth.clamp(1.9198622F + tangage + (joueur.accroupi() ? 0.2617994F : 0), -3.3F, 2.4F),
						lacet + (actifDroit ? 0.2617994F : -0.2617994F));
				case TOOT_HORN -> poser(processor, actifDroit ? "armR" : "armL",
						1.4835298F + Mth.clamp(tangage, -1.2F, 1.2F),
						lacet + (actifDroit ? 0.5235988F : -0.5235988F));
				case BRUSH -> poser(processor, actifDroit ? "armR" : "armL", PI / 5, 0);
				default -> {
					// Manger, boire : le jeu ne bouge pas le bras en troisième personne.
				}
			}
			return;
		}

		// Une arbalète chargée se tient épaulée, prête à tirer.
		boolean chargéePrincipale = joueur.chargéePrincipale();
		boolean chargéeSecondaire = joueur.chargéeSecondaire();
		if (chargéePrincipale || chargéeSecondaire) {
			boolean tenueDroite = chargéePrincipale == droitier;
			String tient = tenueDroite ? "armR" : "armL";
			String soutient = tenueDroite ? "armL" : "armR";
			float sens = tenueDroite ? 1 : -1;
			poser(processor, tient, PI / 2 - 0.1F + tangage, lacet + 0.3F * sens);
			poser(processor, soutient, 1.5F + tangage, lacet - 0.6F * sens);
		}
	}

	/**
	 * Le bras à ces angles, épaule sans roulis, avant-bras droit. Les angles
	 * sont des rotations entières : on y retire celle de repos de l'os, que
	 * GeckoLib 5 ajoute de lui-même.
	 */
	private static void poser(BoneSnapshots processor, String bras, float rotX, float rotY) {
		processor.ifPresent(bras, épaule -> {
			épaule.setRotX(rotX - épaule.getBone().baseRotX());
			épaule.setRotY(rotY - épaule.getBone().baseRotY());
			épaule.setRotZ(-épaule.getBone().baseRotZ());
			// L'avant-bras à sa rotation de repos : un écart nul.
			processor.ifPresent(bras.equals("armR") ? "lowerArmR" : "lowerArmL",
					avantBras -> avantBras.setRotation(0.0F, 0.0F, 0.0F));
		});
	}
}
