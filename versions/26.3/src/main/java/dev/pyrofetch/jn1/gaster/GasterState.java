package dev.pyrofetch.jn1.gaster;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Les arbres derrière lesquels Gaster attend déjà, par ancre de laboratoire.
 *
 * <p>Tant qu'il n'a pas disparu, il ne paraît pas une seconde fois. Chercher l'entité ne suffit pas : l'arbre est à une vingtaine de
 * blocs de l'arrivée, et les entités d'un chunk qu'on vient de charger n'y sont
 * pas encore. On tient donc la liste dans la sauvegarde du vide ; il s'en retire
 * en partant ({@link GasterEntity#remove}).
 */
public class GasterState extends SavedData {
	/** Le fichier de la 1.21.1, dans les données du vide : {@code jn1_gaster.dat}. */
	private static final String NOM = "jn1_gaster";
	private static final String CLÉ = "waiting";

	// 26.3 : une donnée sauvegardée est un type (identifiant, codec) ; le contenu
	// garde la même clé (« waiting », un tableau de longs).
	private static final com.mojang.serialization.Codec<GasterState> CODEC =
			net.minecraft.nbt.CompoundTag.CODEC.xmap(GasterState::lire, état -> état.save(new CompoundTag()));
	public static final net.minecraft.world.level.saveddata.SavedDataType<GasterState> TYPE =
			new net.minecraft.world.level.saveddata.SavedDataType<>(dev.pyrofetch.jn1.JN1.id("gaster"),
					GasterState::new, CODEC, null);

	private final LongSet arbres = new LongOpenHashSet();

	public static GasterState of(ServerLevel vide) {
		return dev.pyrofetch.jn1.config.AnciennesDonnees.charger(vide.getDataStorage(), TYPE,
				dev.pyrofetch.jn1.config.AnciennesDonnees.dossier(vide).resolve(NOM + ".dat"), vide.registryAccess());
	}

	private static long clé(BlockPos ancre) {
		return ChunkPos.pack(ancre.getX(), ancre.getZ());
	}

	public boolean attend(BlockPos ancre) {
		return this.arbres.contains(clé(ancre));
	}

	public void marquer(BlockPos ancre) {
		if (this.arbres.add(clé(ancre))) {
			this.setDirty();
		}
	}

	public void libérer(BlockPos ancre) {
		if (this.arbres.remove(clé(ancre))) {
			this.setDirty();
		}
	}

	private static GasterState lire(CompoundTag nbt) {
		GasterState état = new GasterState();
		for (long clé : nbt.getLongArray(CLÉ).orElse(new long[0])) {
			état.arbres.add(clé);
		}
		return état;
	}

	private CompoundTag save(CompoundTag nbt) {
		nbt.putLongArray(CLÉ, this.arbres.toLongArray());
		return nbt;
	}
}
