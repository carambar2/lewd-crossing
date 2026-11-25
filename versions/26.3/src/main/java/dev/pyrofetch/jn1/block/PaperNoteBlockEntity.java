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

/**
 * Ce qu'une note a d'écrit dessus.
 *
 * <p>Le texte est gardé tel quel, ligne par ligne, plutôt que sous forme de clé
 * de traduction : ces notes sont faites pour être posées dans les décors, et
 * l'on doit pouvoir en écrire une nouvelle sans toucher au code ni aux fichiers
 * de langue. Une commande suffit :
 *
 * <pre>/setblock ~ ~ ~ jn1:note[face=wall,facing=north]{lines:["Première ligne","Seconde"]}</pre>
 *
 * <p>Les lignes voyagent jusqu'au client par le paquet de mise à jour de bloc :
 * sans cela, la page s'ouvrirait vide chez le joueur, l'entité de bloc n'étant
 * pas synchronisée d'office.
 */
public class PaperNoteBlockEntity extends BlockEntity implements TextSupport {
	private static final String CLÉ = "lines";
	private static final String VERROU = "locked";
	/** Au-delà, la page déborderait de la feuille dessinée à l'écran. */
	public static final int LIGNES_MAX = 14;

	private List<String> lignes = new ArrayList<>();
	/**
	 * Une note verrouillée ne se réécrit plus.
	 *
	 * <p>C'est ce qui distingue le brouillon qu'on laisse traîner du document
	 * qu'on a voulu figer : la première se rouvre en écriture, la seconde ne
	 * s'ouvre plus qu'en lecture.
	 */
	private boolean verrouillée;

	public PaperNoteBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.NOTE, pos, state);
	}

	public List<String> lignes() {
		return this.lignes;
	}

	public boolean verrouillée() {
		return this.verrouillée;
	}

	public void setLignes(List<String> lignes, boolean verrou) {
		this.lignes = new ArrayList<>(lignes.subList(0, Math.min(lignes.size(), LIGNES_MAX)));
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
			if (this.lignes.size() >= LIGNES_MAX) {
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
