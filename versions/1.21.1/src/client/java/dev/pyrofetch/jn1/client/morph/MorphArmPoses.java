package dev.pyrofetch.jn1.client.morph;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.data.EntityModelData;

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
 */
final class MorphArmPoses {
	private static final float PI = (float) Math.PI;

	private MorphArmPoses() {
	}

	/** Pose les bras selon ce que le joueur fait de ses mains. */
	static void apply(AnimationProcessor<?> processor, PlayerEntity joueur, @Nullable EntityModelData regard) {
		float lacet = regard == null ? 0 : regard.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE;
		float tangage = regard == null ? 0 : regard.headPitch() * MathHelper.RADIANS_PER_DEGREE;
		boolean droitier = joueur.getMainArm() == Arm.RIGHT;

		if (joueur.isUsingItem()) {
			boolean actifDroit = (joueur.getActiveHand() == Hand.MAIN_HAND) == droitier;
			UseAction usage = joueur.getActiveItem().getUseAction();
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
				case SPEAR -> poser(processor, actifDroit ? "armR" : "armL", PI, 0);
				case SPYGLASS -> poser(processor, actifDroit ? "armR" : "armL",
						MathHelper.clamp(1.9198622F + tangage + (joueur.isInSneakingPose() ? 0.2617994F : 0), -3.3F, 2.4F),
						lacet + (actifDroit ? 0.2617994F : -0.2617994F));
				case TOOT_HORN -> poser(processor, actifDroit ? "armR" : "armL",
						1.4835298F + MathHelper.clamp(tangage, -1.2F, 1.2F),
						lacet + (actifDroit ? 0.5235988F : -0.5235988F));
				case BRUSH -> poser(processor, actifDroit ? "armR" : "armL", PI / 5, 0);
				default -> {
					// Manger, boire : le jeu ne bouge pas le bras en troisième personne.
				}
			}
			return;
		}

		// Une arbalète chargée se tient épaulée, prête à tirer.
		ItemStack principal = joueur.getMainHandStack();
		ItemStack secondaire = joueur.getOffHandStack();
		boolean chargéePrincipale = principal.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(principal);
		boolean chargéeSecondaire = secondaire.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(secondaire);
		if (chargéePrincipale || chargéeSecondaire) {
			boolean tenueDroite = chargéePrincipale == droitier;
			String tient = tenueDroite ? "armR" : "armL";
			String soutient = tenueDroite ? "armL" : "armR";
			float sens = tenueDroite ? 1 : -1;
			poser(processor, tient, PI / 2 - 0.1F + tangage, lacet + 0.3F * sens);
			poser(processor, soutient, 1.5F + tangage, lacet - 0.6F * sens);
		}
	}

	/** Le bras à ces angles, épaule sans roulis, avant-bras droit. */
	private static void poser(AnimationProcessor<?> processor, String bras, float rotX, float rotY) {
		GeoBone épaule = processor.getBone(bras);
		if (épaule == null) {
			return;
		}
		épaule.setRotX(rotX);
		épaule.setRotY(rotY);
		épaule.setRotZ(0);
		épaule.resetStateChanges();
		GeoBone avantBras = processor.getBone(bras.equals("armR") ? "lowerArmR" : "lowerArmL");
		if (avantBras != null) {
			avantBras.setRotX(avantBras.getInitialSnapshot().getRotX());
			avantBras.setRotY(avantBras.getInitialSnapshot().getRotY());
			avantBras.setRotZ(avantBras.getInitialSnapshot().getRotZ());
			avantBras.resetStateChanges();
		}
	}
}
