package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.minecraft.block.DoorBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Le passage entre la cave et le laboratoire.
 *
 * <p>Le laboratoire n'est pas caché quelque part sous la maison : il est dans sa
 * propre dimension, le vide, qui double la surface aux mêmes x et z. Chaque
 * maison y a le sien, à l'aplomb de sa porte de cave (voir {@link VoidLabs}) ;
 * c'est ce qui rend impossible d'y tomber par hasard en creusant.
 */
public final class LabDimension {
	public static final RegistryKey<World> LAB =
			RegistryKey.of(RegistryKeys.WORLD, JN1.id("lab"));

	private LabDimension() {
	}

	/**
	 * Franchit la porte, dans un sens ou dans l'autre.
	 *
	 * @param facing la direction que regarde la porte ; on ressort dos à elle
	 */
	public static void cross(ServerPlayerEntity player, BlockPos door, Direction facing) {
		if (player.getWorld().getRegistryKey().equals(LAB)) {
			sortir(player, door);
		} else {
			enter(player, door, facing);
		}
	}

	private static void enter(ServerPlayerEntity player, BlockPos door, Direction facing) {
		ServerWorld lab = player.getServer().getWorld(LAB);
		if (lab == null) {
			JN1.LOGGER.error("La dimension du laboratoire est absente : porte inerte.");
			return;
		}
		// Où revenir. Un pas en arrière de la porte, sinon on ressort dedans et
		// la traversée repart aussitôt.
		BlockPos retour = door.offset(facing);
		PlayerProgress progress = ModAttachments.get(player);
		ModAttachments.set(player, progress.withLabReturn(
				player.getWorld().getRegistryKey().getValue().toString(), retour));

		BlockPos ancre = VoidLabs.ancre(player.getWorld(), door);
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
	private static void sortir(ServerPlayerEntity player, BlockPos door) {
		ServerWorld surface = player.getServer().getOverworld();
		BlockPos ancre = VoidLabs.ancre(player.getWorld(), door);
		Optional<BlockPos> cave = VoidLabs.porteDeLaMaison(surface, ancre);
		if (cave.isEmpty()) {
			leave(player);
			return;
		}
		Direction regard = surface.getBlockState(cave.get()).get(DoorBlock.FACING);
		send(player, surface, Vec3d.ofBottomCenter(cave.get().offset(regard)), regard.asRotation());
	}

	/**
	 * Ressort du vide, là d'où l'on était venu.
	 *
	 * <p>Par le fond : on y est renvoyé en tombant (voir {@link VoidFall}).
	 */
	public static void leave(ServerPlayerEntity player) {
		PlayerProgress progress = ModAttachments.get(player);
		ServerWorld cible = null;
		BlockPos retour = progress.labReturnPos();

		if (progress.labReturnWorld() != null) {
			net.minecraft.util.Identifier id =
					net.minecraft.util.Identifier.tryParse(progress.labReturnWorld());
			if (id != null) {
				cible = player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
			}
		}
		// Sans repère utilisable — première visite par commande, monde effacé —
		// on renvoie au point d'apparition plutôt que de laisser le joueur
		// enfermé.
		if (cible == null || retour == null) {
			cible = player.getServer().getOverworld();
			retour = cible.getSpawnPos();
		}
		send(player, cible, Vec3d.ofBottomCenter(retour), player.getYaw());
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
	public static void enterThroughRift(ServerPlayerEntity player, Vec3d arrivée, float yaw,
	                                    String retourMonde, BlockPos retour) {
		ServerWorld lab = player.getServer().getWorld(LAB);
		if (lab == null) {
			JN1.LOGGER.error("La dimension du laboratoire est absente : faille inerte.");
			return;
		}
		PlayerProgress progress = ModAttachments.get(player);
		ModAttachments.set(player, progress.withLabReturn(retourMonde, retour));
		player.teleport(lab, arrivée.x, arrivée.y, arrivée.z, java.util.Set.of(), yaw, 0.0F);
		player.setVelocity(Vec3d.ZERO);
		player.fallDistance = 0.0F;
	}

	private static void send(ServerPlayerEntity player, ServerWorld monde, Vec3d pos, float yaw) {
		player.teleport(monde, pos.x, pos.y, pos.z, java.util.Set.of(), yaw, 0.0F);
		monde.playSound(null, BlockPos.ofFloored(pos), SoundEvents.BLOCK_IRON_DOOR_OPEN,
				SoundCategory.BLOCKS, 0.7F, 0.6F);
	}
}
