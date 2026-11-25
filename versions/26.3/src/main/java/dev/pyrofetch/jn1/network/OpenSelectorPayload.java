package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
                                  int décalage, int niveau, String nom) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenSelectorPayload> ID = new CustomPacketPayload.Type<>(JN1.id("open_lift_selector"));

	public record Floor(int niveau, Optional<Component> nom) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Floor> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Floor::niveau,
				ComponentSerialization.OPTIONAL_STREAM_CODEC, Floor::nom,
				Floor::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenSelectorPayload> CODEC = new StreamCodec<>() {
		@Override
		public OpenSelectorPayload decode(RegistryFriendlyByteBuf buf) {
			return new OpenSelectorPayload(BlockPos.STREAM_CODEC.decode(buf),
					Floor.CODEC.apply(ByteBufCodecs.list(64)).decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf));
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, OpenSelectorPayload p) {
			BlockPos.STREAM_CODEC.encode(buf, p.pos());
			Floor.CODEC.apply(ByteBufCodecs.list(64)).encode(buf, p.étages());
			ByteBufCodecs.VAR_INT.encode(buf, p.plateforme());
			ByteBufCodecs.VAR_INT.encode(buf, p.actuel());
			ByteBufCodecs.BOOL.encode(buf, p.enRoute());
			ByteBufCodecs.VAR_INT.encode(buf, p.décalage());
			ByteBufCodecs.VAR_INT.encode(buf, p.niveau());
			ByteBufCodecs.STRING_UTF8.encode(buf, p.nom());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
