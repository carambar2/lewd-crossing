package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le réglage de la télécommande : dessiner ou non la pièce de scène. Revérifié
 * par le serveur comme une scène.
 *
 * @param entityId la compagne visée
 * @param visible  le nouvel état
 */
public record ScenePropPayload(int entityId, boolean visible) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ScenePropPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("scene_prop"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ScenePropPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ScenePropPayload::entityId,
			ByteBufCodecs.BOOL, ScenePropPayload::visible,
			ScenePropPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
