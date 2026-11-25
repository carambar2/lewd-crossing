package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.math.BlockPos;

/**
 * Ordre d'ouvrir le viseur : à main levée, ou depuis un trépied.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record OpenCameraPayload(boolean surPied, BlockPos pied) implements CustomPayload {
	public static final Id<OpenCameraPayload> ID = new Id<>(JN1.id("open_camera"));
	public static final PacketCodec<RegistryByteBuf, OpenCameraPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOL, OpenCameraPayload::surPied, BlockPos.PACKET_CODEC, OpenCameraPayload::pied, OpenCameraPayload::new
	);

	public static OpenCameraPayload enMain() {
		return new OpenCameraPayload(false, BlockPos.ORIGIN);
	}

	public static OpenCameraPayload surPied(BlockPos pos) {
		return new OpenCameraPayload(true, pos);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
