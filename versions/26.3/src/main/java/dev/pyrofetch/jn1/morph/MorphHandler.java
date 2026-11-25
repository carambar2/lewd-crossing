package dev.pyrofetch.jn1.morph;

import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.network.MorphGesturePayload;
import dev.pyrofetch.jn1.network.MorphRequestPayload;
import dev.pyrofetch.jn1.network.MorphSyncPayload;
import dev.pyrofetch.jn1.network.OpenMorphPayload;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * La transformation : ce que la potion ouvre, ce que l'écran demande, et la
 * façon dont les autres joueurs l'apprennent.
 *
 * <p>Le serveur tient l'état ({@link ModAttachments#MORPH}) ; les clients n'en
 * ont qu'une copie, envoyée au joueur lui-même, à ceux qui le voient à chaque
 * changement, et à quiconque commence à le voir.
 */
public final class MorphHandler {
	private MorphHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(MorphRequestPayload.ID,
				(payload, context) -> context.server().execute(
						() -> handle(context.player(), payload)));
		EntityTrackingEvents.START_TRACKING.register((suivi, joueur) -> {
			if (suivi instanceof ServerPlayer cible) {
				ServerPlayNetworking.send(joueur, payload(cible, ModAttachments.morph(cible)));
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ServerPlayNetworking.send(handler.player, payload(handler.player, ModAttachments.morph(handler.player))));
	}

	/** La potion est bue : il peut désormais choisir. */
	public static void potionDrunk(ServerPlayer player) {
		MorphState état = ModAttachments.morph(player);
		set(player, état.with(état.kind(), état.outfit(), true));
		ServerPlayNetworking.send(player, OpenMorphPayload.INSTANCE);
	}

	/**
	 * Change l'apparence et le fait savoir. Aussi appelé par la commande de
	 * test, qui passe outre la potion.
	 */
	public static void set(ServerPlayer player, MorphState état) {
		ModAttachments.setMorph(player, état);
		MorphSyncPayload paquet = payload(player, état);
		ServerPlayNetworking.send(player, paquet);
		for (ServerPlayer témoin : PlayerLookup.tracking(player)) {
			if (témoin != player) {
				ServerPlayNetworking.send(témoin, paquet);
			}
		}
	}

	private static MorphSyncPayload payload(ServerPlayer player, MorphState état) {
		return new MorphSyncPayload(player.getUUID(), état.kind(), état.currentOutfit(), état.pending(),
				état.boobs(), état.booty());
	}

	private static void handle(ServerPlayer player, MorphRequestPayload payload) {
		MorphState état = ModAttachments.morph(player);
		switch (payload.action()) {
			case MorphRequestPayload.BECOME -> {
				// Il faut avoir bu la potion, ou l'être déjà : on change alors
				// de compagne sans en reboire.
				CompanionKind cible = CompanionKind.byId(payload.argument());
				if (cible != null && (état.pending() || état.morphed())) {
					// Chaque fille paraît dans sa tenue par défaut ; rester la même
					// fille garde celle qu'on portait.
					String tenue = cible.id().equals(état.kind()) ? état.currentOutfit() : cible.defaultOutfit();
					set(player, état.with(cible.id(), tenue, false));
				}
			}
			case MorphRequestPayload.OUTFIT -> {
				CompanionKind actuelle = état.companion();
				if (actuelle != null && actuelle.outfits().contains(payload.argument())) {
					set(player, état.with(état.kind(), payload.argument(), false));
				}
			}
			case MorphRequestPayload.REVERT -> {
				if (état.morphed() || état.pending()) {
					set(player, état.with("", "", false));
				}
			}
			case MorphRequestPayload.GESTURE -> {
				MorphGesture geste = MorphGesture.byId(payload.argument());
				CompanionKind actuelle = état.companion();
				if (geste != null && actuelle != null && actuelle.animation(geste.role()) != null) {
					MorphGesturePayload paquet = new MorphGesturePayload(player.getUUID(), geste.id());
					ServerPlayNetworking.send(player, paquet);
					for (ServerPlayer témoin : PlayerLookup.tracking(player)) {
						if (témoin != player) {
							ServerPlayNetworking.send(témoin, paquet);
						}
					}
				}
			}
			default -> {
				// Une action inconnue ne vient pas de l'écran.
			}
		}
	}
}
