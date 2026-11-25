package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.morph.MorphGesture;
import dev.pyrofetch.jn1.morph.MorphState;
import dev.pyrofetch.jn1.network.MorphGesturePayload;
import dev.pyrofetch.jn1.network.MorphSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Ce que le client sait des transformations : la sienne et celles des joueurs qu'il voit. */
public final class ClientMorphs {
	private static final Map<UUID, MorphState> ÉTATS = new HashMap<>();
	private static final Map<UUID, Geste> GESTES = new HashMap<>();
	private static int numéro;

	/**
	 * Un geste en cours : lequel, depuis quand (temps du monde client), et un
	 * numéro qui distingue deux fois le même geste — le second reprend au début.
	 */
	public record Geste(MorphGesture geste, long début, int numéro) {
	}

	private ClientMorphs() {
	}

	public static void receive(MorphSyncPayload payload) {
		ÉTATS.put(payload.player(), new MorphState(payload.kind(), payload.outfit(), payload.pending(),
				payload.boobs(), payload.booty()));
		GESTES.remove(payload.player());
	}

	public static void receive(MorphGesturePayload payload) {
		MorphGesture geste = MorphGesture.byId(payload.gesture());
		Minecraft client = Minecraft.getInstance();
		if (geste != null && client.level != null) {
			GESTES.put(payload.player(), new Geste(geste, client.level.getGameTime(), ++numéro));
		}
	}

	/** Le geste que fait ce joueur, ou {@code null} ; un geste échu est oublié. */
	@Nullable
	public static Geste gesture(Player player) {
		Geste geste = GESTES.get(player.getUUID());
		if (geste != null && !geste.geste().loops()
				&& player.level().getGameTime() - geste.début() > geste.geste().durationTicks()) {
			GESTES.remove(player.getUUID());
			return null;
		}
		return geste;
	}

	/** Il a bougé, ou changé d'apparence : le geste cesse. */
	public static void stopGesture(Player player) {
		GESTES.remove(player.getUUID());
	}

	public static MorphState of(Player player) {
		return ÉTATS.getOrDefault(player.getUUID(), MorphState.NONE);
	}

	/** La sienne. */
	public static MorphState self() {
		Player joueur = Minecraft.getInstance().player;
		return joueur == null ? MorphState.NONE : of(joueur);
	}

	public static void clear() {
		ÉTATS.clear();
		GESTES.clear();
	}
}
