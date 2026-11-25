package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Le menu d'un sélecteur d'étage : les étages de sa plateforme, où elle est,
 * et le réglage de ce sélecteur. Le serveur compose tout ; le client affiche
 * et renvoie un choix, que le serveur revérifie.
 *
 * @param plateforme le nombre de blocs de la plateforme desservie ; 0 : aucune
 * @param actuel     la couche où elle se tient, ou celle qu'elle vise en route
 * @param enRoute    vrai si elle roule
 * @param décalage   où ce sélecteur l'arrête, par rapport à lui
 * @param niveau     la couche que ce sélecteur demande (sa hauteur + le décalage)
 */
public record OpenSelectorPayload(BlockPos pos, List<Floor> étages, int plateforme, int actuel, boolean enRoute,
                                  int décalage, int niveau, String nom) implements CustomPayload {
	public static final CustomPayload.Id<OpenSelectorPayload> ID = new CustomPayload.Id<>(JN1.id("open_lift_selector"));

	public record Floor(int niveau, Optional<Text> nom) {
		public static final PacketCodec<RegistryByteBuf, Floor> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, Floor::niveau,
				TextCodecs.OPTIONAL_PACKET_CODEC, Floor::nom,
				Floor::new);
	}

	public static final PacketCodec<RegistryByteBuf, OpenSelectorPayload> CODEC = new PacketCodec<>() {
		@Override
		public OpenSelectorPayload decode(RegistryByteBuf buf) {
			return new OpenSelectorPayload(BlockPos.PACKET_CODEC.decode(buf),
					Floor.CODEC.collect(PacketCodecs.toList(64)).decode(buf),
					PacketCodecs.VAR_INT.decode(buf), PacketCodecs.VAR_INT.decode(buf), PacketCodecs.BOOL.decode(buf),
					PacketCodecs.VAR_INT.decode(buf), PacketCodecs.VAR_INT.decode(buf), PacketCodecs.STRING.decode(buf));
		}

		@Override
		public void encode(RegistryByteBuf buf, OpenSelectorPayload p) {
			BlockPos.PACKET_CODEC.encode(buf, p.pos());
			Floor.CODEC.collect(PacketCodecs.toList(64)).encode(buf, p.étages());
			PacketCodecs.VAR_INT.encode(buf, p.plateforme());
			PacketCodecs.VAR_INT.encode(buf, p.actuel());
			PacketCodecs.BOOL.encode(buf, p.enRoute());
			PacketCodecs.VAR_INT.encode(buf, p.décalage());
			PacketCodecs.VAR_INT.encode(buf, p.niveau());
			PacketCodecs.STRING.encode(buf, p.nom());
		}
	};

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
