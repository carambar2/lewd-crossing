package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.block.LabDoorBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Surveille qui se tient dans une porte du laboratoire.
 *
 * <p>La détection se fait au tick plutôt que par {@code onEntityCollision} :
 * ce dernier n'est pas appelé pour ce bloc, y compris quand une entité le
 * traverse de part en part — vérifié en poussant un mob au travers, sans
 * qu'aucun appel n'arrive. Regarder où sont les joueurs est plus fruste, mais
 * ne dépend d'aucun détail de forme de collision.
 */
public final class LabDoorWatcher {
	/**
	 * Délai avant qu'un même joueur puisse repasser.
	 *
	 * <p>On ressort à un pas d'une porte, et sans ce verrou le pas suivant
	 * relancerait la traversée : on ferait l'aller-retour sans fin.
	 */
	public static final int COOLDOWN_TICKS = 40;

	private static final Map<UUID, Integer> DERNIER_PASSAGE = new HashMap<>();
	/** Ceux dont le dernier passage s'est fait par la porte elle-même (et non par commande). */
	private static final java.util.Set<UUID> PAR_LA_PORTE = new java.util.HashSet<>();

	private LabDoorWatcher() {
	}

	public static void register() {
		// Les ticks du serveur repartent de zéro à chaque monde ouvert : un
		// passage noté dans le précédent bloquait la porte pendant des heures.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
				server -> {
					DERNIER_PASSAGE.clear();
					PAR_LA_PORTE.clear();
				});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int maintenant = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				verifier(player, maintenant);
			}
			// Purge des joueurs partis, pour que la table ne gonfle pas.
			if (maintenant % 1200 == 0) {
				DERNIER_PASSAGE.entrySet().removeIf(
						e -> maintenant - e.getValue() > COOLDOWN_TICKS * 4);
				PAR_LA_PORTE.retainAll(DERNIER_PASSAGE.keySet());
			}
		});
	}

	/**
	 * Note un passage fait autrement que par la porte ({@code /jn1 lab}) : la
	 * porte ne le relance pas avant le délai, comme après le sien.
	 */
	public static void noterPassage(ServerPlayer player) {
		DERNIER_PASSAGE.put(player.getUUID(), player.level().getServer().getTickCount());
		PAR_LA_PORTE.remove(player.getUUID());
	}

	/**
	 * Les ticks écoulés depuis que ce joueur a passé une porte ouverte en s'y
	 * tenant ; très grand si son dernier passage n'était pas celui d'une porte.
	 */
	public static int depuisLaPorte(ServerPlayer player) {
		Integer dernier = DERNIER_PASSAGE.get(player.getUUID());
		if (dernier == null || !PAR_LA_PORTE.contains(player.getUUID())) {
			return Integer.MAX_VALUE;
		}
		return player.level().getServer().getTickCount() - dernier;
	}

	/**
	 * La porte du laboratoire où se tient ce joueur, ou à un pas de lui (ses
	 * pieds, sa tête, et les quatre cases voisines à ces hauteurs) ; {@code null}
	 * s'il n'y en a pas. Ouverte ou fermée.
	 *
	 * <p>Pour {@code /jn1 lab} : « debout dans la porte », le joueur est souvent
	 * à cheval sur elle et un bloc voisin, et sa position de bloc n'est pas
	 * toujours la porte. Pris pour une ancre, ce bloc voisin faisait bâtir un
	 * second laboratoire, décalé d'un bloc, par-dessus le vrai (vu en 26.3).
	 */
	@org.jetbrains.annotations.Nullable
	public static BlockPos porteProche(ServerPlayer player) {
		BlockPos pieds = player.blockPosition();
		for (BlockPos niveau : new BlockPos[] {pieds, pieds.above()}) {
			if (player.level().getBlockState(niveau).getBlock() instanceof LabDoorBlock) {
				return niveau;
			}
		}
		for (net.minecraft.core.Direction côté : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			for (BlockPos niveau : new BlockPos[] {pieds, pieds.above()}) {
				BlockPos voisin = niveau.relative(côté);
				if (player.level().getBlockState(voisin).getBlock() instanceof LabDoorBlock) {
					return voisin;
				}
			}
		}
		return null;
	}

	private static void verifier(ServerPlayer player, int maintenant) {
		Integer dernier = DERNIER_PASSAGE.get(player.getUUID());
		if (dernier != null && maintenant - dernier < COOLDOWN_TICKS) {
			return;
		}

		// Aux pieds comme à hauteur d'épaules : selon l'assiette du terrain, le
		// joueur peut n'avoir qu'une de ses deux moitiés dans l'embrasure.
		BlockPos pieds = player.blockPosition();
		for (BlockPos pos : new BlockPos[] {pieds, pieds.above()}) {
			BlockState state = player.level().getBlockState(pos);
			if (!(state.getBlock() instanceof LabDoorBlock) || !state.getValue(DoorBlock.OPEN)) {
				continue;
			}
			DERNIER_PASSAGE.put(player.getUUID(), maintenant);
			PAR_LA_PORTE.add(player.getUUID());
			LabDimension.cross(player, pos, state.getValue(DoorBlock.FACING));
			return;
		}
	}
}
