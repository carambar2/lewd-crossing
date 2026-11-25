package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Ce qu'un bloc de plateforme retient : l'apparence qu'il imite (un état de
 * bloc, ou rien), et le groupe auquel il a été assemblé (rien : il est seul).
 * Synchronisé : le client en tire son dessin ({@link #getRenderData}).
 */
public class PlatformBlockEntity extends BlockEntity {
	@Nullable
	private BlockState apparence;
	@Nullable
	private UUID groupe;

	public PlatformBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIFT_PLATFORM, pos, state);
	}

	@Nullable
	public BlockState apparence() {
		return this.apparence;
	}

	@Nullable
	public UUID groupe() {
		return this.groupe;
	}

	public void imiter(@Nullable BlockState apparence) {
		this.apparence = apparence;
		this.changé();
	}

	public void grouper(@Nullable UUID groupe) {
		this.groupe = groupe;
		this.changé();
	}

	/** Reprend ce qu'une plateforme en route portait pour ce bloc. */
	public void reprendre(@Nullable BlockState apparence, @Nullable UUID groupe) {
		this.apparence = apparence;
		this.groupe = groupe;
		this.changé();
	}

	private void changé() {
		this.markDirty();
		if (this.world != null) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
		}
	}

	/** Le client : l'apparence, que le modèle camouflé lit pour se dessiner. */
	@Override
	public Object getRenderData() {
		return this.apparence;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (this.apparence != null) {
			nbt.put("Look", NbtHelper.fromBlockState(this.apparence));
		}
		if (this.groupe != null) {
			nbt.putUuid("Group", this.groupe);
		}
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		this.apparence = nbt.contains("Look")
				? NbtHelper.toBlockState(registries.getWrapperOrThrow(RegistryKeys.BLOCK), nbt.getCompound("Look"))
				: null;
		this.groupe = nbt.containsUuid("Group") ? nbt.getUuid("Group") : null;
		// Côté client, l'apparence a changé : le chunk doit se redessiner.
		if (this.world != null && this.world.isClient()) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
		}
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return this.createNbt(registries);
	}

	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}
}
