package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Une scène choisie à la télécommande. Le serveur revérifie la compagne, la
 * distance, l'objet en main et le nom avant d'obéir.
 *
 * @param entityId la compagne visée
 * @param scène    le nom court de la scène, ou vide pour l'arrêter
 */
public record ScenePayload(int entityId, String scène) implements CustomPayload {
	public static final CustomPayload.Id<ScenePayload> ID =
			new CustomPayload.Id<>(JN1.id("scene"));

	public static final PacketCodec<RegistryByteBuf, ScenePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ScenePayload::entityId,
			PacketCodecs.STRING, ScenePayload::scène,
			ScenePayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
