package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.block.LabDoorBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

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
	private static final int COOLDOWN_TICKS = 40;

	private static final Map<UUID, Integer> DERNIER_PASSAGE = new HashMap<>();

	private LabDoorWatcher() {
	}

	public static void register() {
		// Les ticks du serveur repartent de zéro à chaque monde ouvert : un
		// passage noté dans le précédent bloquait la porte pendant des heures.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
				server -> DERNIER_PASSAGE.clear());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int maintenant = server.getTicks();
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				verifier(player, maintenant);
			}
			// Purge des joueurs partis, pour que la table ne gonfle pas.
			if (maintenant % 1200 == 0) {
				DERNIER_PASSAGE.entrySet().removeIf(
						e -> maintenant - e.getValue() > COOLDOWN_TICKS * 4);
			}
		});
	}

	private static void verifier(ServerPlayerEntity player, int maintenant) {
		Integer dernier = DERNIER_PASSAGE.get(player.getUuid());
		if (dernier != null && maintenant - dernier < COOLDOWN_TICKS) {
			return;
		}

		// Aux pieds comme à hauteur d'épaules : selon l'assiette du terrain, le
		// joueur peut n'avoir qu'une de ses deux moitiés dans l'embrasure.
		BlockPos pieds = player.getBlockPos();
		for (BlockPos pos : new BlockPos[] {pieds, pieds.up()}) {
			BlockState state = player.getWorld().getBlockState(pos);
			if (!(state.getBlock() instanceof LabDoorBlock) || !state.get(DoorBlock.OPEN)) {
				continue;
			}
			DERNIER_PASSAGE.put(player.getUuid(), maintenant);
			LabDimension.cross(player, pos, state.get(DoorBlock.FACING));
			return;
		}
	}
}
