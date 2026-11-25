package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Un choix fait dans l'écran de transformation. Le serveur revérifie tout.
 *
 * @param action   {@link #BECOME}, {@link #OUTFIT}, {@link #REVERT} ou {@link #GESTURE}
 * @param argument la compagne visée pour {@link #BECOME}, la tenue pour {@link #OUTFIT},
 *                 le geste pour {@link #GESTURE}
 */
public record MorphRequestPayload(int action, String argument) implements CustomPacketPayload {
	/** Prendre l'apparence d'une compagne, ou passer de l'une à l'autre. */
	public static final int BECOME = 0;
	/** Changer de tenue. */
	public static final int OUTFIT = 1;
	/** Redevenir soi-même. */
	public static final int REVERT = 2;
	/** Faire un geste ({@link dev.pyrofetch.jn1.morph.MorphGesture}), vu de tous. */
	public static final int GESTURE = 3;

	public static final CustomPacketPayload.Type<MorphRequestPayload> ID = new CustomPacketPayload.Type<>(JN1.id("morph_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MorphRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, MorphRequestPayload::action,
			ByteBufCodecs.STRING_UTF8, MorphRequestPayload::argument,
			MorphRequestPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
