package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Ce que le menu d'un sélecteur demande : aller à un étage ({@link #ALLER},
 * {@code valeur} = la couche), régler le décalage ({@link #DÉCALER}), nommer
 * l'étage ({@link #NOMMER}, {@code texte}).
 */
public record SelectorActionPayload(BlockPos pos, int action, int valeur, String texte) implements CustomPayload {
	public static final int ALLER = 0;
	public static final int DÉCALER = 1;
	public static final int NOMMER = 2;

	public static final CustomPayload.Id<SelectorActionPayload> ID = new CustomPayload.Id<>(JN1.id("lift_selector_action"));
	public static final PacketCodec<RegistryByteBuf, SelectorActionPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, SelectorActionPayload::pos,
			PacketCodecs.VAR_INT, SelectorActionPayload::action,
			PacketCodecs.VAR_INT, SelectorActionPayload::valeur,
			PacketCodecs.string(64), SelectorActionPayload::texte,
			SelectorActionPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
