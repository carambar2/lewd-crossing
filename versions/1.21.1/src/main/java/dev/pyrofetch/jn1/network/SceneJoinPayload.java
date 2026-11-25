package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Prendre la place de {@code steve} dans la scène en cours, ou la quitter.
 * Revérifié par le serveur comme une scène.
 *
 * @param entityId la compagne visée
 * @param join     participer, ou redevenir spectateur
 */
public record SceneJoinPayload(int entityId, boolean join) implements CustomPayload {
	public static final CustomPayload.Id<SceneJoinPayload> ID = new CustomPayload.Id<>(JN1.id("scene_join"));

	public static final PacketCodec<RegistryByteBuf, SceneJoinPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SceneJoinPayload::entityId,
			PacketCodecs.BOOL, SceneJoinPayload::join,
			SceneJoinPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
