package dev.pyrofetch.jn1.worldgen;

import com.mojang.datafixers.util.Pair;
import dev.pyrofetch.jn1.JN1;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Un monde neuf fait apparaître le joueur près d'une maison, à moins de
 * 100 blocs (0.56.7).
 *
 * <p>Les maisons sont rares — une région de 34 chunks, et un terrain plat et
 * sec à trouver ({@link HouseStructure}) : dans le monde éprouvé, la plus
 * proche était à 582 blocs, et un nouveau joueur ne voyait rien du mod. À la
 * création du monde, après que le jeu a choisi son point d'apparition, on
 * cherche la maison la plus proche (comme {@code /locate}, sur
 * {@link #RAYON} chunks) et l'on pose le point d'apparition devant elle, sur
 * un sol ferme, à {@link #RECUL} blocs de son emprise, tourné vers elle. Le
 * jeu disperse encore les joueurs d'une dizaine de blocs autour : la maison
 * reste à portée de vue. Sans maison trouvée, on garde le choix du jeu.
 * Rien ne change pour un monde déjà créé.
 */
public final class HouseSpawn {
	private static final String[] MAISONS = {"house_plains", "house_desert", "house_savanna", "house_taiga", "house_snowy"};
	/** En chunks : une centaine de blocs au-delà de l'espacement des maisons. */
	private static final int RAYON = 100;
	private static final int RECUL = 8;

	private HouseSpawn() {
	}

	public static void placer(ServerWorld monde) {
		Registry<Structure> registre = monde.getRegistryManager().get(RegistryKeys.STRUCTURE);
		List<RegistryEntry<Structure>> maisons = new ArrayList<>();
		for (String nom : MAISONS) {
			registre.getEntry(JN1.id(nom)).ifPresent(maisons::add);
		}
		if (maisons.isEmpty()) {
			return;
		}
		BlockPos départ = monde.getSpawnPos();
		Pair<BlockPos, RegistryEntry<Structure>> trouvée = monde.getChunkManager().getChunkGenerator()
				.locateStructure(monde, RegistryEntryList.of(maisons), départ, RAYON, false);
		if (trouvée == null) {
			JN1.LOGGER.info("Apparition : aucune maison à {} chunks de {}, point du jeu gardé", RAYON, départ);
			return;
		}
		BlockPos où = trouvée.getFirst();
		ChunkPos chunk = new ChunkPos(où);
		StructureStart début = monde.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS)
				.getStructureStart(trouvée.getSecond().value());
		BlockBox boîte = début != null && début.hasChildren() ? début.getBoundingBox()
				: new BlockBox(où.getX() - 6, où.getY(), où.getZ() - 6, où.getX() + 6, où.getY() + 8, où.getZ() + 6);
		int cx = (boîte.getMinX() + boîte.getMaxX()) / 2;
		int cz = (boîte.getMinZ() + boîte.getMaxZ()) / 2;
		// Les quatre côtés, de RECUL à deux fois plus loin de l'emprise : le
		// premier sol ferme et sec, où l'on tient debout (herbe haute permise).
		List<int[]> côtés = new ArrayList<>();
		for (int recul = RECUL; recul <= RECUL * 2; recul += 4) {
			côtés.add(new int[] {cx, boîte.getMaxZ() + recul});
			côtés.add(new int[] {cx, boîte.getMinZ() - recul});
			côtés.add(new int[] {boîte.getMaxX() + recul, cz});
			côtés.add(new int[] {boîte.getMinX() - recul, cz});
		}
		for (int[] c : côtés) {
			// Le chunk n'est pas encore chargé à la création du monde : `getTopY`
			// rendrait le fond du monde. On le génère, puis on lit sa hauteur.
			int y = monde.getChunk(c[0] >> 4, c[1] >> 4)
					.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, c[0] & 15, c[1] & 15) + 1;
			BlockPos pieds = new BlockPos(c[0], y, c[1]);
			BlockState sol = monde.getBlockState(pieds.down());
			if (!sol.getFluidState().isEmpty() || !sol.isSolidBlock(monde, pieds.down())
					|| !libre(monde, pieds) || !libre(monde, pieds.up())) {
				continue;
			}
			float cap = (float) Math.toDegrees(Math.atan2(-(cx + 0.5 - (c[0] + 0.5)), cz + 0.5 - (c[1] + 0.5)));
			monde.setSpawnPos(pieds, cap);
			JN1.LOGGER.info("Apparition près de la maison {} : {} (au lieu de {})",
					trouvée.getSecond().getKey().map(k -> k.getValue().toString()).orElse("?"), pieds, départ);
			return;
		}
		JN1.LOGGER.info("Apparition : maison en {} ({}), mais pas de sol ferme autour ; point du jeu gardé",
				où, boîte);
	}

	private static boolean libre(ServerWorld monde, BlockPos pos) {
		BlockState état = monde.getBlockState(pos);
		return état.getFluidState().isEmpty() && état.getCollisionShape(monde, pos).isEmpty();
	}
}
