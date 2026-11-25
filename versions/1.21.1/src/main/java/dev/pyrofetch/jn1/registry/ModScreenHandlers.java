package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.network.JennyScreenData;
import dev.pyrofetch.jn1.screen.JennyScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
	/**
	 * Type « étendu » : l'ouverture transporte l'identifiant de l'entité et le
	 * niveau d'affection, qu'un ScreenHandlerType ordinaire ne saurait pas
	 * transmettre.
	 */
	public static final ScreenHandlerType<JennyScreenHandler> JENNY = Registry.register(
			Registries.SCREEN_HANDLER,
			JN1.id("jenny"),
			new ExtendedScreenHandlerType<>(JennyScreenHandler::new, JennyScreenData.CODEC)
	);

	/** Les rayons d'un distributeur, en créatif ; l'ouverture transporte sa position. */
	public static final ScreenHandlerType<dev.pyrofetch.jn1.screen.VendingEditScreenHandler> VENDING_EDIT =
			Registry.register(Registries.SCREEN_HANDLER, JN1.id("vending_edit"),
					new ExtendedScreenHandlerType<>(dev.pyrofetch.jn1.screen.VendingEditScreenHandler::new,
							net.minecraft.util.math.BlockPos.PACKET_CODEC));

	/** L'interface d'un bloc de plateforme d'ascenseur ; l'ouverture transporte sa position. */
	public static final ScreenHandlerType<dev.pyrofetch.jn1.lift.PlatformScreenHandler> LIFT_PLATFORM =
			Registry.register(Registries.SCREEN_HANDLER, JN1.id("lift_platform"),
					new ExtendedScreenHandlerType<>(dev.pyrofetch.jn1.lift.PlatformScreenHandler::new,
							net.minecraft.util.math.BlockPos.PACKET_CODEC));

	private ModScreenHandlers() {
	}

	public static void register() {
		JN1.LOGGER.debug("Menus enregistrés.");
	}
}
