package dev.pyrofetch.jn1.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.JennyScreenData;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Ouvre le menu pour un joueur donné.
 *
 * <p>L'affection est lue au moment de l'ouverture, sur les données du joueur qui
 * ouvre : en multijoueur, deux personnes ne voient pas la même barre.
 */
public record JennyScreenFactory(CompanionEntity jenny) implements ExtendedMenuProvider<JennyScreenData> {

	@Override
	public JennyScreenData getScreenOpeningData(ServerPlayer player) {
		return new JennyScreenData(this.jenny.getId(), ModAttachments.get(player).trust(this.jenny.kind().id()));
	}

	@Override
	public Component getDisplayName() {
		return this.jenny.getDisplayName();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new JennyScreenHandler(syncId, playerInventory, this.jenny.getBackpack(),
				ModAttachments.get(player).trust(this.jenny.kind().id()), this.jenny.getId()).avec(this.jenny);
	}
}
