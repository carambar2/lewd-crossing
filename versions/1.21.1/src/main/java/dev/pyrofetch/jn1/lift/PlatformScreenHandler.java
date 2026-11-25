package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModScreenHandlers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * L'interface d'un bloc de plateforme.
 *
 * <p>La case d'apparence est un modèle, pas un coffre : on y dépose un bloc,
 * le bloc reste dans la main, et la plateforme en prend l'apparence (main vide :
 * elle retrouve la sienne). Rien n'y est stocké, rien n'en tombe : c'est un
 * décor, qu'on ne doit pas pouvoir dupliquer en cassant la plateforme.
 *
 * <p>Propriétés : blocs de la plateforme assemblée, blocs de plateforme
 * voisins (ce qu'« Assembler » réunirait), assemblée ou non.
 */
public class PlatformScreenHandler extends ScreenHandler {
	public static final int BOUTON_ASSEMBLER = 0;
	public static final int BOUTON_DÉSASSEMBLER = 1;
	public static final int BOUTON_HABILLER_TOUT = 2;

	public static final int CASE_X = 36;
	public static final int CASE_Y = 22;
	public static final int INVENTAIRE_Y = 84;

	private final SimpleInventory modèle = new SimpleInventory(1);
	private final PropertyDelegate propriétés;
	private final BlockPos pos;

	public PlatformScreenHandler(int syncId, PlayerInventory inventaire, BlockPos pos) {
		this(syncId, inventaire, pos, new ArrayPropertyDelegate(3));
	}

	public PlatformScreenHandler(int syncId, PlayerInventory inventaire, BlockPos pos, PropertyDelegate propriétés) {
		super(ModScreenHandlers.LIFT_PLATFORM, syncId);
		this.pos = pos;
		this.propriétés = propriétés;
		this.addSlot(new Slot(this.modèle, 0, CASE_X, CASE_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public boolean canTakeItems(PlayerEntity player) {
				return false;
			}
		});
		for (int ligne = 0; ligne < 3; ligne++) {
			for (int colonne = 0; colonne < 9; colonne++) {
				this.addSlot(new Slot(inventaire, colonne + ligne * 9 + 9, 8 + colonne * 18, INVENTAIRE_Y + ligne * 18));
			}
		}
		for (int colonne = 0; colonne < 9; colonne++) {
			this.addSlot(new Slot(inventaire, colonne, 8 + colonne * 18, INVENTAIRE_Y + 58));
		}
		this.addProperties(propriétés);
		if (!inventaire.player.getWorld().isClient()) {
			this.relire(inventaire.player);
		}
	}

	public BlockPos pos() {
		return this.pos;
	}

	public int taille() {
		return this.propriétés.get(0);
	}

	public int voisines() {
		return this.propriétés.get(1);
	}

	public boolean assemblée() {
		return this.propriétés.get(2) != 0;
	}

	/** Le bloc qu'une pile ferait imiter, s'il s'y prête : un bloc dessiné par un modèle, pas une plateforme. */
	@Nullable
	public static BlockState apparence(ItemStack pile) {
		if (!(pile.getItem() instanceof BlockItem objet)) {
			return null;
		}
		Block bloc = objet.getBlock();
		BlockState état = bloc.getDefaultState();
		if (bloc instanceof PlatformBlock || bloc instanceof SelectorBlock
				|| état.getRenderType() != BlockRenderType.MODEL) {
			return null;
		}
		return état;
	}

	/** Ce que la case et les compteurs montrent, relu de la plateforme. */
	private void relire(PlayerEntity joueur) {
		if (!(joueur.getWorld().getBlockEntity(this.pos) instanceof PlatformBlockEntity be)) {
			return;
		}
		this.modèle.setStack(0, be.apparence() == null ? ItemStack.EMPTY : new ItemStack(be.apparence().getBlock()));
		this.propriétés.set(0, Platforms.groupe(joueur.getWorld(), this.pos).size());
		this.propriétés.set(1, Platforms.voisines(joueur.getWorld(), this.pos).size());
		this.propriétés.set(2, be.groupe() != null ? 1 : 0);
		this.sendContentUpdates();
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex != 0) {
			super.onSlotClick(slotIndex, button, actionType, player);
			return;
		}
		if (player.getWorld().isClient()
				|| !(player.getWorld().getBlockEntity(this.pos) instanceof PlatformBlockEntity be)) {
			return;
		}
		ItemStack curseur = this.getCursorStack();
		if (curseur.isEmpty()) {
			be.imiter(null);
		} else {
			BlockState apparence = apparence(curseur);
			if (apparence != null) {
				be.imiter(apparence);
			}
		}
		this.relire(player);
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		var monde = player.getWorld();
		if (!(monde.getBlockEntity(this.pos) instanceof PlatformBlockEntity)) {
			return false;
		}
		switch (id) {
			case BOUTON_ASSEMBLER -> Platforms.assembler(monde, this.pos);
			case BOUTON_DÉSASSEMBLER -> Platforms.désassembler(monde, this.pos);
			case BOUTON_HABILLER_TOUT -> Platforms.habillerTout(monde, this.pos);
			default -> {
				return false;
			}
		}
		this.relire(player);
		return true;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return player.getWorld().getBlockState(this.pos).getBlock() instanceof PlatformBlock
				&& player.squaredDistanceTo(this.pos.toCenterPos()) <= 64.0;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		// Un clic Maj sur un bloc de l'inventaire le donne pour modèle.
		if (index > 0 && !player.getWorld().isClient()
				&& player.getWorld().getBlockEntity(this.pos) instanceof PlatformBlockEntity be) {
			BlockState apparence = apparence(this.slots.get(index).getStack());
			if (apparence != null) {
				be.imiter(apparence);
				this.relire(player);
			}
		}
		return ItemStack.EMPTY;
	}
}
