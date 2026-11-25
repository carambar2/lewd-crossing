package dev.pyrofetch.jn1.screen;

import dev.pyrofetch.jn1.block.VendingMachineBlockEntity;
import dev.pyrofetch.jn1.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Les rayons d'un distributeur, ouverts en créatif : douze cases qu'on garnit
 * comme un coffre, un prix sous chacune, et l'inventaire du joueur.
 *
 * <p>Les prix voyagent par les propriétés du menu, que le jeu synchronise
 * seul. Les changer passe par le clic de bouton du jeu, dont le numéro porte
 * la case et le prix d'un bloc ({@link #boutonPrix}) : le serveur revérifie
 * le créatif et la portée ({@link #canUse}) avant d'y toucher.
 */
public class VendingEditScreenHandler extends ScreenHandler {
	public static final int RAYONS = VendingMachineBlockEntity.RAYONS;
	public static final int COLONNES = VendingMachineBlockEntity.COLONNES;

	/** Disposition, partagée avec l'écran. */
	public static final int LARGEUR = 176;
	public static final int RAYON_X = 9;
	public static final int RAYON_Y = 30;
	public static final int CASE_L = 54;
	public static final int CASE_H = 22;
	public static final int INVENTAIRE_Y = RAYON_Y + 4 * CASE_H + 36;

	/** Rétablir le catalogue commun. */
	public static final int BOUTON_RÉTABLIR = 0;
	private static final int BOUTON_PRIX = 1000;

	private final Inventory rayons;
	private final PropertyDelegate propriétés;
	private final BlockPos pos;

	/** Constructeur client : le serveur remplira cases et prix. */
	public VendingEditScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
		this(syncId, playerInventory, new SimpleInventory(RAYONS), new ArrayPropertyDelegate(RAYONS + 1), pos);
	}

	public VendingEditScreenHandler(int syncId, PlayerInventory playerInventory, Inventory rayons,
	                                PropertyDelegate propriétés, BlockPos pos) {
		super(ModScreenHandlers.VENDING_EDIT, syncId);
		checkSize(rayons, RAYONS);
		checkDataCount(propriétés, RAYONS + 1);
		this.rayons = rayons;
		this.propriétés = propriétés;
		this.pos = pos;

		for (int rayon = 0; rayon < RAYONS; rayon++) {
			this.addSlot(new Slot(rayons, rayon,
					RAYON_X + (rayon % COLONNES) * CASE_L + 1,
					RAYON_Y + (rayon / COLONNES) * CASE_H + 3));
		}
		for (int ligne = 0; ligne < 3; ligne++) {
			for (int colonne = 0; colonne < 9; colonne++) {
				this.addSlot(new Slot(playerInventory, colonne + ligne * 9 + 9,
						8 + colonne * 18, INVENTAIRE_Y + ligne * 18));
			}
		}
		for (int colonne = 0; colonne < 9; colonne++) {
			this.addSlot(new Slot(playerInventory, colonne, 8 + colonne * 18, INVENTAIRE_Y + 58));
		}
		this.addProperties(propriétés);
	}

	public BlockPos pos() {
		return this.pos;
	}

	public int prix(int rayon) {
		return this.propriétés.get(rayon);
	}

	/** Vrai si la machine ne suit plus le catalogue. */
	public boolean garnie() {
		return this.propriétés.get(RAYONS) != 0;
	}

	/** Le numéro de bouton qui fixe ce prix sur cette case. */
	public static int boutonPrix(int rayon, int prix) {
		return BOUTON_PRIX * (rayon + 1) + Math.clamp(prix, 0, VendingMachineBlockEntity.PRIX_MAX);
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (!player.isCreative()
				|| !(player.getWorld().getBlockEntity(this.pos) instanceof VendingMachineBlockEntity machine)) {
			return false;
		}
		if (id == BOUTON_RÉTABLIR) {
			machine.rétablir();
			return true;
		}
		int rayon = id / BOUTON_PRIX - 1;
		if (rayon < 0 || rayon >= RAYONS) {
			return false;
		}
		machine.fixerPrix(rayon, id % BOUTON_PRIX);
		return true;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.rayons.canPlayerUse(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}
		ItemStack pile = slot.getStack();
		ItemStack origine = pile.copy();
		if (index < RAYONS) {
			if (!this.insertItem(pile, RAYONS, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(pile, 0, RAYONS, false)) {
			return ItemStack.EMPTY;
		}
		if (pile.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		return origine;
	}
}
