package dev.pyrofetch.jn1.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.JennyScreenData;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Ouvre le menu pour un joueur donné.
 *
 * <p>L'affection est lue au moment de l'ouverture, sur les données du joueur qui
 * ouvre : en multijoueur, deux personnes ne voient pas la même barre.
 */
public record JennyScreenFactory(CompanionEntity jenny) implements ExtendedScreenHandlerFactory<JennyScreenData> {

	@Override
	public JennyScreenData getScreenOpeningData(ServerPlayerEntity player) {
		return new JennyScreenData(this.jenny.getId(), ModAttachments.get(player).trust(this.jenny.kind().id()));
	}

	@Override
	public Text getDisplayName() {
		return this.jenny.getDisplayName();
	}

	@Nullable
	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return new JennyScreenHandler(syncId, playerInventory, this.jenny.getBackpack(),
				ModAttachments.get(player).trust(this.jenny.kind().id()), this.jenny.getId()).avec(this.jenny);
	}
}
