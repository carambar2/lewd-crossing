package dev.pyrofetch.jn1.screen;

import dev.pyrofetch.jn1.network.JennyScreenData;
import dev.pyrofetch.jn1.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Le sac de Jenny, plus l'inventaire du joueur.
 *
 * <p>Elle porte ses affaires elle-même : ce qu'on lui donne, elle le garde. Le
 * niveau d'affection voyage avec l'écran mais n'est qu'un affichage — il reste
 * décidé par le serveur.
 */
public class JennyScreenHandler extends AbstractContainerMenu {
	public static final int SLOT_COUNT = 9;

	/** Marge de gauche des deux sacs : ce qui reste avant est la colonne d'actions. */
	public static final int SLOTS_LEFT = 8;

	private final Container inventory;
	private final int trust;
	private final int entityId;
	/** Côté serveur seulement : la compagne, pour fermer le menu si elle s'éloigne ou meurt. */
	@org.jetbrains.annotations.Nullable
	private dev.pyrofetch.jn1.entity.CompanionEntity compagne;

	/** Constructeur client : l'inventaire est vide, le serveur le remplira. */
	public JennyScreenHandler(int syncId, Inventory playerInventory, JennyScreenData data) {
		this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT), data.trust(), data.entityId());
	}

	public JennyScreenHandler(int syncId, Inventory playerInventory,
	                          Container inventory, int trust, int entityId) {
		super(ModScreenHandlers.JENNY, syncId);
		checkContainerSize(inventory, SLOT_COUNT);
		this.inventory = inventory;
		this.trust = trust;
		this.entityId = entityId;
		inventory.startOpen(playerInventory.player);

		// La colonne de gauche est réservée aux actions ; les sacs commencent
		// donc à SLOTS_LEFT et non contre le bord.
		for (int column = 0; column < SLOT_COUNT; column++) {
			this.addSlot(new Slot(inventory, column, SLOTS_LEFT + column * 18, 22));
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
						SLOTS_LEFT + column * 18, 61 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, SLOTS_LEFT + column * 18, 123));
		}
	}

	public int trust() {
		return this.trust;
	}

	public int entityId() {
		return this.entityId;
	}

	public JennyScreenHandler avec(dev.pyrofetch.jn1.entity.CompanionEntity compagne) {
		this.compagne = compagne;
		return this;
	}

	/**
	 * Le menu se ferme si elle meurt, part, est capturée ou s'éloigne de huit
	 * blocs : sans cela on gardait son sac ouvert, et ce qu'on en retirait
	 * après une capture était dupliqué (la capsule en gardait une copie).
	 */
	@Override
	public boolean stillValid(Player player) {
		if (this.compagne == null) {
			return this.inventory.stillValid(player);
		}
		return this.compagne.isAlive() && !this.compagne.isRemoved() && player.distanceToSqr(this.compagne) < 64.0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex < SLOT_COUNT) {
			// De son sac vers le joueur.
			if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.inventory.stopOpen(player);
	}
}
