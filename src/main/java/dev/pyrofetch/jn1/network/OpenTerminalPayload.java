package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Ordre d'entrer en session devant un terminal.
 *
 * @param pos    le bloc concerné : la caméra viendra se placer devant
 * @param facing sa direction, pour savoir de quel côté est l'écran
 */
public record OpenTerminalPayload(BlockPos pos, int facing) implements CustomPayload {
	public static final CustomPayload.Id<OpenTerminalPayload> ID =
			new CustomPayload.Id<>(JN1.id("open_terminal"));

	public static final PacketCodec<RegistryByteBuf, OpenTerminalPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenTerminalPayload::pos,
			PacketCodecs.VAR_INT, OpenTerminalPayload::facing,
			OpenTerminalPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
