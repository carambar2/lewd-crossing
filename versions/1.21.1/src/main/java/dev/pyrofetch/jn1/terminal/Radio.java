package dev.pyrofetch.jn1.terminal;

import dev.pyrofetch.jn1.block.ComputerBlock;
import dev.pyrofetch.jn1.registry.ModSounds;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Radio.exe : le terminal joue une station, pour toute la pièce.
 *
 * <p>Le son vient du terminal lui-même, pas de l'interface : il continue quand
 * on quitte l'écran, tout le monde l'entend à l'entour, et il s'arrête si l'on
 * casse la machine. Le serveur ne fait que changer la station dans l'état de
 * bloc ; chaque client, voyant ce changement, lance ou coupe le son de son côté.
 *
 * <p>Le client branche ici sa propre routine au démarrage : le code commun ne
 * peut pas toucher au gestionnaire de sons, qui n'existe pas sur un serveur.
 */
public final class Radio {
	/** Une station : son identifiant d'affichage et ce qu'elle joue. */
	public record Station(String id, SoundEvent son) {
		public String translationKey() {
			return "radio.jn1." + this.id;
		}
	}

	/** Dans l'ordre du panneau. La dernière n'existe pas tout à fait. */
	public static final List<Station> STATIONS = List.of(
			new Station("calm", ModSounds.RADIO_CALM),
			new Station("retro", ModSounds.RADIO_RETRO),
			new Station("jazz", ModSounds.RADIO_JAZZ),
			new Station("void", ModSounds.RADIO_VOID));

	/** Ce que fait le client à chaque battement d'un terminal ; rien sur un serveur. */
	@FunctionalInterface
	public interface ClientTicker {
		void tick(World monde, BlockPos pos, BlockState état);
	}

	private static ClientTicker clientTicker = (monde, pos, état) -> { };

	private Radio() {
	}

	public static void setClientTicker(ClientTicker ticker) {
		clientTicker = ticker;
	}

	public static void clientTick(World monde, BlockPos pos, BlockState état) {
		clientTicker.tick(monde, pos, état);
	}

	/** La station d'un terminal, ou {@code null} si sa radio est éteinte. */
	public static Station station(BlockState état) {
		if (!(état.getBlock() instanceof ComputerBlock)) {
			return null;
		}
		int numéro = état.get(ComputerBlock.STATION);
		return numéro == 0 ? null : STATIONS.get(numéro - 1);
	}
}
