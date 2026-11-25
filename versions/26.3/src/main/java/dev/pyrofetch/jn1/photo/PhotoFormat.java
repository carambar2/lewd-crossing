package dev.pyrofetch.jn1.photo;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Les cadrages possibles, mesurés en cases de bloc : carré, portrait,
 * portrait large, paysage, panorama. Chaque case fait {@link #PIXELS_PAR_CASE}
 * pixels de côté.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public enum PhotoFormat implements StringRepresentable {
	CARRÉ("square", 1, 1),
	PORTRAIT("portrait", 1, 2),
	PORTRAIT_LARGE("wide_portrait", 2, 3),
	PAYSAGE("landscape", 3, 2),
	PANORAMA("panorama", 2, 1);

	public static final int PIXELS_PAR_CASE = 256;
	public static final Codec<PhotoFormat> CODEC = StringRepresentable.fromEnum(PhotoFormat::values);
	public static final StreamCodec<ByteBuf, PhotoFormat> PACKET_CODEC = ByteBufCodecs.idMapper(indice -> values()[indice], Enum::ordinal);
	private final String nom;
	private final int cases_en_largeur;
	private final int cases_en_hauteur;

	PhotoFormat(String nom, int casesEnLargeur, int casesEnHauteur) {
		this.nom = nom;
		this.cases_en_largeur = casesEnLargeur;
		this.cases_en_hauteur = casesEnHauteur;
	}

	public int casesEnLargeur() {
		return this.cases_en_largeur;
	}

	public int casesEnHauteur() {
		return this.cases_en_hauteur;
	}

	public int cases() {
		return this.cases_en_largeur * this.cases_en_hauteur;
	}

	public int largeurEnPixels() {
		return this.cases_en_largeur * 256;
	}

	public int hauteurEnPixels() {
		return this.cases_en_hauteur * 256;
	}

	public int octets() {
		return this.largeurEnPixels() * this.hauteurEnPixels();
	}

	public float rapport() {
		return (float)this.cases_en_largeur / this.cases_en_hauteur;
	}

	public PhotoFormat suivant() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	public String translationKey() {
		return "photo.jn1.format." + this.nom;
	}

	@Override
	public String getSerializedName() {
		return this.nom;
	}
}
