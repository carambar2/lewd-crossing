package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Un geste fait par un joueur transformé, relayé à ceux qui le voient. */
public record MorphGesturePayload(UUID player, String gesture) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MorphGesturePayload> ID = new CustomPacketPayload.Type<>(JN1.id("morph_gesture"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MorphGesturePayload> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, MorphGesturePayload::player,
			ByteBufCodecs.STRING_UTF8, MorphGesturePayload::gesture,
			MorphGesturePayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
