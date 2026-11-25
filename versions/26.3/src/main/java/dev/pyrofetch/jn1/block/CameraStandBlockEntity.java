package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Le réglage d'un trépied : orientation de l'objectif, retardateur et
 * cadrage. Synchronisé avec les clients, qui en ont besoin pour placer la vue.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class CameraStandBlockEntity extends BlockEntity {
	private static final String CLÉ_LACET = "yaw";
	private static final String CLÉ_TANGAGE = "pitch";
	private static final String CLÉ_RETARD = "delay";
	private static final String CLÉ_FORMAT = "format";
	public static final int[] RETARDS = new int[]{0, 3, 5, 10};
	private float lacet;
	private float tangage;
	private int retard;
	private PhotoFormat format = PhotoFormat.PAYSAGE;

	public CameraStandBlockEntity(BlockPos pos, BlockState état) {
		super(ModBlockEntities.CAMERA_STAND, pos, état);
		// Un trépied neuf cadre dans l'axe où il est posé (l'objectif vers le
		// joueur qui l'a posé) ; la sauvegarde, lue ensuite, l'emporte.
		if (état.hasProperty(CameraStandBlock.FACING)) {
			this.lacet = état.getValue(CameraStandBlock.FACING).toYRot();
		}
	}

	public float lacet() {
		return this.lacet;
	}

	public float tangage() {
		return this.tangage;
	}

	public int retard() {
		return this.retard;
	}

	public PhotoFormat format() {
		return this.format;
	}

	public static int retardSuivant(int actuel) {
		for (int i = 0; i < RETARDS.length; i++) {
			if (RETARDS[i] == actuel) {
				return RETARDS[(i + 1) % RETARDS.length];
			}
		}

		return RETARDS[0];
	}

	public void régler(float lacet, float tangage, int retard, PhotoFormat format) {
		this.lacet = Mth.wrapDegrees(lacet);
		this.tangage = Mth.clamp(tangage, -89.0F, 89.0F);
		this.retard = retard;
		this.format = format;
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public Vec3 objectif() {
		return new Vec3(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.62, this.worldPosition.getZ() + 0.5);
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.lacet = nbt.getFloatOr("yaw", 0.0F);
		this.tangage = nbt.getFloatOr("pitch", 0.0F);
		this.retard = nbt.getIntOr("delay", 0);
		this.format = nbt.read("format", PhotoFormat.CODEC).orElse(PhotoFormat.PAYSAGE);
	}

	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		nbt.putFloat("yaw", this.lacet);
		nbt.putFloat("pitch", this.tangage);
		nbt.putInt("delay", this.retard);
		nbt.store("format", PhotoFormat.CODEC, this.format);
	}

	@Override
	public CompoundTag getUpdateTag(Provider registryLookup) {
		return this.saveWithoutMetadata(registryLookup);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
