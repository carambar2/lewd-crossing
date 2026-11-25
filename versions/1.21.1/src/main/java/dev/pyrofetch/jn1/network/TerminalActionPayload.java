package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Ce que le joueur a demandé au terminal.
 *
 * @param action 0 : quitter la session ; 1 : faire apparaître une compagne ;
 *               2 : changer de station de radio
 * @param target identifiant de la compagne à faire venir ; vide pour les autres
 *               actions
 */
public record TerminalActionPayload(BlockPos pos, int action, String target)
		implements CustomPayload {
	public static final int ACTION_CLOSE = 0;
	public static final int ACTION_SPAWN = 1;
	/** Radio.exe : {@code target} porte le numéro de station, « 0 » pour éteindre. */
	public static final int ACTION_RADIO = 2;

	public static final CustomPayload.Id<TerminalActionPayload> ID =
			new CustomPayload.Id<>(JN1.id("terminal_action"));

	public static TerminalActionPayload of(BlockPos pos, int action) {
		return new TerminalActionPayload(pos, action, "");
	}

	public static final PacketCodec<RegistryByteBuf, TerminalActionPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, TerminalActionPayload::pos,
			PacketCodecs.VAR_INT, TerminalActionPayload::action,
			PacketCodecs.STRING, TerminalActionPayload::target,
			TerminalActionPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
