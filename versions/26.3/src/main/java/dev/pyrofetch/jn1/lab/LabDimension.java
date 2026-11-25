package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Le passage entre la cave et le laboratoire.
 *
 * <p>Le laboratoire n'est pas caché quelque part sous la maison : il est dans sa
 * propre dimension, le vide, qui double la surface aux mêmes x et z. Chaque
 * maison y a le sien, à l'aplomb de sa porte de cave (voir {@link VoidLabs}) ;
 * c'est ce qui rend impossible d'y tomber par hasard en creusant.
 */
public final class LabDimension {
	public static final ResourceKey<Level> LAB =
			ResourceKey.create(Registries.DIMENSION, JN1.id("lab"));

	private LabDimension() {
	}

	/**
	 * Franchit la porte, dans un sens ou dans l'autre.
	 *
	 * @param facing la direction que regarde la porte ; on ressort dos à elle
	 */
	public static void cross(ServerPlayer player, BlockPos door, Direction facing) {
		if (player.level().dimension().equals(LAB)) {
			sortir(player, door);
		} else {
			enter(player, door, facing);
		}
	}

	private static void enter(ServerPlayer player, BlockPos door, Direction facing) {
		ServerLevel lab = player.level().getServer().getLevel(LAB);
		if (lab == null) {
			JN1.LOGGER.error("La dimension du laboratoire est absente : porte inerte.");
			return;
		}
		// Où revenir. Un pas en arrière de la porte, sinon on ressort dedans et
		// la traversée repart aussitôt.
		BlockPos retour = door.relative(facing);
		PlayerProgress progress = ModAttachments.get(player);
		ModAttachments.set(player, progress.withLabReturn(
				player.level().dimension().identifier().toString(), retour));

		BlockPos ancre = VoidLabs.ancre(player.level(), door);
		VoidLabs.bâtir(lab, ancre);
		send(player, lab, VoidLabs.arrivée(ancre), VoidLabs.REGARD);
		dev.pyrofetch.jn1.gaster.GasterEntity.apparaître(lab, ancre, false);
	}

	/**
	 * Ressort par la porte d'un laboratoire : dans la cave de la maison qui est
	 * au-dessus, aux mêmes x et z.
	 *
	 * <p>Qu'on soit entré par elle, par une autre maison ou par une faille, c'est
	 * ce laboratoire-ci qu'on quitte, et c'est donc sa maison qu'on retrouve. À
	 * défaut de maison — le premier laboratoire, bâti avant que chaque maison ait
	 * le sien —, on revient d'où l'on était parti.
	 */
	private static void sortir(ServerPlayer player, BlockPos door) {
		ServerLevel surface = player.level().getServer().overworld();
		BlockPos ancre = VoidLabs.ancre(player.level(), door);
		Optional<BlockPos> cave = VoidLabs.porteDeLaMaison(surface, ancre);
		if (cave.isEmpty()) {
			leave(player);
			return;
		}
		Direction regard = surface.getBlockState(cave.get()).getValue(DoorBlock.FACING);
		send(player, surface, Vec3.atBottomCenterOf(cave.get().relative(regard)), regard.toYRot());
	}

	/**
	 * Ressort du vide, là d'où l'on était venu.
	 *
	 * <p>Par le fond : on y est renvoyé en tombant (voir {@link VoidFall}).
	 */
	public static void leave(ServerPlayer player) {
		PlayerProgress progress = ModAttachments.get(player);
		ServerLevel cible = null;
		BlockPos retour = progress.labReturnPos();

		if (progress.labReturnWorld() != null) {
			net.minecraft.resources.Identifier id =
					net.minecraft.resources.Identifier.tryParse(progress.labReturnWorld());
			if (id != null) {
				cible = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
			}
		}
		// Sans repère utilisable — première visite par commande, monde effacé —
		// on renvoie au point d'apparition plutôt que de laisser le joueur
		// enfermé.
		if (cible == null || retour == null) {
			cible = player.level().getServer().overworld();
			retour = cible.getRespawnData().pos();
		}
		send(player, cible, Vec3.atBottomCenterOf(retour), player.getYRot());
	}

	/**
	 * Entre dans le vide par une faille : pas sur un chemin, mais en plein vide,
	 * aux mêmes x et z que la faille.
	 *
	 * <p>Aucun laboratoire n'y est bâti d'office : il y en a un sous chaque
	 * maison, et c'est au lidar de dire où.
	 *
	 * @param retourMonde le monde où ressortir
	 * @param retour      où ressortir — pas sur la faille, sans quoi on y
	 *                    retomberait aussitôt
	 */
	public static void enterThroughRift(ServerPlayer player, Vec3 arrivée, float yaw,
	                                    String retourMonde, BlockPos retour) {
		ServerLevel lab = player.level().getServer().getLevel(LAB);
		if (lab == null) {
			JN1.LOGGER.error("La dimension du laboratoire est absente : faille inerte.");
			return;
		}
		PlayerProgress progress = ModAttachments.get(player);
		ModAttachments.set(player, progress.withLabReturn(retourMonde, retour));
		player.teleportTo(lab, arrivée.x, arrivée.y, arrivée.z, java.util.Set.of(), yaw, 0.0F, true);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
	}

	private static void send(ServerPlayer player, ServerLevel monde, Vec3 pos, float yaw) {
		player.teleportTo(monde, pos.x, pos.y, pos.z, java.util.Set.of(), yaw, 0.0F, true);
		monde.playSound(null, BlockPos.containing(pos), SoundEvents.IRON_DOOR_OPEN,
				SoundSource.BLOCKS, 0.7F, 0.6F);
	}
}
