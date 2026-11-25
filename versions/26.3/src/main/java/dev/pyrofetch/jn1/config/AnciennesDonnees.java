package dev.pyrofetch.jn1.config;

import dev.pyrofetch.jn1.JN1;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Reprend les données sauvegardées d'un monde joué en 1.21.1.
 *
 * <p>En 26.3, une donnée sauvegardée ({@link SavedData}) est désignée par un
 * identifiant, et son fichier rangé sous son espace de noms :
 * {@code jn1:photos} s'écrit dans {@code data/jn1/photos.dat}. La 1.21.1 les
 * nommait d'une simple chaîne, à plat : {@code data/jn1_photos.dat}. Le jeu ne
 * déplace que ses propres fichiers à la mise à niveau d'un monde ; les nôtres
 * restent où ils étaient. Faute de fichier au nouvel endroit, on lit donc
 * l'ancien une fois — même contenu, mêmes clés —, et la donnée est réécrite au
 * nouvel endroit à la prochaine sauvegarde.
 */
public final class AnciennesDonnees {
	private AnciennesDonnees() {
	}

	/**
	 * La donnée de ce type ; à défaut, celle que la 1.21.1 avait écrite dans
	 * {@code ancien} ; à défaut encore, une neuve.
	 */
	public static <T extends SavedData> T charger(SavedDataStorage stockage, SavedDataType<T> type, Path ancien,
	                                              HolderLookup.Provider registres) {
		T lue = stockage.get(type);
		if (lue != null) {
			return lue;
		}
		T reprise = lire(type, ancien, registres);
		if (reprise != null) {
			// `set` la marque à réécrire : elle passera au nouvel endroit.
			stockage.set(type, reprise);
			JN1.LOGGER.info("Données {} reprises de l'ancien fichier {}", type.id(), ancien);
			return reprise;
		}
		return stockage.computeIfAbsent(type);
	}

	/** Le dossier de données d'une dimension, tel que la 1.21.1 et la 26.3 le placent toutes deux. */
	public static Path dossier(ServerLevel monde) {
		Path racine = monde.getServer().getWorldPath(LevelResource.ROOT);
		return DimensionType.getStorageFolder(monde.dimension(), racine).resolve("data");
	}

	/**
	 * Le dossier {@code data/} à la racine du monde : celui du monde principal
	 * en 1.21.1, celui des données communes au serveur en 26.3
	 * ({@link MinecraftServer#getDataStorage()}).
	 */
	public static Path dossierRacine(MinecraftServer serveur) {
		return serveur.getWorldPath(LevelResource.DATA);
	}

	@Nullable
	private static <T extends SavedData> T lire(SavedDataType<T> type, Path ancien, HolderLookup.Provider registres) {
		if (!Files.isRegularFile(ancien)) {
			return null;
		}
		try {
			CompoundTag fichier = NbtIo.readCompressed(ancien, NbtAccounter.unlimitedHeap());
			Tag données = fichier.get("data");
			if (données == null) {
				return null;
			}
			return type.codec().parse(registres.createSerializationContext(NbtOps.INSTANCE), données)
					.resultOrPartial(erreur -> JN1.LOGGER.error("Ancien fichier {} illisible : {}", ancien, erreur))
					.orElse(null);
		} catch (IOException e) {
			JN1.LOGGER.error("Ancien fichier {} illisible", ancien, e);
			return null;
		}
	}
}
