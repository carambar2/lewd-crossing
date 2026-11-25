package dev.pyrofetch.jn1.lab;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.Codec;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.config.AnciennesDonnees;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.LongStream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Les laboratoires du vide déjà bâtis, par ancre.
 *
 * <p>Le premier laboratoire se reconnaissait à son terminal : un bloc témoin
 * suffisait pour une salle unique. Avec un laboratoire par maison, casser ce
 * terminal ferait rebâtir la salle par-dessus ce que le joueur y a changé.
 * On tient donc la liste, dans la sauvegarde du vide.
 */
public class VoidLabState extends SavedData {
	private static final String NOM = "jn1_void_labs";
	private static final String CLÉ = "built";

	/** Le fichier tel qu'il était : {@code {built: [L; …]}}. */
	private static final Codec<VoidLabState> CODEC = Codec.LONG_STREAM.optionalFieldOf(CLÉ)
			.xmap(VoidLabState::lire, état -> Optional.of(Arrays.stream(état.bâtis.toLongArray())))
			.codec();

	/**
	 * 26.3 : une donnée sauvegardée est désignée par un identifiant, rangée dans
	 * {@code data/jn1/void_labs.dat} ; l'ancien fichier ({@code data/jn1_void_labs.dat})
	 * est repris une fois, voir {@link AnciennesDonnees}.
	 */
	public static final SavedDataType<VoidLabState> TYPE =
			new SavedDataType<>(JN1.id("void_labs"), VoidLabState::new, CODEC, null);

	/** Seuls x et z comptent : on les range comme une position de chunk. */
	private final LongSet bâtis = new LongOpenHashSet();

	public static VoidLabState of(ServerLevel vide) {
		return AnciennesDonnees.charger(vide.getDataStorage(), TYPE,
				AnciennesDonnees.dossier(vide).resolve(NOM + ".dat"), vide.registryAccess());
	}

	private static long clé(BlockPos ancre) {
		return ChunkPos.pack(ancre.getX(), ancre.getZ());
	}

	public boolean bâti(BlockPos ancre) {
		return this.bâtis.contains(clé(ancre));
	}

	public void marquer(BlockPos ancre) {
		if (this.bâtis.add(clé(ancre))) {
			this.setDirty();
		}
	}

	private static VoidLabState lire(Optional<LongStream> clés) {
		VoidLabState état = new VoidLabState();
		clés.ifPresent(flux -> flux.forEach(état.bâtis::add));
		return état;
	}
}
