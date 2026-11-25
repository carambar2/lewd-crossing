package dev.pyrofetch.jn1.capsule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Qui se trouve dans la capsule.
 *
 * <p>Le nom et l'affection sont conservés à part de {@code entityData} : ils
 * doivent être lisibles dans l'infobulle sans reconstruire l'entité, et
 * l'infobulle est dessinée côté client, qui ne verra jamais le NBT complet
 * autrement.
 *
 * @param name       le nom affiché de la captive
 * @param affection  son affection envers celui qui l'a capturée, 0 à 10
 * @param entityData son état complet, pour la relâcher intacte
 */
public record CapsuleContents(String name, int affection, CompoundTag entityData) {
	public static final Codec<CapsuleContents> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Codec.STRING.fieldOf("name").forGetter(CapsuleContents::name),
					Codec.INT.fieldOf("affection").forGetter(CapsuleContents::affection),
					CompoundTag.CODEC.fieldOf("entity").forGetter(CapsuleContents::entityData)
			).apply(instance, CapsuleContents::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CapsuleContents> PACKET_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, CapsuleContents::name,
					ByteBufCodecs.VAR_INT, CapsuleContents::affection,
					ByteBufCodecs.COMPOUND_TAG, CapsuleContents::entityData,
					CapsuleContents::new
			);
}
