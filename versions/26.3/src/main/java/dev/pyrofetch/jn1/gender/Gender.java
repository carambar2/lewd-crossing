package dev.pyrofetch.jn1.gender;

import dev.pyrofetch.jn1.network.GenderChoicePayload;
import dev.pyrofetch.jn1.network.OpenGenderPayload;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Homme ou femme : le joueur le choisit à sa première venue dans un monde
 * (écran {@code GenderScreen}), la potion d'inversion le change ensuite.
 *
 * <p>Ce que ça décide : l'objet de plaisir qui lui va (le masturbateur, le
 * dildo — {@link dev.pyrofetch.jn1.excitation.Plaisir}) et, quand il prend la
 * place de {@code steve} dans une scène, si la pièce de scène paraît.
 * L'apparence du personnage ne change pas : c'est son skin.
 *
 * <p>Rangé dans un attachement persistant, gardé à la mort : {@code ""} tant
 * qu'il n'a pas répondu, puis {@code homme} ou {@code femme}.
 */
public final class Gender {
	public static final String HOMME = "homme";
	public static final String FEMME = "femme";

	private Gender() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!choisi(handler.player)) {
				ServerPlayNetworking.send(handler.player, OpenGenderPayload.INSTANCE);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(GenderChoicePayload.ID,
				(payload, context) -> context.server().execute(() -> {
					// Une seule fois : ensuite, c'est l'affaire de la potion.
					if (!choisi(context.player())) {
						fixer(context.player(), payload.femme() ? FEMME : HOMME);
					}
				}));
	}

	public static String get(Player joueur) {
		return joueur.getAttachedOrCreate(ModAttachments.GENDER);
	}

	public static boolean choisi(Player joueur) {
		return !get(joueur).isEmpty();
	}

	/** Femme ; un joueur qui n'a pas encore répondu compte pour un homme. */
	public static boolean femme(Player joueur) {
		return FEMME.equals(get(joueur));
	}

	public static void fixer(ServerPlayer joueur, String genre) {
		joueur.setAttached(ModAttachments.GENDER, genre);
		joueur.sendOverlayMessage(Component.translatable("message.jn1.gender." + genre));
	}

	/** La potion d'inversion. */
	public static void inverser(ServerPlayer joueur) {
		fixer(joueur, femme(joueur) ? HOMME : FEMME);
	}
}
