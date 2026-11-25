package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
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

import java.util.ArrayList;
import java.util.List;

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
		for (int i = 0; i < Math.min(liste.size(), LIGNES_MAX); i++) {
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
