package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ordre d'entrer en session devant un terminal.
 *
 * @param pos    le bloc concerné : la caméra viendra se placer devant
 * @param facing sa direction, pour savoir de quel côté est l'écran
 */
public record OpenTerminalPayload(BlockPos pos, int facing) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenTerminalPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("open_terminal"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenTerminalPayload::pos,
			ByteBufCodecs.VAR_INT, OpenTerminalPayload::facing,
			OpenTerminalPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
