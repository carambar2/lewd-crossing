package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
		if (état.contains(CameraStandBlock.FACING)) {
			this.lacet = état.get(CameraStandBlock.FACING).asRotation();
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
		this.lacet = MathHelper.wrapDegrees(lacet);
		this.tangage = MathHelper.clamp(tangage, -89.0F, 89.0F);
		this.retard = retard;
		this.format = format;
		this.markDirty();
		if (this.world != null && !this.world.isClient) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
		}
	}

	public Vec3d objectif() {
		return new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.62, this.pos.getZ() + 0.5);
	}

	@Override
	protected void readNbt(NbtCompound nbt, WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.lacet = nbt.getFloat("yaw");
		this.tangage = nbt.getFloat("pitch");
		this.retard = nbt.getInt("delay");
		this.format = nbt.contains("format") ? PhotoFormat.CODEC.parse(NbtOps.INSTANCE, nbt.get("format")).result().orElse(PhotoFormat.PAYSAGE) : PhotoFormat.PAYSAGE;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putFloat("yaw", this.lacet);
		nbt.putFloat("pitch", this.tangage);
		nbt.putInt("delay", this.retard);
		PhotoFormat.CODEC.encodeStart(NbtOps.INSTANCE, this.format).result().ifPresent(écrit -> nbt.put("format", écrit));
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registryLookup) {
		return this.createNbt(registryLookup);
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}
}
