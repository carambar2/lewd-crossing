package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * L'apparence d'un joueur, envoyée à lui-même et à ceux qui le voient.
 *
 * <p>Par UUID et non par identifiant réseau : celui-ci change à la
 * réapparition, l'UUID jamais.
 */
public record MorphSyncPayload(UUID player, String kind, String outfit, boolean pending, int boobs, int booty)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MorphSyncPayload> ID = new CustomPacketPayload.Type<>(JN1.id("morph_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MorphSyncPayload> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, MorphSyncPayload::player,
			ByteBufCodecs.STRING_UTF8, MorphSyncPayload::kind,
			ByteBufCodecs.STRING_UTF8, MorphSyncPayload::outfit,
			ByteBufCodecs.BOOL, MorphSyncPayload::pending,
			ByteBufCodecs.VAR_INT, MorphSyncPayload::boobs,
			ByteBufCodecs.VAR_INT, MorphSyncPayload::booty,
			MorphSyncPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
