package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import dev.pyrofetch.jn1.network.SaveNotePayload;

/**
 * Ce qu'affiche un écran mural.
 *
 * <p>Même contenu et mêmes règles qu'une note — lignes, verrou, synchronisation
 * par le paquet de mise à jour de bloc —, sans quoi l'écran s'allumerait vide
 * chez le joueur alors que le serveur a bien le texte. Seul le coin supérieur
 * gauche d'un ensemble s'en sert ; les autres gardent le leur, qui ressurgit si
 * l'ensemble est coupé en deux.
 */
public class WallScreenBlockEntity extends BlockEntity implements TextSupport {
	private static final String CLÉ = "lines";
	private static final String VERROU = "locked";

	private List<String> lignes = new ArrayList<>();
	private boolean verrouillée;

	public WallScreenBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WALL_SCREEN, pos, state);
	}

	@Override
	public List<String> lignes() {
		return this.lignes;
	}

	@Override
	public boolean verrouillée() {
		return this.verrouillée;
	}

	@Override
	public void setLignes(List<String> lignes, boolean verrou) {
		// Pas plus de lignes que l'écran n'en montre, ni de caractères qu'une
		// ligne n'en tient (deux points au moins chacun, le plus étroit).
		int[] capacité = {SaveNotePayload.LARGEUR_MAX * 2, SaveNotePayload.LIGNES_MAX};
		if (this.world != null && this.getCachedState().getBlock() instanceof WallScreenBlock) {
			capacité = WallScreenBlock.capacité(WallScreenBlock.taille(this.world, this.pos,
					this.getCachedState().get(WallScreenBlock.FACING)));
		}
		this.lignes = new ArrayList<>();
		for (String ligne : lignes.subList(0, Math.min(lignes.size(), capacité[1]))) {
			this.lignes.add(ligne.length() > capacité[0] / 2 ? ligne.substring(0, capacité[0] / 2) : ligne);
		}
		this.verrouillée = verrou;
		this.markDirty();
		if (this.world != null && !this.world.isClient) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
		}
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		this.lignes = new ArrayList<>();
		this.verrouillée = nbt.getBoolean(VERROU);
		NbtList liste = nbt.getList(CLÉ, NbtElement.STRING_TYPE);
		for (int i = 0; i < Math.min(liste.size(), SaveNotePayload.LIGNES_MAX); i++) {
			this.lignes.add(liste.getString(i));
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		NbtList liste = new NbtList();
		for (String ligne : this.lignes) {
			liste.add(NbtString.of(ligne));
		}
		nbt.put(CLÉ, liste);
		nbt.putBoolean(VERROU, this.verrouillée);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return this.createNbt(registries);
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}
}
