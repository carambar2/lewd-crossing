package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.network.OpenSelectorPayload;
import dev.pyrofetch.jn1.network.SelectorActionPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import java.util.List;

/**
 * L'ascenseur côté serveur : l'interface de la plateforme (accroupi), le menu
 * du sélecteur, et ce qu'on accepte de lui. Rien de ce que dit le client n'est
 * cru sur parole : le sélecteur doit exister et être à portée, l'étage en être
 * un de sa plateforme.
 */
public final class LiftHandler {
	private static final double PORTÉE_CARRÉE = 8.0 * 8.0;

	private LiftHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(SelectorActionPayload.ID,
				(payload, context) -> context.server().execute(() -> agir(context.player(), payload)));
		// Accroupi, un objet en main, le jeu ne consulte pas le bloc : on prend le clic avant lui.
		UseBlockCallback.EVENT.register((joueur, monde, main, visée) -> {
			BlockPos pos = visée.getBlockPos();
			if (!joueur.isShiftKeyDown() || joueur.isSpectator()
					|| !(monde.getBlockState(pos).getBlock() instanceof PlatformBlock)) {
				return InteractionResult.PASS;
			}
			if (joueur instanceof ServerPlayer serveur) {
				ouvrirPlateforme(serveur, pos);
			}
			return InteractionResult.SUCCESS;
		});
	}

	public static void ouvrirPlateforme(ServerPlayer joueur, BlockPos pos) {
		joueur.openMenu(new ExtendedMenuProvider<BlockPos>() {
			@Override
			public BlockPos getScreenOpeningData(ServerPlayer player) {
				return pos;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.jn1.lift_platform.title");
			}

			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory inventaire, Player player) {
				return new PlatformScreenHandler(syncId, inventaire, pos,
						new net.minecraft.world.inventory.SimpleContainerData(3));
			}
		});
	}

	public static void ouvrir(ServerPlayer joueur, BlockPos pos) {
		if (!(joueur.level().getBlockEntity(pos) instanceof SelectorBlockEntity be)) {
			return;
		}
		Platforms.Plateforme p = Platforms.desservie(joueur.level(), pos);
		List<OpenSelectorPayload.Floor> étages = p == null ? List.of() : Platforms.étages(joueur.level(), p).stream()
				.map(é -> new OpenSelectorPayload.Floor(é.niveau(), é.nom()))
				.toList();
		ServerPlayNetworking.send(joueur, new OpenSelectorPayload(pos, étages, p == null ? 0 : p.taille(),
				p == null ? 0 : p.niveau(), p != null && p.enRoute(), be.décalage(), be.niveau(),
				be.nom().map(Component::getString).orElse("")));
	}

	private static void agir(ServerPlayer joueur, SelectorActionPayload payload) {
		BlockPos pos = payload.pos();
		if (joueur.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > PORTÉE_CARRÉE
				|| !(joueur.level().getBlockEntity(pos) instanceof SelectorBlockEntity be)) {
			return;
		}
		ServerLevel monde = joueur.level();
		switch (payload.action()) {
			case SelectorActionPayload.ALLER -> {
				Platforms.Plateforme p = Platforms.desservie(monde, pos);
				if (p == null) {
					Platforms.dire(joueur, "message.jn1.lift.no_platform");
				} else if (Platforms.étages(monde, p).stream().anyMatch(é -> é.niveau() == payload.valeur())) {
					Platforms.envoyer(monde, p, payload.valeur(), joueur);
				}
			}
			case SelectorActionPayload.DÉCALER -> {
				be.régler(payload.valeur());
				Platforms.Plateforme p = Platforms.desservie(monde, pos);
				SelectorBlock.alimenter(monde, pos, p != null && !p.enRoute() && p.niveau() == be.niveau());
				ouvrir(joueur, pos);
			}
			case SelectorActionPayload.NOMMER -> {
				String nom = payload.texte().strip();
				if (nom.length() > SelectorBlockEntity.NOM_MAX) {
					nom = nom.substring(0, SelectorBlockEntity.NOM_MAX);
				}
				be.nommer(nom.isEmpty() ? null : Component.literal(nom));
				ouvrir(joueur, pos);
			}
			default -> {
			}
		}
	}
}
