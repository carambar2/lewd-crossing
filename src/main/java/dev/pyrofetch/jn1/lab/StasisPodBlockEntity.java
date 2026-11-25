package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

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
		this.markDirty();
		if (this.world != null) {
			// L'état du bloc porte l'occupation : c'est lui qui choisit le
			// vitrage, et il doit donc suivre le contenu.
			BlockState etat = this.getCachedState()
					.with(dev.pyrofetch.jn1.block.StasisPodBlock.OCCUPIED, companion != null);
			this.world.setBlockState(this.pos, etat, 3);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (this.occupant != null) {
			nbt.putString(KEY_OCCUPANT, this.occupant);
		}
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		this.occupant = nbt.contains(KEY_OCCUPANT) ? nbt.getString(KEY_OCCUPANT) : null;
	}

	/** Le contenu doit voyager jusqu'au client, qui s'en sert pour le rendu. */
	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return this.createNbt(registries);
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}
}
