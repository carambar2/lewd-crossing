package dev.pyrofetch.jn1.onlyfaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Une fille sur une photo, telle qu'elle était à l'instant de la prise.
 *
 * @param fille   sa sorte ({@code jenny}…)
 * @param tenue   le modèle porté
 * @param rang    la place de la tenue dans sa liste, de la plus habillée (0) à la plus nue
 * @param tenues  combien elle en a
 * @param scène   la scène jouée, vide sinon
 * @param traces  0 à 3
 * @param menottée
 * @param cadrage la part de la hauteur du cadre qu'elle occupe (1 : en pied, plein cadre)
 */
public record Sujet(String fille, String tenue, int rang, int tenues, String scène, int traces,
                    boolean menottée, float cadrage) {
	public static final Codec<Sujet> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("girl").forGetter(Sujet::fille),
			Codec.STRING.fieldOf("outfit").forGetter(Sujet::tenue),
			Codec.INT.fieldOf("rank").forGetter(Sujet::rang),
			Codec.INT.fieldOf("outfits").forGetter(Sujet::tenues),
			Codec.STRING.optionalFieldOf("scene", "").forGetter(Sujet::scène),
			Codec.INT.optionalFieldOf("traces", 0).forGetter(Sujet::traces),
			Codec.BOOL.optionalFieldOf("cuffed", false).forGetter(Sujet::menottée),
			Codec.FLOAT.fieldOf("framing").forGetter(Sujet::cadrage)
	).apply(i, Sujet::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Sujet> PACKET_CODEC = StreamCodec.ofMember(
			(sujet, tampon) -> {
				tampon.writeUtf(sujet.fille);
				tampon.writeUtf(sujet.tenue);
				tampon.writeVarInt(sujet.rang);
				tampon.writeVarInt(sujet.tenues);
				tampon.writeUtf(sujet.scène);
				tampon.writeVarInt(sujet.traces);
				tampon.writeBoolean(sujet.menottée);
				tampon.writeFloat(sujet.cadrage);
			},
			tampon -> new Sujet(tampon.readUtf(), tampon.readUtf(), tampon.readVarInt(), tampon.readVarInt(),
					tampon.readUtf(), tampon.readVarInt(), tampon.readBoolean(), tampon.readFloat()));

	public boolean enScène() {
		return !this.scène.isEmpty();
	}

	/** De 0 (la plus habillée) à 1 (la plus nue) ; une fille à une seule tenue compte pour un tiers. */
	public float nudité() {
		return this.tenues <= 1 ? 1f / 3f : this.rang / (float) (this.tenues - 1);
	}
}
