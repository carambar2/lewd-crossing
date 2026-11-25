package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce qu'elle a répondu à une interaction de « Talk » ({@code interaction/Conversation}).
 *
 * @param entityId  la compagne
 * @param geste     l'interaction ({@code chat}, {@code flirt}…)
 * @param réussi    bien prise, ou elle l'a remballé
 * @param réponse   sa réplique
 * @param affection l'affection du joueur pour elle, après coup
 */
public record TalkPayload(int entityId, String geste, boolean réussi, Component réponse, int affection)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TalkPayload> ID = new CustomPacketPayload.Type<>(JN1.id("talk"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TalkPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TalkPayload::entityId,
			ByteBufCodecs.STRING_UTF8, TalkPayload::geste,
			ByteBufCodecs.BOOL, TalkPayload::réussi,
			ComponentSerialization.STREAM_CODEC, TalkPayload::réponse,
			ByteBufCodecs.VAR_INT, TalkPayload::affection,
			TalkPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
