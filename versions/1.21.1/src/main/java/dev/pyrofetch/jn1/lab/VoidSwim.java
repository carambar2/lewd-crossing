package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Nager dans l'air du vide.
 *
 * <p>Porter la tenue entière, dans la dimension du vide, les pieds hors du sol :
 * on n'y tombe plus, on y flotte. Avancer pousse dans le sens exact du regard —
 * on plonge en regardant ses pieds, on remonte en levant la tête —, le pas de
 * côté reste à l'horizontale, la barre d'espace fait monter et s'accroupir
 * descendre. Le sprint va deux fois plus vite et couche le corps comme dans
 * l'eau.
 *
 * <p>On ne passe pas par l'eau du jeu, même si elle ferait presque tout : elle
 * amènerait avec elle la noyade, le brouillard bleu, les bulles et les bruits
 * d'éclaboussure. Le mouvement est donc réécrit ici en entier, et seuls quelques
 * tests du client sur l'eau sont détournés pour que le sprint tienne en l'air
 * (voir {@code ClientPlayerEntityMixin}).
 *
 * <p>Trois mécanismes du jeu freinaient la première version, et c'est d'eux que
 * venait sa lenteur, pas de ses réglages :
 * <ul>
 *   <li>hors de l'eau, la pose de nage est celle du joueur qui rampe sous un
 *       bloc, et le client divise alors la saisie par trois — au sprint, on
 *       n'allait pas plus vite qu'au pas ({@code ClientPlayerEntityMixin}) ;
 *   <li>{@code PlayerEntity.travel} tire la vitesse verticale vers le regard,
 *       mais seulement quand on regarde vers le bas : on plongeait trois fois
 *       plus vite qu'on ne remontait. Le remplacement se fait donc à ce
 *       niveau-là, avant lui ({@code PlayerEntityMixin}) ;
 *   <li>la poussée du jeu ne connaît que le cap : le regard vers le bas, on
 *       filait quand même à l'horizontale. Elle est calculée ici.
 * </ul>
 *
 * <p>Le mouvement d'un joueur est calculé par son client ; le serveur n'en voit
 * que le résultat. Il doit seulement ne pas le prendre pour un tricheur qui
 * vole ({@code ServerPlayNetworkHandlerMixin}) ni lui compter une chute quand il
 * touche enfin le sol.
 */
public final class VoidSwim {
	/*
	 * Vitesse de croisière = poussée × traînée / (1 − traînée), soit neuf fois la
	 * poussée : environ 5 m/s au pas, un peu plus que la marche, et 11 m/s au
	 * sprint, la vitesse du vol en créatif.
	 */
	/** Accélération par tick, au pas puis au sprint. */
	private static final double POUSSÉE = 0.028;
	private static final double POUSSÉE_SPRINT = 0.06;
	/** Ce qu'il reste de la vitesse à chaque tick, tant qu'on nage. */
	private static final double TRAÎNÉE = 0.90;
	/**
	 * Et quand on lâche tout : on s'arrête en une demi-seconde. Sans ce frein, on
	 * glissait loin après avoir lâché les touches, et l'on manquait sa cible.
	 */
	private static final double TRAÎNÉE_LÂCHÉ = 0.84;
	/** Ce qui tire vers le bas : presque rien, mais on dérive si l'on ne fait rien. */
	private static final double DÉRIVE = 0.003;
	/** L'élan qu'on reçoit contre un rebord pour se hisser dessus, comme au sortir de l'eau. */
	private static final double HISSE = 0.3;

	private VoidSwim() {
	}

	/** La tenue, en entier. Trois pièces sur quatre ne suffisent pas. */
	public static boolean tenueComplète(LivingEntity entité) {
		return porte(entité, EquipmentSlot.HEAD, ModItems.VOID_HELMET)
				&& porte(entité, EquipmentSlot.CHEST, ModItems.VOID_SUIT)
				&& porte(entité, EquipmentSlot.LEGS, ModItems.VOID_LEGGINGS)
				&& porte(entité, EquipmentSlot.FEET, ModItems.VOID_BOOTS);
	}

	private static boolean porte(LivingEntity entité, EquipmentSlot emplacement, Item pièce) {
		return entité.getEquippedStack(emplacement).isOf(pièce);
	}

	public static boolean dansLeVide(Entity entité) {
		return entité.getWorld().getRegistryKey().equals(LabDimension.LAB);
	}

	/** Vrai tant que ce joueur nage : dans le vide, en tenue, hors du sol. */
	public static boolean nage(Entity entité) {
		return entité instanceof PlayerEntity joueur
				&& dansLeVide(joueur)
				&& !joueur.isOnGround()
				&& !joueur.hasVehicle()
				&& !joueur.getAbilities().flying
				&& !joueur.isSpectator()
				&& tenueComplète(joueur);
	}

	/**
	 * Un pas de nage.
	 *
	 * @param saisie   la saisie du jeu : x le pas de côté (positif à gauche),
	 *                 z l'avant
	 * @param monte    barre d'espace
	 * @param descend  accroupissement
	 */
	public static void avancer(LivingEntity entité, Vec3d saisie, boolean monte, boolean descend) {
		// L'avant suit le regard entier ; le côté reste à plat, sans quoi un pas
		// de côté tête baissée ferait aussi descendre.
		Vec3d regard = entité.getRotationVector();
		float cap = entité.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		Vec3d gauche = new Vec3d(MathHelper.cos(cap), 0.0, MathHelper.sin(cap));
		double vertical = (monte ? 1.0 : 0.0) - (descend ? 1.0 : 0.0);
		Vec3d direction = regard.multiply(saisie.z)
				.add(gauche.multiply(saisie.x))
				.add(0.0, vertical, 0.0);
		// En diagonale, on ne va pas plus vite qu'en ligne droite.
		if (direction.lengthSquared() > 1.0) {
			direction = direction.normalize();
		}
		boolean pousse = direction.lengthSquared() > 1.0E-4;

		double poussée = entité.isSprinting() ? POUSSÉE_SPRINT : POUSSÉE;
		entité.setVelocity(entité.getVelocity().add(direction.multiply(poussée)));

		double hauteurAvant = entité.getY();
		entité.move(MovementType.SELF, entité.getVelocity());

		double traînée = pousse ? TRAÎNÉE : TRAÎNÉE_LÂCHÉ;
		Vec3d après = entité.getVelocity();
		après = new Vec3d(après.x * traînée, après.y * traînée - DÉRIVE, après.z * traînée);
		// Buté contre un rebord qu'on dépasse presque : on se hisse dessus, comme
		// le jeu le fait au bord de l'eau. Sans cela, aborder le chemin de côté
		// demandait de remonter à la main, puis de revenir.
		if (pousse && entité.horizontalCollision
				&& entité.doesNotCollide(après.x, après.y + 0.6 - entité.getY() + hauteurAvant, après.z)) {
			après = new Vec3d(après.x, HISSE, après.z);
		}
		entité.setVelocity(après);
		entité.fallDistance = 0.0F;
		// En trois dimensions : une plongée droite doit aussi faire battre les bras.
		entité.updateLimbs(true);
	}
}
