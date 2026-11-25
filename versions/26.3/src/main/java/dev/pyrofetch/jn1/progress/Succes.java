package dev.pyrofetch.jn1.progress;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Les succès que seul le code sait reconnaître (critère {@code fait} en
 * {@code minecraft:impossible}, écrits par {@code tools/gen_succes.py}).
 * Accorder un succès déjà obtenu ne fait rien.
 */
public final class Succes {
	private Succes() {
	}

	public static void accorder(Player joueur, String nom) {
		if (!(joueur instanceof ServerPlayer serveur)) {
			return;
		}
		AdvancementHolder succès = serveur.level().getServer().getAdvancements().get(JN1.id(nom));
		if (succès != null) {
			serveur.getAdvancements().award(succès, "fait");
		} else {
			JN1.LOGGER.warn("Succès inconnu : {}", nom);
		}
	}
}
