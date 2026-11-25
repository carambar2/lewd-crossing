package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Une scène choisie à la télécommande. Le serveur revérifie la compagne, la
 * distance, l'objet en main et le nom avant d'obéir.
 *
 * @param entityId la compagne visée
 * @param scène    le nom court de la scène, ou vide pour l'arrêter
 */
public record ScenePayload(int entityId, String scène) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ScenePayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("scene"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ScenePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ScenePayload::entityId,
			ByteBufCodecs.STRING_UTF8, ScenePayload::scène,
			ScenePayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
