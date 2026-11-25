package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Recopie les drapeaux du joueur vers son client : c'est eux qui décident de ce
 * que le terminal propose. L'affection n'y figure pas — elle est propre à
 * chaque compagne, et voyage avec l'écran qui la montre.
 */
public record ProgressSyncPayload(List<String> flags) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ProgressSyncPayload> ID = new CustomPacketPayload.Type<>(JN1.id("progress_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ProgressSyncPayload::flags,
			ProgressSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
