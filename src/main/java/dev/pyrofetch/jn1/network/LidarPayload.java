package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Les échos d'un balayage de lidar : où sont les structures du vide, les plus
 * proches d'abord.
 */
public record LidarPayload(List<BlockPos> échos) implements CustomPayload {
	public static final CustomPayload.Id<LidarPayload> ID = new CustomPayload.Id<>(JN1.id("lidar"));

	public static final PacketCodec<RegistryByteBuf, LidarPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), LidarPayload::échos,
			LidarPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
