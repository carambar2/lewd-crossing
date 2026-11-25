package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Un tube de stase : c'est là qu'une compagne attend d'être appelée.
 *
 * <p>Le tube retient le nom de celle qu'il garde. Le terminal ne fait pas venir
 * les compagnes de nulle part : il vide un tube, et c'est ce tube qui les
 * relâche. Sans tube à portée, l'écran n'a rien à commander.
 */
public class StasisPodBlockEntity extends BlockEntity {
	private static final String KEY_OCCUPANT = "occupant";

	/** Le nom de la compagne en attente, ou {@code null} si le tube est vide. */
	private String occupant;

	public StasisPodBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STASIS_POD, pos, state);
	}

	public String occupant() {
		return this.occupant;
	}

	public boolean isEmpty() {
		return this.occupant == null;
	}

	public void setOccupant(String companion) {
		this.occupant = companion;
		this.setChanged();
		if (this.level != null) {
			// L'état du bloc porte l'occupation : c'est lui qui choisit le
			// vitrage, et il doit donc suivre le contenu.
			BlockState etat = this.getBlockState()
					.setValue(dev.pyrofetch.jn1.block.StasisPodBlock.OCCUPIED, companion != null);
			this.level.setBlock(this.worldPosition, etat, 3);
		}
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput nbt) {
		super.saveAdditional(nbt);
		if (this.occupant != null) {
			nbt.putString(KEY_OCCUPANT, this.occupant);
		}
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput nbt) {
		super.loadAdditional(nbt);
		this.occupant = nbt.getString(KEY_OCCUPANT).orElse(null);
	}

	/** Le contenu doit voyager jusqu'au client, qui s'en sert pour le rendu. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
