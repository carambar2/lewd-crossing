package dev.pyrofetch.jn1.photo;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.CameraStandBlockEntity;
import dev.pyrofetch.jn1.network.CameraAimPayload;
import dev.pyrofetch.jn1.network.PhotoDataPayload;
import dev.pyrofetch.jn1.network.PhotoRequestPayload;
import dev.pyrofetch.jn1.network.PhotoUploadPayload;
import dev.pyrofetch.jn1.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Le côté serveur de la photo : reçoit l'image envoyée par tranches depuis le
 * client, la vérifie, l'archive et remet le tirage ; sert les images aux
 * clients qui les demandent ; mémorise le réglage d'un trépied.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class PhotoHandler {
	private static final double PORTÉE_CARRÉE = 64.0;
	private static final Map<UUID, PhotoHandler.Montée> EN_COURS = new HashMap<>();
	/** Une photo par seconde au plus : chacune pèse jusqu'à 384 Ko dans l'archive, pour toujours. */
	private static final int CADENCE = 20;
	private static final Map<UUID, Integer> DERNIÈRE = new HashMap<>();

	private PhotoHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(
			PhotoUploadPayload.ID, (payload, context) -> context.server().execute(() -> recevoir(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(
			PhotoRequestPayload.ID, (payload, context) -> context.server().execute(() -> servir(context.player(), payload.numéro()))
		);
		ServerPlayNetworking.registerGlobalReceiver(
			CameraAimPayload.ID, (payload, context) -> context.server().execute(() -> viser(context.player(), payload))
		);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			EN_COURS.remove(handler.getPlayer().getUUID());
			DERNIÈRE.remove(handler.getPlayer().getUUID());
		});
	}

	private static void recevoir(ServerPlayer joueur, PhotoUploadPayload tranche) {
		PhotoHandler.Montée montée = EN_COURS.get(joueur.getUUID());
		if (tranche.décalage() == 0) {
			int maintenant = joueur.level().getServer().getTickCount();
			Integer avant = DERNIÈRE.get(joueur.getUUID());
			if (avant != null && maintenant - avant < CADENCE && maintenant >= avant || !appareilÀPortée(joueur, tranche.pose())) {
				EN_COURS.remove(joueur.getUUID());
				return;
			}
			DERNIÈRE.put(joueur.getUUID(), maintenant);
			montée = new PhotoHandler.Montée(tranche.session(), tranche.format(), tranche.pose());
			EN_COURS.put(joueur.getUUID(), montée);
		}

		if (montée != null && montée.session == tranche.session() && montée.format == tranche.format()) {
			if (tranche.décalage() == montée.reçu && tranche.décalage() + tranche.tranche().length <= montée.pixels.length) {
				System.arraycopy(tranche.tranche(), 0, montée.pixels, montée.reçu, tranche.tranche().length);
				montée.reçu = montée.reçu + tranche.tranche().length;
				if (tranche.dernière()) {
					EN_COURS.remove(joueur.getUUID());
					if (montée.reçu != montée.pixels.length) {
						JN1.LOGGER.warn("Photo tronquée de {} : {} octets sur {}.", new Object[]{joueur.getGameProfile().name(), montée.reçu, montée.pixels.length});
					} else {
						développer(joueur, new PhotoImage(montée.format, montée.pixels, joueur.getGameProfile().name()), montée.pose);
					}
				}
			} else {
				JN1.LOGGER
					.warn(
						"Tranche de photo incohérente de {} : décalage {}, {} octets, attendu {} sur {}. Montée abandonnée.",
						new Object[]{joueur.getGameProfile().name(), tranche.décalage(), tranche.tranche().length, montée.reçu, montée.pixels.length}
					);
				EN_COURS.remove(joueur.getUUID());
			}
		}
	}

	/**
	 * Une photo vient d'un appareil : en main, ou sur un trépied là où la vue
	 * a été prise (le retardateur part sans que le joueur tienne rien).
	 */
	private static boolean appareilÀPortée(ServerPlayer joueur, PhotoPose pose) {
		if (joueur.getMainHandItem().is(ModItems.CAMERA) || joueur.getOffhandItem().is(ModItems.CAMERA)) {
			return true;
		}
		if (pose == null || pose.vide()) {
			return false;
		}
		BlockPos œil = BlockPos.containing(pose.position());
		for (BlockPos près : BlockPos.betweenClosed(œil.offset(-1, -1, -1), œil.offset(1, 1, 1))) {
			if (joueur.level().getBlockEntity(près) instanceof CameraStandBlockEntity) {
				return true;
			}
		}
		return false;
	}

	private static void développer(ServerPlayer joueur, PhotoImage image, PhotoPose pose) {
		PhotoArchive archive = PhotoArchive.of(joueur.level());
		int numéro = archive.ajouter(image);
		// Qui est sur la photo, relevé maintenant : c'est ce qu'OnlyFaps paiera.
		archive.poserSujets(numéro, dev.pyrofetch.jn1.onlyfaps.Sujets.relever(joueur, pose));
		ItemStack tirage = PhotoItem.tirage(ModItems.PHOTO, numéro, image.format());
		if (!joueur.getInventory().add(tirage)) {
			joueur.drop(tirage, false, net.minecraft.util.Prediction.PREDICTED);
		}

		joueur.level().playSound((Player)null, joueur.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.6F);
		joueur.sendOverlayMessage(Component.translatable("photo.jn1.taken", numéro));
		JN1.LOGGER.debug("Photo {} développée pour {} ({} octets).", new Object[]{numéro, joueur.getGameProfile().name(), image.pixels().length});
	}

	private static void servir(ServerPlayer joueur, int numéro) {
		PhotoImage image = PhotoArchive.of(joueur.level()).get(numéro);
		if (image != null) {
			ServerPlayNetworking.send(joueur, new PhotoDataPayload(numéro, image));
		}
	}

	private static void viser(ServerPlayer joueur, CameraAimPayload réglage) {
		if (!(joueur.distanceToSqr(Vec3.atCenterOf(réglage.pied())) > 64.0)) {
			if (joueur.level().getBlockEntity(réglage.pied()) instanceof CameraStandBlockEntity pied) {
				pied.régler(réglage.lacet(), réglage.tangage(), réglage.retard(), réglage.format());
			}
		}
	}

	private static final class Montée {
		private final int session;
		private final PhotoFormat format;
		private final byte[] pixels;
		private final PhotoPose pose;
		private int reçu;

		private Montée(int session, PhotoFormat format, PhotoPose pose) {
			this.session = session;
			this.format = format;
			this.pose = pose;
			this.pixels = new byte[format.octets()];
		}
	}
}
