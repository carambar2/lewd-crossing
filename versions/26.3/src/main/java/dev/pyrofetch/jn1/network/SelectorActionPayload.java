package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce que le menu d'un sélecteur demande : aller à un étage ({@link #ALLER},
 * {@code valeur} = la couche), régler le décalage ({@link #DÉCALER}), nommer
 * l'étage ({@link #NOMMER}, {@code texte}).
 */
public record SelectorActionPayload(BlockPos pos, int action, int valeur, String texte) implements CustomPacketPayload {
	public static final int ALLER = 0;
	public static final int DÉCALER = 1;
	public static final int NOMMER = 2;

	public static final CustomPacketPayload.Type<SelectorActionPayload> ID = new CustomPacketPayload.Type<>(JN1.id("lift_selector_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SelectorActionPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, SelectorActionPayload::pos,
			ByteBufCodecs.VAR_INT, SelectorActionPayload::action,
			ByteBufCodecs.VAR_INT, SelectorActionPayload::valeur,
			ByteBufCodecs.stringUtf8(64), SelectorActionPayload::texte,
			SelectorActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
