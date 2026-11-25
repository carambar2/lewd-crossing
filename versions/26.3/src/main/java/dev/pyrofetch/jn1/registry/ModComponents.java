package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleContents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
	/** Ce que contient une capsule. Absent tant qu'elle est vide. */
	public static final DataComponentType<CapsuleContents> CAPSULE_CONTENTS =
			Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JN1.id("capsule_contents"),
					DataComponentType.<CapsuleContents>builder()
							.persistent(CapsuleContents.CODEC)
							.networkSynchronized(CapsuleContents.PACKET_CODEC)
							.build());

	/** Le numéro d'une photo dans l'archive du monde. */
	public static final DataComponentType<Integer> PHOTO_ID =
			Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JN1.id("photo_id"),
					DataComponentType.<Integer>builder()
							.persistent(com.mojang.serialization.Codec.INT)
							.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
							.build());

	/** Le cadrage d'une photo : portrait, paysage, panorama… */
	public static final DataComponentType<dev.pyrofetch.jn1.photo.PhotoFormat> PHOTO_FORMAT =
			Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JN1.id("photo_format"),
					DataComponentType.<dev.pyrofetch.jn1.photo.PhotoFormat>builder()
							.persistent(dev.pyrofetch.jn1.photo.PhotoFormat.CODEC)
							.networkSynchronized(dev.pyrofetch.jn1.photo.PhotoFormat.PACKET_CODEC)
							.build());

	private ModComponents() {
	}

	public static void register() {
		JN1.LOGGER.debug("Composants enregistrés.");
	}
}
