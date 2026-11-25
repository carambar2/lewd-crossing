package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import dev.pyrofetch.jn1.screen.VendingEditScreenHandler;
import dev.pyrofetch.jn1.shop.ShopEntry;
import dev.pyrofetch.jn1.shop.VendingOffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
 * l'entité n'est pas un {@link Inventory} : un entonnoir y puiserait.
 *
 * <p>Seule la moitié basse en porte une. Les machines posées avant qu'elle
 * existe en reçoivent une vide au premier accès, que le jeu crée d'office.
 */
public class VendingMachineBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
	public static final int RAYONS = 12;
	public static final int COLONNES = 3;
	public static final int PRIX_MAX = 999;

	private final DefaultedList<ItemStack> objets = DefaultedList.ofSize(RAYONS, ItemStack.EMPTY);
	private final int[] prix = new int[RAYONS];
	private boolean garnie;

	/** Les rayons vus par le menu d'édition ; tout changement garnit la machine. */
	private final Inventory rayons = new Inventory() {
		@Override
		public int size() {
			return RAYONS;
		}

		@Override
		public boolean isEmpty() {
			return VendingMachineBlockEntity.this.objets.stream().allMatch(ItemStack::isEmpty);
		}

		@Override
		public ItemStack getStack(int slot) {
			return VendingMachineBlockEntity.this.objets.get(slot);
		}

		@Override
		public ItemStack removeStack(int slot, int amount) {
			ItemStack pris = Inventories.splitStack(VendingMachineBlockEntity.this.objets, slot, amount);
			if (!pris.isEmpty()) {
				this.markDirty();
			}
			return pris;
		}

		@Override
		public ItemStack removeStack(int slot) {
			ItemStack pris = Inventories.removeStack(VendingMachineBlockEntity.this.objets, slot);
			this.markDirty();
			return pris;
		}

		@Override
		public void setStack(int slot, ItemStack stack) {
			VendingMachineBlockEntity.this.objets.set(slot, stack);
			this.markDirty();
		}

		@Override
		public void markDirty() {
			VendingMachineBlockEntity.this.changer();
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			return VendingMachineBlockEntity.this.éditablePar(player);
		}

		@Override
		public void clear() {
			VendingMachineBlockEntity.this.objets.clear();
			this.markDirty();
		}
	};

	/** Les douze prix, puis « garnie » (1) ou non (0) : ce que le menu synchronise. */
	private final PropertyDelegate propriétés = new PropertyDelegate() {
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
		public int size() {
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
	public List<VendingOffer> vitrine(World monde) {
		return vitrine(this, monde);
	}

	/** La vitrine d'une machine, ou le catalogue si elle n'a pas encore d'entité. */
	public static List<VendingOffer> vitrine(@Nullable VendingMachineBlockEntity machine, World monde) {
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
		if (this.garnie || this.world == null) {
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
		if (this.world == null) {
			return;
		}
		List<ShopEntry> catalogue = ShopEntry.catalogue(ShopEntry.Guichet.DISTRIBUTEUR, this.world);
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
	public boolean éditablePar(PlayerEntity player) {
		return !this.isRemoved() && player.isCreative()
				&& player.squaredDistanceTo(this.pos.toCenterPos()) <= 64.0;
	}

	public Inventory rayons() {
		return this.rayons;
	}

	public PropertyDelegate propriétés() {
		return this.propriétés;
	}

	private void changer() {
		this.garnie = true;
		this.synchroniser();
	}

	private void synchroniser() {
		this.markDirty();
		if (this.world != null && !this.world.isClient) {
			this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
		}
	}

	@Override
	protected void readNbt(NbtCompound nbt, WrapperLookup registries) {
		super.readNbt(nbt, registries);
		this.objets.clear();
		Arrays.fill(this.prix, 0);
		this.garnie = nbt.getBoolean("Garnie");
		if (this.garnie) {
			Inventories.readNbt(nbt, this.objets, registries);
			int[] lus = nbt.getIntArray("Prix");
			System.arraycopy(lus, 0, this.prix, 0, Math.min(lus.length, RAYONS));
		}
	}

	/** Rien n'est écrit d'une machine qui suit le catalogue. */
	@Override
	protected void writeNbt(NbtCompound nbt, WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (this.garnie) {
			nbt.putBoolean("Garnie", true);
			Inventories.writeNbt(nbt, this.objets, registries);
			nbt.putIntArray("Prix", this.prix.clone());
		}
	}

	/** Le client en a besoin pour dessiner la vitrine. */
	@Override
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registries) {
		return this.createNbt(registries);
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
		return this.pos;
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("screen.jn1.vending.edit");
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
		return new VendingEditScreenHandler(syncId, inventory, this.rayons, this.propriétés, this.pos);
	}
}
