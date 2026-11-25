package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
	private Component nom;

	public SelectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIFT_SELECTOR, pos, state);
	}

	public int décalage() {
		return this.décalage;
	}

	/** La couche où la plateforme se pose à cet étage. */
	public int niveau() {
		return this.worldPosition.getY() + this.décalage;
	}

	public Optional<Component> nom() {
		return Optional.ofNullable(this.nom);
	}

	public void régler(int décalage) {
		this.décalage = Math.clamp(décalage, -DÉCALAGE_MAX, DÉCALAGE_MAX);
		this.changé();
	}

	public void nommer(@Nullable Component nom) {
		this.nom = nom;
		this.changé();
	}

	private void changé() {
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("Offset", this.décalage);
		// 26.3 : le nom s'écrit comme ceux du jeu (un composant de texte en NBT),
		// et non plus en chaîne JSON ; l'ancienne forme se lit encore (lireNom).
		nbt.storeNullable("CustomName", ComponentSerialization.CODEC, this.nom);
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.décalage = nbt.getIntOr("Offset", DÉCALAGE_DÉFAUT);
		this.nom = lireNom(nbt);
	}

	/**
	 * Le nom de l'étage. En 1.21.1, il était écrit en chaîne JSON
	 * ({@code Component.Serializer.toJson}) — c'est encore le cas dans les
	 * gabarits du site de confinement — : une chaîne qui ressemble à du JSON est
	 * relue comme telle, sans quoi l'étage s'afficherait « {"text":"Level 0"} ».
	 */
	@Nullable
	private static Component lireNom(ValueInput nbt) {
		Optional<String> ancien = nbt.getString("CustomName");
		if (ancien.isPresent()) {
			String texte = ancien.get().trim();
			if (texte.startsWith("{") || texte.startsWith("[") || texte.startsWith("\"")) {
				try {
					return ComponentSerialization.CODEC
							.parse(RegistryOps.create(JsonOps.INSTANCE, nbt.lookup()), JsonParser.parseString(texte))
							.result().orElse(Component.literal(ancien.get()));
				} catch (RuntimeException illisible) {
					return Component.literal(ancien.get());
				}
			}
		}
		return parseCustomNameSafe(nbt, "CustomName");
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
