package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Les échos d'un balayage de lidar : où sont les structures du vide, les plus
 * proches d'abord.
 */
public record LidarPayload(List<BlockPos> échos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LidarPayload> ID = new CustomPacketPayload.Type<>(JN1.id("lidar"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LidarPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), LidarPayload::échos,
			LidarPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
