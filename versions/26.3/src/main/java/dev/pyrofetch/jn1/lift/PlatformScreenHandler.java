package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
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
public class PlatformScreenHandler extends AbstractContainerMenu {
	public static final int BOUTON_ASSEMBLER = 0;
	public static final int BOUTON_DÉSASSEMBLER = 1;
	public static final int BOUTON_HABILLER_TOUT = 2;

	public static final int CASE_X = 36;
	public static final int CASE_Y = 22;
	public static final int INVENTAIRE_Y = 84;

	private final SimpleContainer modèle = new SimpleContainer(1);
	private final ContainerData propriétés;
	private final BlockPos pos;

	public PlatformScreenHandler(int syncId, Inventory inventaire, BlockPos pos) {
		this(syncId, inventaire, pos, new SimpleContainerData(3));
	}

	public PlatformScreenHandler(int syncId, Inventory inventaire, BlockPos pos, ContainerData propriétés) {
		super(ModScreenHandlers.LIFT_PLATFORM, syncId);
		this.pos = pos;
		this.propriétés = propriétés;
		this.addSlot(new Slot(this.modèle, 0, CASE_X, CASE_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
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
		this.addDataSlots(propriétés);
		if (!inventaire.player.level().isClientSide()) {
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
		BlockState état = bloc.defaultBlockState();
		if (bloc instanceof PlatformBlock || bloc instanceof SelectorBlock
				|| état.getRenderShape() != RenderShape.MODEL) {
			return null;
		}
		return état;
	}

	/** Ce que la case et les compteurs montrent, relu de la plateforme. */
	private void relire(Player joueur) {
		if (!(joueur.level().getBlockEntity(this.pos) instanceof PlatformBlockEntity be)) {
			return;
		}
		this.modèle.setItem(0, be.apparence() == null ? ItemStack.EMPTY : new ItemStack(be.apparence().getBlock()));
		this.propriétés.set(0, Platforms.groupe(joueur.level(), this.pos).size());
		this.propriétés.set(1, Platforms.voisines(joueur.level(), this.pos).size());
		this.propriétés.set(2, be.groupe() != null ? 1 : 0);
		this.broadcastChanges();
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
		if (slotIndex != 0) {
			super.clicked(slotIndex, button, actionType, player);
			return;
		}
		if (player.level().isClientSide()
				|| !(player.level().getBlockEntity(this.pos) instanceof PlatformBlockEntity be)) {
			return;
		}
		ItemStack curseur = this.getCarried();
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
	public boolean clickMenuButton(Player player, int id) {
		var monde = player.level();
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
	public boolean stillValid(Player player) {
		return player.level().getBlockState(this.pos).getBlock() instanceof PlatformBlock
				&& player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.pos)) <= 64.0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		// Un clic Maj sur un bloc de l'inventaire le donne pour modèle.
		if (index > 0 && !player.level().isClientSide()
				&& player.level().getBlockEntity(this.pos) instanceof PlatformBlockEntity be) {
			BlockState apparence = apparence(this.slots.get(index).getItem());
			if (apparence != null) {
				be.imiter(apparence);
				this.relire(player);
			}
		}
		return ItemStack.EMPTY;
	}
}
