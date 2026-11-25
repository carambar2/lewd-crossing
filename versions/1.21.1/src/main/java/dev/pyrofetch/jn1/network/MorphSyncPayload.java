package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/**
 * L'apparence d'un joueur, envoyée à lui-même et à ceux qui le voient.
 *
 * <p>Par UUID et non par identifiant réseau : celui-ci change à la
 * réapparition, l'UUID jamais.
 */
public record MorphSyncPayload(UUID player, String kind, String outfit, boolean pending, int boobs, int booty)
		implements CustomPayload {
	public static final CustomPayload.Id<MorphSyncPayload> ID = new CustomPayload.Id<>(JN1.id("morph_sync"));

	public static final PacketCodec<RegistryByteBuf, MorphSyncPayload> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MorphSyncPayload::player,
			PacketCodecs.STRING, MorphSyncPayload::kind,
			PacketCodecs.STRING, MorphSyncPayload::outfit,
			PacketCodecs.BOOL, MorphSyncPayload::pending,
			PacketCodecs.VAR_INT, MorphSyncPayload::boobs,
			PacketCodecs.VAR_INT, MorphSyncPayload::booty,
			MorphSyncPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
