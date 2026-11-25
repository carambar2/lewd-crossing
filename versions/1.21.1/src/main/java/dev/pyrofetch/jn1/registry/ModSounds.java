package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

/**
 * Les sons du mod.
 *
 * <p>Presque tous sont synthétisés par {@code tools/gen_music.py}. Exceptions :
 * les voix des filles (le mod d'origine) et l'achat chez Gaster (Deltarune). Les fichiers eux-mêmes sont décrits dans
 * {@code assets/jn1/sounds.json} ; ici ne vivent que les identifiants.
 *
 * <p>Tous sont mono sauf la musique du vide, qui ne vient d'aucun point de
 * l'espace. Un son stéréo n'est pas spatialisé : une radio stéréo s'entendrait
 * à pleine force depuis l'autre bout du monde.
 */
public final class ModSounds {
	/** La musique de la dimension : trois morceaux, tirés au hasard. */
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_VOID = reference("music.void");

	public static final SoundEvent RIFT_IDLE = register("entity.rift.idle");
	public static final SoundEvent RIFT_OPEN = register("entity.rift.open");
	public static final SoundEvent RIFT_ENTER = register("entity.rift.enter");
	public static final SoundEvent RIFT_EJECT = register("entity.rift.eject");

	public static final SoundEvent ELEVATOR_START = register("entity.elevator.start");
	public static final SoundEvent ELEVATOR_MOVE = register("entity.elevator.move");
	public static final SoundEvent ELEVATOR_DING = register("entity.elevator.ding");
	public static final SoundEvent ELEVATOR_DOOR_OPEN = register("block.elevator_door.open");
	public static final SoundEvent ELEVATOR_DOOR_CLOSE = register("block.elevator_door.close");

	/** Le lidar : l'impulsion qu'il émet, et l'écho qui lui revient. */
	public static final SoundEvent LIDAR_PING = register("item.lidar.ping");
	public static final SoundEvent LIDAR_ECHO = register("item.lidar.echo");

	public static final SoundEvent VENDING_BUY = register("block.vending_machine.buy");
	public static final SoundEvent VENDING_DENY = register("block.vending_machine.deny");

	/** L'achat chez Gaster : le bruit de la boutique de Deltarune ({@code snd_locker}), mis en mono. */
	public static final SoundEvent GASTER_SHOP_BUY = register("gaster.shop.buy");

	public static final SoundEvent NEON_ON = register("block.neon.on");
	public static final SoundEvent NEON_OFF = register("block.neon.off");

	/** Les stations de Radio.exe, dans l'ordre où l'écran les propose. */
	public static final SoundEvent RADIO_CALM = register("radio.calm");
	public static final SoundEvent RADIO_RETRO = register("radio.retro");
	public static final SoundEvent RADIO_JAZZ = register("radio.jazz");
	public static final SoundEvent RADIO_VOID = register("radio.void");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		return Registry.register(Registries.SOUND_EVENT, JN1.id(path), SoundEvent.of(JN1.id(path)));
	}

	/** La musique doit être tenue par une entrée de registre : c'est ce que réclame MusicSound. */
	private static RegistryEntry.Reference<SoundEvent> reference(String path) {
		return Registry.registerReference(Registries.SOUND_EVENT, JN1.id(path), SoundEvent.of(JN1.id(path)));
	}

	public static void register() {
		JN1.LOGGER.debug("Sons enregistrés.");
	}
}
