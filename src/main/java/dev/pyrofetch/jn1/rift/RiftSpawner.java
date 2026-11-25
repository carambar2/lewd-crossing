package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModSounds;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Ouvre une faille.
 *
 * <p>Les failles ne naissent plus au hasard, la nuit, près des joueurs : elles
 * sont tenues par les brèches des sites de confinement ({@link BreachBlock}),
 * et c'est là qu'on les trouve. Seule reste la façon d'en ouvrir une, que les
 * brèches emploient.
 */
public final class RiftSpawner {
	private RiftSpawner() {
	}

	/** Ouvre une faille dont le pied est ici ; {@code null} si l'entité n'a pu naître. */
	@Nullable
	public static RiftEntity ouvrir(ServerWorld monde, BlockPos pied) {
		RiftEntity faille = ModEntities.RIFT.create(monde);
		if (faille == null) {
			return null;
		}
		faille.refreshPositionAndAngles(pied.getX() + 0.5, pied.getY() + 0.1, pied.getZ() + 0.5, 0.0F, 0.0F);
		monde.spawnEntity(faille);
		monde.playSound(null, pied, ModSounds.RIFT_OPEN, SoundCategory.AMBIENT, 2.0F, 1.0F);
		JN1.LOGGER.debug("Faille ouverte en {}.", pied);
		return faille;
	}
}
