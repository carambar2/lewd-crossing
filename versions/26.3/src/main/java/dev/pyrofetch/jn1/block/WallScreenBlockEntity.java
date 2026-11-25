package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
		if (this.level != null && this.getBlockState().getBlock() instanceof WallScreenBlock) {
			capacité = WallScreenBlock.capacité(WallScreenBlock.taille(this.level, this.worldPosition,
					this.getBlockState().getValue(WallScreenBlock.FACING)));
		}
		this.lignes = new ArrayList<>();
		for (String ligne : lignes.subList(0, Math.min(lignes.size(), capacité[1]))) {
			this.lignes.add(ligne.length() > capacité[0] / 2 ? ligne.substring(0, capacité[0] / 2) : ligne);
		}
		this.verrouillée = verrou;
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.lignes = new ArrayList<>();
		this.verrouillée = nbt.getBooleanOr(VERROU, false);
		for (String ligne : nbt.listOrEmpty(CLÉ, Codec.STRING)) {
			if (this.lignes.size() >= SaveNotePayload.LIGNES_MAX) {
				break;
			}
			this.lignes.add(ligne);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		ValueOutput.TypedOutputList<String> liste = nbt.list(CLÉ, Codec.STRING);
		for (String ligne : this.lignes) {
			liste.add(ligne);
		}
		nbt.putBoolean(VERROU, this.verrouillée);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
