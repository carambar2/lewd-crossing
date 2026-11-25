package dev.pyrofetch.jn1.lab;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

/**
 * Les laboratoires du vide déjà bâtis, par ancre.
 *
 * <p>Le premier laboratoire se reconnaissait à son terminal : un bloc témoin
 * suffisait pour une salle unique. Avec un laboratoire par maison, casser ce
 * terminal ferait rebâtir la salle par-dessus ce que le joueur y a changé.
 * On tient donc la liste, dans la sauvegarde du vide.
 */
public class VoidLabState extends PersistentState {
	private static final String NOM = "jn1_void_labs";
	private static final String CLÉ = "built";

	public static final Type<VoidLabState> TYPE = new Type<>(VoidLabState::new, VoidLabState::lire, null);

	/** Seuls x et z comptent : on les range comme une position de chunk. */
	private final LongSet bâtis = new LongOpenHashSet();

	public static VoidLabState of(ServerWorld vide) {
		return vide.getPersistentStateManager().getOrCreate(TYPE, NOM);
	}

	private static long clé(BlockPos ancre) {
		return ChunkPos.toLong(ancre.getX(), ancre.getZ());
	}

	public boolean bâti(BlockPos ancre) {
		return this.bâtis.contains(clé(ancre));
	}

	public void marquer(BlockPos ancre) {
		if (this.bâtis.add(clé(ancre))) {
			this.markDirty();
		}
	}

	private static VoidLabState lire(NbtCompound nbt, WrapperLookup registres) {
		VoidLabState état = new VoidLabState();
		for (long clé : nbt.getLongArray(CLÉ)) {
			état.bâtis.add(clé);
		}
		return état;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registres) {
		nbt.putLongArray(CLÉ, this.bâtis.toLongArray());
		return nbt;
	}
}
