package dev.pyrofetch.jn1.notebook;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.lab.LabDimension;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Le carnet de terrain : remplace le manuel du laboratoire, livre du jeu qu'il
 * était. Remis à la première connexion, vendu à la boutique.
 *
 * <p>Son contenu vit côté client ({@code assets/jn1/notebook/carnet.json}) :
 * le serveur n'en connaît que les drapeaux qui débloquent des pages, posés
 * ici au fil de la progression et synchronisés avec elle.
 */
public final class FieldNotebook {
	/** Posé quand le carnet a été remis : on ne le redonne pas à chaque connexion. */
	public static final String FLAG_GIVEN = "notebook_given";
	/** Premier passage dans le vide. */
	public static final String FLAG_VOID = "notebook_void";
	/** Aura éveillée : on l'a trouvée. */
	public static final String FLAG_AURA = "notebook_aura";
	/** Première contamination près d'une brèche. */
	public static final String FLAG_BREACH = "notebook_breach";

	private FieldNotebook() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				server.execute(() -> remettre(handler.getPlayer())));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((joueur, origine, destination) -> {
			if (destination.dimension().equals(LabDimension.LAB)) {
				DialogueEngine.setFlag(joueur, FLAG_VOID);
			}
		});
	}

	public static ItemStack create() {
		return new ItemStack(ModItems.NOTEBOOK);
	}

	/**
	 * À la première connexion. Un joueur qui avait reçu l'ancien manuel reçoit
	 * aussi le carnet : son drapeau est un autre.
	 */
	private static void remettre(ServerPlayer joueur) {
		if (ModAttachments.get(joueur).hasFlag(FLAG_GIVEN)) {
			return;
		}
		ItemStack carnet = create();
		if (!joueur.getInventory().add(carnet)) {
			joueur.drop(carnet, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
		DialogueEngine.setFlag(joueur, FLAG_GIVEN);
		JN1.LOGGER.debug("Carnet remis à {}.", joueur.getName().getString());
	}
}
