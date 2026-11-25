package dev.pyrofetch.jn1.gaster;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

/**
 * Les arbres derrière lesquels Gaster attend déjà, par ancre de laboratoire.
 *
 * <p>Tant qu'il n'a pas disparu, il ne paraît pas une seconde fois. Chercher l'entité ne suffit pas : l'arbre est à une vingtaine de
 * blocs de l'arrivée, et les entités d'un chunk qu'on vient de charger n'y sont
 * pas encore. On tient donc la liste dans la sauvegarde du vide ; il s'en retire
 * en partant ({@link GasterEntity#remove}).
 */
public class GasterState extends PersistentState {
	private static final String NOM = "jn1_gaster";
	private static final String CLÉ = "waiting";

	public static final Type<GasterState> TYPE = new Type<>(GasterState::new, GasterState::lire, null);

	private final LongSet arbres = new LongOpenHashSet();

	public static GasterState of(ServerWorld vide) {
		return vide.getPersistentStateManager().getOrCreate(TYPE, NOM);
	}

	private static long clé(BlockPos ancre) {
		return ChunkPos.toLong(ancre.getX(), ancre.getZ());
	}

	public boolean attend(BlockPos ancre) {
		return this.arbres.contains(clé(ancre));
	}

	public void marquer(BlockPos ancre) {
		if (this.arbres.add(clé(ancre))) {
			this.markDirty();
		}
	}

	public void libérer(BlockPos ancre) {
		if (this.arbres.remove(clé(ancre))) {
			this.markDirty();
		}
	}

	private static GasterState lire(NbtCompound nbt, WrapperLookup registres) {
		GasterState état = new GasterState();
		for (long clé : nbt.getLongArray(CLÉ)) {
			état.arbres.add(clé);
		}
		return état;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registres) {
		nbt.putLongArray(CLÉ, this.arbres.toLongArray());
		return nbt;
	}
}
