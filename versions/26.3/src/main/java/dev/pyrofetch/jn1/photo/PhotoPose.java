package dev.pyrofetch.jn1.photo;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
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

	public static final StreamCodec<RegistryFriendlyByteBuf, PhotoPose> PACKET_CODEC = StreamCodec.composite(
			// 26.3 : le codec du jeu est en Vector3fc ; la pose garde un Vector3f.
			ByteBufCodecs.VECTOR3F.map(Vector3f::new, v -> v), PhotoPose::œil,
			ByteBufCodecs.FLOAT, PhotoPose::lacet,
			ByteBufCodecs.FLOAT, PhotoPose::tangage,
			ByteBufCodecs.FLOAT, PhotoPose::ouvertureH,
			ByteBufCodecs.FLOAT, PhotoPose::ouvertureV,
			PhotoPose::new
	);

	public Vec3 position() {
		return new Vec3(this.œil.x, this.œil.y, this.œil.z);
	}

	public boolean vide() {
		return this.ouvertureH <= 0 || this.ouvertureV <= 0;
	}
}
