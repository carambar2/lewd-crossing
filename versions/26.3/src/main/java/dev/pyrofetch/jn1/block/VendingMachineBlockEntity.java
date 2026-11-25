package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import dev.pyrofetch.jn1.screen.VendingEditScreenHandler;
import dev.pyrofetch.jn1.shop.ShopEntry;
import dev.pyrofetch.jn1.shop.VendingOffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ce qu'un distributeur vend, s'il ne suit plus le catalogue.
 *
 * <p>Une machine neuve vend le catalogue commun ({@link ShopEntry}) : rien
 * n'est écrit ici tant que personne ne l'a garnie. En créatif, accroupi, un
 * clic droit ouvre ses rayons ({@link VendingEditScreenHandler}) : douze cases
 * qu'on remplit comme un coffre — la pile posée est ce que l'achat rend, sa
 * quantité comprise — et un prix par case. Le premier changement la rend
 * « garnie » ; elle ne vend plus alors que ses rayons, à leur place dans la
 * vitrine. Rétablir le catalogue efface tout.
 *
 * <p>Les rayons ne sont pas un stock : un achat ne les vide pas, et casser la
 * machine ne les lâche pas. Ce sont des étiquettes. C'est aussi pourquoi
 * l'entité n'est pas un {@link Container} : un entonnoir y puiserait.
 *
 * <p>Seule la moitié basse en porte une. Les machines posées avant qu'elle
 * existe en reçoivent une vide au premier accès, que le jeu crée d'office.
 */
public class VendingMachineBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
	public static final int RAYONS = 12;
	public static final int COLONNES = 3;
	public static final int PRIX_MAX = 999;

	private final NonNullList<ItemStack> objets = NonNullList.withSize(RAYONS, ItemStack.EMPTY);
	private final int[] prix = new int[RAYONS];
	private boolean garnie;

	/** Les rayons vus par le menu d'édition ; tout changement garnit la machine. */
	private final Container rayons = new Container() {
		@Override
		public int getContainerSize() {
			return RAYONS;
		}

		@Override
		public boolean isEmpty() {
			return VendingMachineBlockEntity.this.objets.stream().allMatch(ItemStack::isEmpty);
		}

		@Override
		public ItemStack getItem(int slot) {
			return VendingMachineBlockEntity.this.objets.get(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			ItemStack pris = ContainerHelper.removeItem(VendingMachineBlockEntity.this.objets, slot, amount);
			if (!pris.isEmpty()) {
				this.setChanged();
			}
			return pris;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack pris = ContainerHelper.takeItem(VendingMachineBlockEntity.this.objets, slot);
			this.setChanged();
			return pris;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			VendingMachineBlockEntity.this.objets.set(slot, stack);
			this.setChanged();
		}

		@Override
		public void setChanged() {
			VendingMachineBlockEntity.this.changer();
		}

		@Override
		public boolean stillValid(Player player) {
			return VendingMachineBlockEntity.this.éditablePar(player);
		}

		@Override
		public void clearContent() {
			VendingMachineBlockEntity.this.objets.clear();
			this.setChanged();
		}
	};

	/** Les douze prix, puis « garnie » (1) ou non (0) : ce que le menu synchronise. */
	private final ContainerData propriétés = new ContainerData() {
		@Override
		public int get(int index) {
			return index < RAYONS ? VendingMachineBlockEntity.this.prix[index]
					: VendingMachineBlockEntity.this.garnie ? 1 : 0;
		}

		@Override
		public void set(int index, int value) {
			if (index < RAYONS) {
				VendingMachineBlockEntity.this.fixerPrix(index, value);
			}
		}

		@Override
		public int getCount() {
			return RAYONS + 1;
		}
	};

	public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDING_MACHINE, pos, state);
	}

	public boolean garnie() {
		return this.garnie;
	}

	/**
	 * Ce que la vitrine montre, chacun à sa case.
	 *
	 * <p>Garnie, la machine montre ses rayons à la place où on les a mis, les
	 * cases vides restant vides : on voit en vente ce qu'on a vu en l'éditant.
	 * Sinon, le catalogue du distributeur, là où elle se trouve.
	 */
	public List<VendingOffer> vitrine(Level monde) {
		return vitrine(this, monde);
	}

	/** La vitrine d'une machine, ou le catalogue si elle n'a pas encore d'entité. */
	public static List<VendingOffer> vitrine(@Nullable VendingMachineBlockEntity machine, Level monde) {
		List<VendingOffer> offres = new ArrayList<>();
		if (machine != null && machine.garnie) {
			for (int rayon = 0; rayon < RAYONS; rayon++) {
				if (!machine.objets.get(rayon).isEmpty()) {
					offres.add(VendingOffer.rayon(rayon, machine.objets.get(rayon), machine.prix[rayon]));
				}
			}
			return offres;
		}
		List<ShopEntry> catalogue = ShopEntry.catalogue(ShopEntry.Guichet.DISTRIBUTEUR, monde);
		for (int place = 0; place < catalogue.size(); place++) {
			offres.add(VendingOffer.of(catalogue.get(place), place));
		}
		return offres;
	}

	/** L'offre que cet identifiant désigne sur une machine garnie, ou {@code null}. */
	@Nullable
	public VendingOffer offre(String id) {
		if (!this.garnie || !id.startsWith(VendingOffer.RAYON)) {
			return null;
		}
		int rayon;
		try {
			rayon = Integer.parseInt(id.substring(VendingOffer.RAYON.length()));
		} catch (NumberFormatException exception) {
			return null;
		}
		if (rayon < 0 || rayon >= RAYONS || this.objets.get(rayon).isEmpty()) {
			return null;
		}
		return VendingOffer.rayon(rayon, this.objets.get(rayon), this.prix[rayon]);
	}

	/**
	 * Avant d'ouvrir les rayons d'une machine qui suit le catalogue, on les
	 * remplit de ce qu'elle vend : on part de sa vitrine, pas d'étagères vides.
	 * Elle n'est pas garnie pour autant — refermer sans rien toucher la laisse
	 * suivre le catalogue.
	 */
	public void préparerÉdition() {
		if (this.garnie || this.level == null) {
			return;
		}
		this.remplirDuCatalogue();
	}

	/** Rend la machine au catalogue commun. */
	public void rétablir() {
		this.garnie = false;
		this.remplirDuCatalogue();
		this.synchroniser();
	}

	private void remplirDuCatalogue() {
		this.objets.clear();
		Arrays.fill(this.prix, 0);
		if (this.level == null) {
			return;
		}
		List<ShopEntry> catalogue = ShopEntry.catalogue(ShopEntry.Guichet.DISTRIBUTEUR, this.level);
		for (int rayon = 0; rayon < Math.min(RAYONS, catalogue.size()); rayon++) {
			this.objets.set(rayon, catalogue.get(rayon).stack().copy());
			this.prix[rayon] = catalogue.get(rayon).price();
		}
	}

	public void fixerPrix(int rayon, int valeur) {
		if (rayon < 0 || rayon >= RAYONS) {
			return;
		}
		int borné = Math.clamp(valeur, 0, PRIX_MAX);
		if (this.prix[rayon] == borné) {
			return;
		}
		this.prix[rayon] = borné;
		this.changer();
	}

	/** Réservé au créatif, et à portée de bras. */
	public boolean éditablePar(Player player) {
		return !this.isRemoved() && player.isCreative()
				&& player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition)) <= 64.0;
	}

	public Container rayons() {
		return this.rayons;
	}

	public ContainerData propriétés() {
		return this.propriétés;
	}

	private void changer() {
		this.garnie = true;
		this.synchroniser();
	}

	private void synchroniser() {
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.objets.clear();
		Arrays.fill(this.prix, 0);
		this.garnie = nbt.getBooleanOr("Garnie", false);
		if (this.garnie) {
			ContainerHelper.loadAllItems(nbt, this.objets);
			int[] lus = nbt.getIntArray("Prix").orElse(new int[0]);
			System.arraycopy(lus, 0, this.prix, 0, Math.min(lus.length, RAYONS));
		}
	}

	/** Rien n'est écrit d'une machine qui suit le catalogue. */
	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		if (this.garnie) {
			nbt.putBoolean("Garnie", true);
			ContainerHelper.saveAllItems(nbt, this.objets);
			nbt.putIntArray("Prix", this.prix.clone());
		}
	}

	/** Le client en a besoin pour dessiner la vitrine. */
	@Override
	public CompoundTag getUpdateTag(Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return this.worldPosition;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("screen.jn1.vending.edit");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new VendingEditScreenHandler(syncId, inventory, this.rayons, this.propriétés, this.worldPosition);
	}
}
