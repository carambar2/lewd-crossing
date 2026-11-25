package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	/** Le client : l'apparence, que le modèle camouflé lit pour se dessiner. */
	@Override
	public Object getRenderData() {
		return this.apparence;
	}

	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		if (this.apparence != null) {
			nbt.store("Look", CompoundTag.CODEC, NbtUtils.writeBlockState(this.apparence));
		}
		nbt.storeNullable("Group", UUIDUtil.CODEC, this.groupe);
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.apparence = nbt.read("Look", CompoundTag.CODEC)
				.map(look -> NbtUtils.readBlockState(BuiltInRegistries.BLOCK, look))
				.orElse(null);
		this.groupe = nbt.read("Group", UUIDUtil.CODEC).orElse(null);
		// Côté client, l'apparence a changé : le chunk doit se redessiner.
		if (this.level != null && this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
