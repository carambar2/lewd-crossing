package dev.pyrofetch.jn1.photo;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * D'où une photo a été prise : la caméra telle que le client l'a rendue (à la
 * main, à la troisième personne ou depuis le trépied), et l'ouverture du cadre
 * une fois recadré au format — en tangentes des demi-angles, horizontal et
 * vertical. C'est ce qui permet au serveur de dire qui est sur la photo
 * ({@link dev.pyrofetch.jn1.onlyfaps.Sujets}).
 */
public record PhotoPose(Vector3f œil, float lacet, float tangage, float ouvertureH, float ouvertureV) {
	/** Une pose qui ne voit rien : celle d'un client qui n'en envoie pas. */
	public static final PhotoPose AUCUNE = new PhotoPose(new Vector3f(), 0, 0, 0, 0);

	public static final PacketCodec<RegistryByteBuf, PhotoPose> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VECTOR3F, PhotoPose::œil,
			PacketCodecs.FLOAT, PhotoPose::lacet,
			PacketCodecs.FLOAT, PhotoPose::tangage,
			PacketCodecs.FLOAT, PhotoPose::ouvertureH,
			PacketCodecs.FLOAT, PhotoPose::ouvertureV,
			PhotoPose::new
	);

	public Vec3d position() {
		return new Vec3d(this.œil.x, this.œil.y, this.œil.z);
	}

	public boolean vide() {
		return this.ouvertureH <= 0 || this.ouvertureV <= 0;
	}
}
