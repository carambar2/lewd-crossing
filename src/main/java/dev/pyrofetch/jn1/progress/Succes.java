package dev.pyrofetch.jn1.progress;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Les succès que seul le code sait reconnaître (critère {@code fait} en
 * {@code minecraft:impossible}, écrits par {@code tools/gen_succes.py}).
 * Accorder un succès déjà obtenu ne fait rien.
 */
public final class Succes {
	private Succes() {
	}

	public static void accorder(PlayerEntity joueur, String nom) {
		if (!(joueur instanceof ServerPlayerEntity serveur)) {
			return;
		}
		AdvancementEntry succès = serveur.server.getAdvancementLoader().get(JN1.id(nom));
		if (succès != null) {
			serveur.getAdvancementTracker().grantCriterion(succès, "fait");
		} else {
			JN1.LOGGER.warn("Succès inconnu : {}", nom);
		}
	}
}
