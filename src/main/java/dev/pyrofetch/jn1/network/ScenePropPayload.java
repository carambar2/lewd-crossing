package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Le réglage de la télécommande : dessiner ou non la pièce de scène. Revérifié
 * par le serveur comme une scène.
 *
 * @param entityId la compagne visée
 * @param visible  le nouvel état
 */
public record ScenePropPayload(int entityId, boolean visible) implements CustomPayload {
	public static final CustomPayload.Id<ScenePropPayload> ID =
			new CustomPayload.Id<>(JN1.id("scene_prop"));

	public static final PacketCodec<RegistryByteBuf, ScenePropPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ScenePropPayload::entityId,
			PacketCodecs.BOOL, ScenePropPayload::visible,
			ScenePropPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
