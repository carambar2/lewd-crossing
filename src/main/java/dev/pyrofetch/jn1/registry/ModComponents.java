package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleContents;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModComponents {
	/** Ce que contient une capsule. Absent tant qu'elle est vide. */
	public static final ComponentType<CapsuleContents> CAPSULE_CONTENTS =
			Registry.register(Registries.DATA_COMPONENT_TYPE, JN1.id("capsule_contents"),
					ComponentType.<CapsuleContents>builder()
							.codec(CapsuleContents.CODEC)
							.packetCodec(CapsuleContents.PACKET_CODEC)
							.build());

	/** Le numéro d'une photo dans l'archive du monde. */
	public static final ComponentType<Integer> PHOTO_ID =
			Registry.register(Registries.DATA_COMPONENT_TYPE, JN1.id("photo_id"),
					ComponentType.<Integer>builder()
							.codec(com.mojang.serialization.Codec.INT)
							.packetCodec(net.minecraft.network.codec.PacketCodecs.VAR_INT)
							.build());

	/** Le cadrage d'une photo : portrait, paysage, panorama… */
	public static final ComponentType<dev.pyrofetch.jn1.photo.PhotoFormat> PHOTO_FORMAT =
			Registry.register(Registries.DATA_COMPONENT_TYPE, JN1.id("photo_format"),
					ComponentType.<dev.pyrofetch.jn1.photo.PhotoFormat>builder()
							.codec(dev.pyrofetch.jn1.photo.PhotoFormat.CODEC)
							.packetCodec(dev.pyrofetch.jn1.photo.PhotoFormat.PACKET_CODEC)
							.build());

	private ModComponents() {
	}

	public static void register() {
		JN1.LOGGER.debug("Composants enregistrés.");
	}
}
