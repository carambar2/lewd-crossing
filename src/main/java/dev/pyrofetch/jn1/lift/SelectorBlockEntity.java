package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Le réglage d'un sélecteur d'étage : où s'arrête la plateforme par rapport à
 * lui (sa couche de blocs, en blocs, {@link #DÉCALAGE_DÉFAUT} : posé à hauteur
 * des pieds, la plateforme affleure le sol du palier), et le nom de l'étage.
 */
public class SelectorBlockEntity extends BlockEntity {
	public static final int DÉCALAGE_DÉFAUT = -1;
	public static final int DÉCALAGE_MAX = 16;
	public static final int NOM_MAX = 32;

	private int décalage = DÉCALAGE_DÉFAUT;
	@Nullable
	private Text nom;

	public SelectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIFT_SELECTOR, pos, state);
	}

	public int décalage() {
		return this.décalage;
	}

	/** La couche où la plateforme se pose à cet étage. */
	public int niveau() {
		return this.pos.getY() + this.décalage;
	}

	public Optional<Text> nom() {
		return Optional.ofNullable(this.nom);
	}

	public void régler(int décalage) {
		this.décalage = Math.clamp(décalage, -DÉCALAGE_MAX, DÉCALAGE_MAX);
		this.changé();
	}

	public void nommer(@Nullable Text nom) {
		this.nom = nom;
		this.changé();
	}

	private void changé() {
		this.markDirty();
		if (this.world != null) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putInt("Offset", this.décalage);
		if (this.nom != null) {
			nbt.putString("CustomName", Text.Serialization.toJsonString(this.nom, registries));
		}
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		this.décalage = nbt.contains("Offset") ? nbt.getInt("Offset") : DÉCALAGE_DÉFAUT;
		this.nom = nbt.contains("CustomName") ? tryParseCustomName(nbt.getString("CustomName"), registries) : null;
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
