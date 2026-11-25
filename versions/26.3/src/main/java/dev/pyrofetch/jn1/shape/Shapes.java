package dev.pyrofetch.jn1.shape;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.morph.MorphHandler;
import dev.pyrofetch.jn1.morph.MorphState;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Les formes : poitrine, croupe, taille entière, que les potions de forme
 * changent d'un cran à chaque dose.
 *
 * <p>Poitrine et croupe ne sont que des échelles d'os, appliquées au rendu
 * ({@code CompanionModel.applyShape}) : un niveau suivi par la compagne
 * ({@link CompanionEntity#getShape}), ou rangé dans {@link MorphState} pour le
 * joueur transformé — qui n'en voit l'effet que transformé. La taille passe
 * par l'attribut {@code scale} du jeu : boîte de collision, hauteur des yeux
 * et synchronisation viennent avec, pour le joueur comme pour la compagne.
 *
 * <p>Tout redescend d'un cran toutes les {@link #DÉCRUE} ticks vers la forme
 * d'origine.
 */
public final class Shapes {
	/** Trois minutes par cran. */
	public static final int DÉCRUE = 3600;
	private static final Identifier MODIFICATEUR = JN1.id("shape_size");

	/** Ce qu'une potion change, ses bornes et l'effet d'un cran. */
	public enum Part {
		BOOBS(-2, 5, 0.15F), BOOTY(-2, 5, 0.12F), SIZE(-3, 3, 0.1F);

		public final int min;
		public final int max;
		public final float pas;

		Part(int min, int max, float pas) {
			this.min = min;
			this.max = max;
			this.pas = pas;
		}

		/** Le facteur d'échelle d'un niveau. */
		public float facteur(int niveau) {
			return 1.0F + this.pas * niveau;
		}

		public String id() {
			return this.name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private Shapes() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer joueur : server.getPlayerList().getPlayers()) {
				if (joueur.tickCount > 0 && joueur.tickCount % DÉCRUE == 0) {
					décroître(joueur);
				}
			}
		});
	}

	public static int niveau(LivingEntity cible, Part part) {
		if (part == Part.SIZE) {
			AttributeInstance échelle = cible.getAttribute(Attributes.SCALE);
			AttributeModifier modificateur = échelle == null ? null : échelle.getModifier(MODIFICATEUR);
			return modificateur == null ? 0 : Math.round((float) modificateur.amount() / part.pas);
		}
		if (cible instanceof CompanionEntity fille) {
			return fille.getShape(part);
		}
		if (cible instanceof ServerPlayer joueur) {
			MorphState état = ModAttachments.morph(joueur);
			return part == Part.BOOBS ? état.boobs() : état.booty();
		}
		return 0;
	}

	/** Fixe un niveau, borné. Rend le niveau obtenu. */
	public static int fixer(LivingEntity cible, Part part, int niveau) {
		int borné = Math.clamp(niveau, part.min, part.max);
		if (part == Part.SIZE) {
			AttributeInstance échelle = cible.getAttribute(Attributes.SCALE);
			if (échelle != null) {
				échelle.removeModifier(MODIFICATEUR);
				if (borné != 0) {
					échelle.addPermanentModifier(new AttributeModifier(MODIFICATEUR, borné * part.pas,
							AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
				}
			}
		} else if (cible instanceof CompanionEntity fille) {
			fille.setShape(part, borné);
		} else if (cible instanceof ServerPlayer joueur) {
			MorphState état = ModAttachments.morph(joueur);
			MorphHandler.set(joueur, part == Part.BOOBS
					? état.withShape(borné, état.booty())
					: état.withShape(état.boobs(), borné));
		}
		return borné;
	}

	public static int changer(LivingEntity cible, Part part, int delta) {
		return fixer(cible, part, niveau(cible, part) + delta);
	}

	/** Chaque forme redescend d'un cran vers l'origine. */
	public static void décroître(LivingEntity cible) {
		for (Part part : Part.values()) {
			int n = niveau(cible, part);
			if (n != 0) {
				fixer(cible, part, n - Integer.signum(n));
			}
		}
	}
}
