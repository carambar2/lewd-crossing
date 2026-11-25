package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
		this.markDirty();
		if (this.world != null && !this.world.isClient) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
		}
	}

	public static Direction[] axes(BlockFace face, Direction facing) {
		return switch (face) {
			case WALL -> new Direction[]{facing.rotateYCounterclockwise(), Direction.DOWN};
			case FLOOR -> new Direction[]{facing.rotateYCounterclockwise(), facing};
			case CEILING -> new Direction[]{facing.rotateYClockwise(), facing};
		};
	}

	public static List<BlockPos> emprise(BlockPos ancre, BlockState état, PhotoFormat format) {
		Direction[] axes = axes(état.get(Properties.BLOCK_FACE), état.get(Properties.HORIZONTAL_FACING));
		List<BlockPos> cases = new ArrayList<>(format.cases());

		for (int ligne = 0; ligne < format.casesEnHauteur(); ligne++) {
			for (int colonne = 0; colonne < format.casesEnLargeur(); colonne++) {
				cases.add(ancre.offset(axes[0], colonne).offset(axes[1], ligne));
			}
		}

		return cases;
	}

	@Nullable
	public PhotoFrameBlockEntity porteur() {
		if (this.estAncre()) {
			return this;
		} else if (this.ancre != null && this.world != null) {
			return this.world.getBlockEntity(this.ancre) instanceof PhotoFrameBlockEntity cadre && cadre.estAncre() ? cadre : null;
		} else {
			return null;
		}
	}

	public boolean accrocher(int numéro, PhotoFormat format) {
		if (this.world != null && !this.world.isClient && this.estLibre()) {
			List<PhotoFrameBlockEntity> cases = this.empriseLibre(format);
			if (cases == null) {
				return false;
			}

			this.photo = numéro;
			this.format = format;

			for (PhotoFrameBlockEntity recouvert : cases) {
				if (recouvert != this) {
					recouvert.ancre = this.pos;
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
		if (this.world == null) {
			return null;
		}

		BlockState état = this.getCachedState();
		BlockFace face = état.get(Properties.BLOCK_FACE);
		Direction facing = état.get(Properties.HORIZONTAL_FACING);
		List<PhotoFrameBlockEntity> trouvés = new ArrayList<>(format.cases());

		for (BlockPos pos : emprise(this.pos, état, format)) {
			BlockState voisin = this.world.getBlockState(pos);
			if (!(voisin.getBlock() instanceof PhotoFrameBlock)) {
				return null;
			}

			if (voisin.get(Properties.BLOCK_FACE) == face && voisin.get(Properties.HORIZONTAL_FACING) == facing) {
				if (this.world.getBlockEntity(pos) instanceof PhotoFrameBlockEntity cadre && cadre.estLibre()) {
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
			if (this.world != null) {
				for (BlockPos pos : emprise(porteur.pos, porteur.getCachedState(), format)) {
					if (this.world.getBlockEntity(pos) instanceof PhotoFrameBlockEntity cadre) {
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

	@Override
	protected void readNbt(NbtCompound nbt, WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.photo = nbt.getInt("photo");
		this.format = nbt.contains("format") ? (PhotoFormat)PhotoFormat.CODEC.parse(NbtOps.INSTANCE, nbt.get("format")).result().orElse(null) : null;
		this.ancre = NbtHelper.toBlockPos(nbt, "anchor").orElse(null);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putInt("photo", this.photo);
		if (this.format != null) {
			PhotoFormat.CODEC.encodeStart(NbtOps.INSTANCE, this.format).result().ifPresent(écrit -> nbt.put("format", écrit));
		}

		if (this.ancre != null) {
			nbt.put("anchor", NbtHelper.fromBlockPos(this.ancre));
		}
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registryLookup) {
		return this.createNbt(registryLookup);
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Nullable
	public static PhotoFrameBlockEntity at(World monde, BlockPos pos) {
		return monde.getBlockEntity(pos) instanceof PhotoFrameBlockEntity cadre ? cadre : null;
	}

	public record Décrochage(int numéro, PhotoFormat format) {
	}
}
