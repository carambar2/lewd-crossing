package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.morph.MorphGesture;
import dev.pyrofetch.jn1.morph.MorphState;
import dev.pyrofetch.jn1.network.MorphGesturePayload;
import dev.pyrofetch.jn1.network.MorphSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
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
		MinecraftClient client = MinecraftClient.getInstance();
		if (geste != null && client.world != null) {
			GESTES.put(payload.player(), new Geste(geste, client.world.getTime(), ++numéro));
		}
	}

	/** Le geste que fait ce joueur, ou {@code null} ; un geste échu est oublié. */
	@Nullable
	public static Geste gesture(PlayerEntity player) {
		Geste geste = GESTES.get(player.getUuid());
		if (geste != null && !geste.geste().loops()
				&& player.getWorld().getTime() - geste.début() > geste.geste().durationTicks()) {
			GESTES.remove(player.getUuid());
			return null;
		}
		return geste;
	}

	/** Il a bougé, ou changé d'apparence : le geste cesse. */
	public static void stopGesture(PlayerEntity player) {
		GESTES.remove(player.getUuid());
	}

	public static MorphState of(PlayerEntity player) {
		return ÉTATS.getOrDefault(player.getUuid(), MorphState.NONE);
	}

	/** La sienne. */
	public static MorphState self() {
		PlayerEntity joueur = MinecraftClient.getInstance().player;
		return joueur == null ? MorphState.NONE : of(joueur);
	}

	public static void clear() {
		ÉTATS.clear();
		GESTES.clear();
	}
}
