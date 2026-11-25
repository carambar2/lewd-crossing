package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Prendre la place de {@code steve} dans la scène en cours, ou la quitter.
 * Revérifié par le serveur comme une scène.
 *
 * @param entityId la compagne visée
 * @param join     participer, ou redevenir spectateur
 */
public record SceneJoinPayload(int entityId, boolean join) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SceneJoinPayload> ID = new CustomPacketPayload.Type<>(JN1.id("scene_join"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SceneJoinPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SceneJoinPayload::entityId,
			ByteBufCodecs.BOOL, SceneJoinPayload::join,
			SceneJoinPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
