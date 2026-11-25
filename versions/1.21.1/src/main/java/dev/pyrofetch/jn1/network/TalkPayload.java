package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

/**
 * Ce qu'elle a répondu à une interaction de « Talk » ({@code interaction/Conversation}).
 *
 * @param entityId  la compagne
 * @param geste     l'interaction ({@code chat}, {@code flirt}…)
 * @param réussi    bien prise, ou elle l'a remballé
 * @param réponse   sa réplique
 * @param affection l'affection du joueur pour elle, après coup
 */
public record TalkPayload(int entityId, String geste, boolean réussi, Text réponse, int affection)
		implements CustomPayload {
	public static final CustomPayload.Id<TalkPayload> ID = new CustomPayload.Id<>(JN1.id("talk"));

	public static final PacketCodec<RegistryByteBuf, TalkPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, TalkPayload::entityId,
			PacketCodecs.STRING, TalkPayload::geste,
			PacketCodecs.BOOL, TalkPayload::réussi,
			TextCodecs.REGISTRY_PACKET_CODEC, TalkPayload::réponse,
			PacketCodecs.VAR_INT, TalkPayload::affection,
			TalkPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
