package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.network.OpenSelectorPayload;
import dev.pyrofetch.jn1.network.SelectorActionPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

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
				(payload, context) -> context.player().server.execute(() -> agir(context.player(), payload)));
		// Accroupi, un objet en main, le jeu ne consulte pas le bloc : on prend le clic avant lui.
		UseBlockCallback.EVENT.register((joueur, monde, main, visée) -> {
			BlockPos pos = visée.getBlockPos();
			if (!joueur.isSneaking() || joueur.isSpectator()
					|| !(monde.getBlockState(pos).getBlock() instanceof PlatformBlock)) {
				return ActionResult.PASS;
			}
			if (joueur instanceof ServerPlayerEntity serveur) {
				ouvrirPlateforme(serveur, pos);
			}
			return ActionResult.SUCCESS;
		});
	}

	public static void ouvrirPlateforme(ServerPlayerEntity joueur, BlockPos pos) {
		joueur.openHandledScreen(new ExtendedScreenHandlerFactory<BlockPos>() {
			@Override
			public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
				return pos;
			}

			@Override
			public Text getDisplayName() {
				return Text.translatable("screen.jn1.lift_platform.title");
			}

			@Override
			public ScreenHandler createMenu(int syncId, PlayerInventory inventaire, PlayerEntity player) {
				return new PlatformScreenHandler(syncId, inventaire, pos,
						new net.minecraft.screen.ArrayPropertyDelegate(3));
			}
		});
	}

	public static void ouvrir(ServerPlayerEntity joueur, BlockPos pos) {
		if (!(joueur.getWorld().getBlockEntity(pos) instanceof SelectorBlockEntity be)) {
			return;
		}
		Platforms.Plateforme p = Platforms.desservie(joueur.getWorld(), pos);
		List<OpenSelectorPayload.Floor> étages = p == null ? List.of() : Platforms.étages(joueur.getWorld(), p).stream()
				.map(é -> new OpenSelectorPayload.Floor(é.niveau(), é.nom()))
				.toList();
		ServerPlayNetworking.send(joueur, new OpenSelectorPayload(pos, étages, p == null ? 0 : p.taille(),
				p == null ? 0 : p.niveau(), p != null && p.enRoute(), be.décalage(), be.niveau(),
				be.nom().map(Text::getString).orElse("")));
	}

	private static void agir(ServerPlayerEntity joueur, SelectorActionPayload payload) {
		BlockPos pos = payload.pos();
		if (joueur.squaredDistanceTo(pos.toCenterPos()) > PORTÉE_CARRÉE
				|| !(joueur.getWorld().getBlockEntity(pos) instanceof SelectorBlockEntity be)) {
			return;
		}
		ServerWorld monde = joueur.getServerWorld();
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
				be.nommer(nom.isEmpty() ? null : Text.literal(nom));
				ouvrir(joueur, pos);
			}
			default -> {
			}
		}
	}
}
