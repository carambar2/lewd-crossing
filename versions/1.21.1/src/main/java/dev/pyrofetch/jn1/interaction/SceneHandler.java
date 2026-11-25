package dev.pyrofetch.jn1.interaction;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionScenes;
import dev.pyrofetch.jn1.network.OpenScenesPayload;
import dev.pyrofetch.jn1.network.ScenePayload;
import dev.pyrofetch.jn1.network.SceneJoinPayload;
import dev.pyrofetch.jn1.network.ScenePropPayload;
import dev.pyrofetch.jn1.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * La télécommande de scènes : elle ouvre la liste, puis fait jouer ce qu'on y
 * choisit.
 *
 * <p>L'écran reste ouvert pendant qu'elle joue, pour passer d'une scène à
 * l'autre en la regardant : la portée est donc celle du regard, pas du bras.
 * On y choisit aussi de regarder de l'extérieur, en gardant la main sur son
 * personnage, ou de prendre la place de {@code steve} ({@link CompanionEntity#joinScene}).
 */
public final class SceneHandler {
	private static final double MAX_RANGE_SQUARED = 24.0 * 24.0;

	private SceneHandler() {
	}

	public static void register() {
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> OUVERTS.remove(handler.player.getUuid()));
		ServerPlayNetworking.registerGlobalReceiver(ScenePayload.ID,
				(payload, context) -> context.player().server.execute(
						() -> handle(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(SceneJoinPayload.ID,
				(payload, context) -> context.player().server.execute(() -> {
					CompanionEntity jenny = cible(context.player(), payload.entityId());
					if (jenny == null) {
						return;
					}
					if (payload.join()) {
						jenny.joinScene(context.player());
					} else if (jenny.isScenePartner(context.player())) {
						jenny.leaveScene();
					}
				}));
		ServerPlayNetworking.registerGlobalReceiver(ScenePropPayload.ID,
				(payload, context) -> context.player().server.execute(() -> {
					CompanionEntity jenny = cible(context.player(), payload.entityId());
					if (jenny != null) {
						jenny.setScenePropShown(payload.visible());
					}
				}));
	}

	/**
	 * La compagne dont chaque joueur a ouvert le choix des scènes en dernier :
	 * par la télécommande ou par le menu de la fille. Sans télécommande en main,
	 * c'est ce qui rend recevables les demandes de l'écran.
	 */
	private static final java.util.Map<java.util.UUID, Integer> OUVERTS = new java.util.HashMap<>();

	/** Ouvre le choix des scènes, ou dit qu'elle n'en a aucune. */
	public static void open(ServerPlayerEntity player, CompanionEntity jenny) {
		List<String> scènes = CompanionScenes.choisissables(jenny.kind());
		OUVERTS.put(player.getUuid(), jenny.getId());
		if (scènes.isEmpty()) {
			player.sendMessage(Text.translatable("message.jn1.scene_remote.none", jenny.getDisplayName()), true);
			return;
		}
		ServerPlayNetworking.send(player, new OpenScenesPayload(jenny.getId(), scènes, jenny.getScene(),
				jenny.kind().hasSceneProp() ? jenny.kind().sceneProp() : "", jenny.isScenePropShown(),
				jenny.isScenePartner(player)));
	}

	/** La compagne visée, si la demande est recevable ; sinon {@code null}. */
	@org.jetbrains.annotations.Nullable
	private static CompanionEntity cible(ServerPlayerEntity player, int entityId) {
		Entity target = player.getWorld().getEntityById(entityId);
		if (!(target instanceof CompanionEntity jenny)) {
			return null;
		}
		if (player.squaredDistanceTo(jenny) > MAX_RANGE_SQUARED) {
			return null;
		}
		// L'écran s'ouvre avec la télécommande en main, ou par le menu de la
		// fille ; un client qui enverrait la demande sans l'un ni l'autre vient
		// d'ailleurs que de l'écran.
		boolean télécommande = player.getMainHandStack().isOf(ModItems.SCENE_REMOTE)
				|| player.getOffHandStack().isOf(ModItems.SCENE_REMOTE);
		if (!télécommande && !Integer.valueOf(entityId).equals(OUVERTS.get(player.getUuid()))) {
			return null;
		}
		return jenny;
	}

	private static void handle(ServerPlayerEntity player, ScenePayload payload) {
		CompanionEntity jenny = cible(player, payload.entityId());
		if (jenny == null) {
			return;
		}
		// Un second joueur ne détourne pas la scène d'un partenaire en place.
		if (jenny.getScenePartner().isPresent() && !jenny.isScenePartner(player)) {
			return;
		}
		if (payload.scène().isEmpty()) {
			jenny.stopScene();
		} else if (CompanionScenes.choisissables(jenny.kind()).contains(payload.scène())) {
			jenny.playScene(payload.scène(), player);
		}
	}
}
