package dev.pyrofetch.jn1.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Une photo : son cadrage, ses pixels (un indice de palette par octet) et
 * le nom de son auteur.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public record PhotoImage(PhotoFormat format, byte[] pixels, String auteur) {
	public static final Codec<PhotoImage> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(PhotoFormat.CODEC.fieldOf("format").forGetter(PhotoImage::format), Codec.BYTE_BUFFER.fieldOf("pixels").xmap(tampon -> {
			byte[] copie = new byte[tampon.remaining()];
			tampon.duplicate().get(copie);
			return copie;
		}, ByteBuffer::wrap).forGetter(PhotoImage::pixels), Codec.STRING.optionalFieldOf("author", "").forGetter(PhotoImage::auteur)).apply(instance, PhotoImage::new)
	);
	public static final PacketCodec<RegistryByteBuf, PhotoImage> PACKET_CODEC = PacketCodec.tuple(
		PhotoFormat.PACKET_CODEC, PhotoImage::format, PacketCodecs.BYTE_ARRAY, PhotoImage::pixels, PacketCodecs.STRING, PhotoImage::auteur, PhotoImage::new
	);

	public boolean estCohérente() {
		return this.pixels.length == this.format.octets();
	}

	public int largeur() {
		return this.format.largeurEnPixels();
	}

	public int hauteur() {
		return this.format.hauteurEnPixels();
	}

	public byte pixel(int x, int y) {
		return x >= 0 && y >= 0 && x < this.largeur() && y < this.hauteur() ? this.pixels[y * this.largeur() + x] : 0;
	}
}
