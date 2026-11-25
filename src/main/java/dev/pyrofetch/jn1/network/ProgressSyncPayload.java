package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/**
 * Recopie les drapeaux du joueur vers son client : c'est eux qui décident de ce
 * que le terminal propose. L'affection n'y figure pas — elle est propre à
 * chaque compagne, et voyage avec l'écran qui la montre.
 */
public record ProgressSyncPayload(List<String> flags) implements CustomPayload {
	public static final CustomPayload.Id<ProgressSyncPayload> ID = new CustomPayload.Id<>(JN1.id("progress_sync"));

	public static final PacketCodec<RegistryByteBuf, ProgressSyncPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING.collect(PacketCodecs.toList()), ProgressSyncPayload::flags,
			ProgressSyncPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
