package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ordre d'ouvrir le viseur : à main levée, ou depuis un trépied.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record OpenCameraPayload(boolean surPied, BlockPos pied) implements CustomPacketPayload {
	public static final Type<OpenCameraPayload> ID = new Type<>(JN1.id("open_camera"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenCameraPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, OpenCameraPayload::surPied, BlockPos.STREAM_CODEC, OpenCameraPayload::pied, OpenCameraPayload::new
	);

	public static OpenCameraPayload enMain() {
		return new OpenCameraPayload(false, BlockPos.ZERO);
	}

	public static OpenCameraPayload surPied(BlockPos pos) {
		return new OpenCameraPayload(true, pos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
