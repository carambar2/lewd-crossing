package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce que le joueur a demandé au terminal.
 *
 * @param action 0 : quitter la session ; 1 : faire apparaître une compagne ;
 *               2 : changer de station de radio
 * @param target identifiant de la compagne à faire venir ; vide pour les autres
 *               actions
 */
public record TerminalActionPayload(BlockPos pos, int action, String target)
		implements CustomPacketPayload {
	public static final int ACTION_CLOSE = 0;
	public static final int ACTION_SPAWN = 1;
	/** Radio.exe : {@code target} porte le numéro de station, « 0 » pour éteindre. */
	public static final int ACTION_RADIO = 2;

	public static final CustomPacketPayload.Type<TerminalActionPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("terminal_action"));

	public static TerminalActionPayload of(BlockPos pos, int action) {
		return new TerminalActionPayload(pos, action, "");
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, TerminalActionPayload::pos,
			ByteBufCodecs.VAR_INT, TerminalActionPayload::action,
			ByteBufCodecs.STRING_UTF8, TerminalActionPayload::target,
			TerminalActionPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
