package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.network.JennyScreenData;
import dev.pyrofetch.jn1.screen.JennyScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
	/**
	 * Type « étendu » : l'ouverture transporte l'identifiant de l'entité et le
	 * niveau d'affection, qu'un ScreenHandlerType ordinaire ne saurait pas
	 * transmettre.
	 */
	public static final MenuType<JennyScreenHandler> JENNY = Registry.register(
			BuiltInRegistries.MENU,
			JN1.id("jenny"),
			new ExtendedMenuType<>(JennyScreenHandler::new, JennyScreenData.CODEC)
	);

	/** Les rayons d'un distributeur, en créatif ; l'ouverture transporte sa position. */
	public static final MenuType<dev.pyrofetch.jn1.screen.VendingEditScreenHandler> VENDING_EDIT =
			Registry.register(BuiltInRegistries.MENU, JN1.id("vending_edit"),
					new ExtendedMenuType<>(dev.pyrofetch.jn1.screen.VendingEditScreenHandler::new,
							net.minecraft.core.BlockPos.STREAM_CODEC));

	/** L'interface d'un bloc de plateforme d'ascenseur ; l'ouverture transporte sa position. */
	public static final MenuType<dev.pyrofetch.jn1.lift.PlatformScreenHandler> LIFT_PLATFORM =
			Registry.register(BuiltInRegistries.MENU, JN1.id("lift_platform"),
					new ExtendedMenuType<>(dev.pyrofetch.jn1.lift.PlatformScreenHandler::new,
							net.minecraft.core.BlockPos.STREAM_CODEC));

	private ModScreenHandlers() {
	}

	public static void register() {
		JN1.LOGGER.debug("Menus enregistrés.");
	}
}
