package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Ce que montre un cadre. Le cadre d'ancrage porte la photo ; les autres
 * cadres de son emprise ne font que renvoyer vers lui.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoFrameBlockEntity extends BlockEntity {
	private static final String CLÉ_PHOTO = "photo";
	private static final String CLÉ_FORMAT = "format";
	private static final String CLÉ_ANCRE = "anchor";
	private int photo;
	@Nullable
	private PhotoFormat format;
	@Nullable
	private BlockPos ancre;

	public PhotoFrameBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PHOTO_FRAME, pos, state);
	}

	public int photo() {
		return this.photo;
	}

	@Nullable
	public PhotoFormat format() {
		return this.format;
	}

	@Nullable
	public BlockPos ancre() {
		return this.ancre;
	}

	public boolean estLibre() {
		return this.photo == 0 && this.ancre == null;
	}

	public boolean estAncre() {
		return this.photo != 0 && this.format != null;
	}

	private void synchroniser() {
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public static Direction[] axes(AttachFace face, Direction facing) {
		return switch (face) {
			case WALL -> new Direction[]{facing.getCounterClockWise(), Direction.DOWN};
			case FLOOR -> new Direction[]{facing.getCounterClockWise(), facing};
			case CEILING -> new Direction[]{facing.getClockWise(), facing};
		};
	}

	public static List<BlockPos> emprise(BlockPos ancre, BlockState état, PhotoFormat format) {
		Direction[] axes = axes(état.getValue(BlockStateProperties.ATTACH_FACE), état.getValue(BlockStateProperties.HORIZONTAL_FACING));
		List<BlockPos> cases = new ArrayList<>(format.cases());

		for (int ligne = 0; ligne < format.casesEnHauteur(); ligne++) {
			for (int colonne = 0; colonne < format.casesEnLargeur(); colonne++) {
				cases.add(ancre.relative(axes[0], colonne).relative(axes[1], ligne));
			}
		}

		return cases;
	}

	@Nullable
	public PhotoFrameBlockEntity porteur() {
		if (this.estAncre()) {
			return this;
		} else if (this.ancre != null && this.level != null) {
			return this.level.getBlockEntity(this.ancre) instanceof PhotoFrameBlockEntity cadre && cadre.estAncre() ? cadre : null;
		} else {
			return null;
		}
	}

	public boolean accrocher(int numéro, PhotoFormat format) {
		if (this.level != null && !this.level.isClientSide() && this.estLibre()) {
			List<PhotoFrameBlockEntity> cases = this.empriseLibre(format);
			if (cases == null) {
				return false;
			}

			this.photo = numéro;
			this.format = format;

			for (PhotoFrameBlockEntity recouvert : cases) {
				if (recouvert != this) {
					recouvert.ancre = this.worldPosition;
					recouvert.synchroniser();
				}
			}

			this.synchroniser();
			return true;
		} else {
			return false;
		}
	}

	@Nullable
	private List<PhotoFrameBlockEntity> empriseLibre(PhotoFormat format) {
		if (this.level == null) {
			return null;
		}

		BlockState état = this.getBlockState();
		AttachFace face = état.getValue(BlockStateProperties.ATTACH_FACE);
		Direction facing = état.getValue(BlockStateProperties.HORIZONTAL_FACING);
		List<PhotoFrameBlockEntity> trouvés = new ArrayList<>(format.cases());

		for (BlockPos pos : emprise(this.worldPosition, état, format)) {
			BlockState voisin = this.level.getBlockState(pos);
			if (!(voisin.getBlock() instanceof PhotoFrameBlock)) {
				return null;
			}

			if (voisin.getValue(BlockStateProperties.ATTACH_FACE) == face && voisin.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
				if (this.level.getBlockEntity(pos) instanceof PhotoFrameBlockEntity cadre && cadre.estLibre()) {
					trouvés.add(cadre);
					continue;
				}

				return null;
			}

			return null;
		}

		return trouvés;
	}

	@Nullable
	public PhotoFrameBlockEntity.Décrochage décrocher() {
		PhotoFrameBlockEntity porteur = this.porteur();
		if (porteur == null) {
			if (this.ancre != null) {
				this.ancre = null;
				this.synchroniser();
			}

			return null;
		} else {
			int numéro = porteur.photo;
			PhotoFormat format = porteur.format;
			if (this.level != null) {
				for (BlockPos pos : emprise(porteur.worldPosition, porteur.getBlockState(), format)) {
					if (this.level.getBlockEntity(pos) instanceof PhotoFrameBlockEntity cadre) {
						cadre.photo = 0;
						cadre.format = null;
						cadre.ancre = null;
						cadre.synchroniser();
					}
				}
			}

			return new PhotoFrameBlockEntity.Décrochage(numéro, format);
		}
	}

	/** Le cadre disparaît (cassé, remplacé) : le tirage qu'il portait est rendu au sol. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level != null && !this.level.isClientSide()) {
			PhotoFrameBlockEntity.Décrochage décroché = this.décrocher();
			if (décroché != null) {
				PhotoFrameBlock.rendre(this.level, pos, null, décroché);
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.photo = nbt.getIntOr("photo", 0);
		this.format = nbt.read("format", PhotoFormat.CODEC).orElse(null);
		// Écrite en tableau d'entiers [x, y, z], comme NbtUtils.writeBlockPos en 1.21.1.
		this.ancre = nbt.read("anchor", BlockPos.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("photo", this.photo);
		nbt.storeNullable("format", PhotoFormat.CODEC, this.format);
		nbt.storeNullable("anchor", BlockPos.CODEC, this.ancre);
	}

	@Override
	public CompoundTag getUpdateTag(Provider registryLookup) {
		return this.saveWithoutMetadata(registryLookup);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Nullable
	public static PhotoFrameBlockEntity at(Level monde, BlockPos pos) {
		return monde.getBlockEntity(pos) instanceof PhotoFrameBlockEntity cadre ? cadre : null;
	}

	public record Décrochage(int numéro, PhotoFormat format) {
	}
}
