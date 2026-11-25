package dev.pyrofetch.jn1.interaction;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionGesture;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Les gestes demandés depuis le menu de Jenny.
 *
 * <p>Chaque geste a son seuil d'affection, et le bouton reste grisé en dessous.
 * Le seuil est malgré tout revérifié ici : un client modifié pourrait envoyer la
 * demande sans jamais avoir affiché le bouton.
 */
public final class JennyActionHandler {
	/** Il faut être à portée de bras. */
	private static final double MAX_RANGE_SQUARED = 36.0;

	private JennyActionHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(JennyActionPayload.ID,
				(payload, context) -> context.server().execute(
						() -> handle(context.player(), payload)));
	}

	private static void handle(ServerPlayer player, JennyActionPayload payload) {
		Entity target = player.level().getEntity(payload.entityId());
		if (!(target instanceof CompanionEntity jenny)) {
			return;
		}
		if (player.distanceToSqr(jenny) > MAX_RANGE_SQUARED) {
			return;
		}
		int trust = ModAttachments.get(player).trust(jenny.kind().id());
		switch (payload.action()) {
			case JennyActionPayload.ACTION_HUG -> {
				// En scène, elle ne se retourne pas : la scène se désalignait.
				if (trust >= PlayerProgress.hugTrust(jenny.kind()) && jenny.kind().saitCâliner()
						&& !jenny.hasScene()) {
					hug(player, jenny);
					Conversation.événement(player, jenny, "hug");
				}
			}
			case JennyActionPayload.ACTION_OUTFIT -> {
				if (trust >= PlayerProgress.CHANGE_TRUST && jenny.kind().outfits().contains(payload.argument())
						&& !payload.argument().equals(jenny.getOutfit())) {
					String avant = jenny.getOutfit();
					jenny.setOutfit(payload.argument());
					dev.pyrofetch.jn1.jouets.Lingerie.changer(jenny, avant, jenny.getOutfit(), player);
					jenny.showHearts(6);
					java.util.List<String> tenues = jenny.kind().outfits();
					boolean plusLégère = tenues.indexOf(jenny.getOutfit()) > Math.max(0, tenues.indexOf(avant));
					Conversation.événement(player, jenny, plusLégère ? "undress" : "dress");
				}
			}
			// Le choix des scènes morceau par morceau ne s'ouvre plus du menu : il
			// reste à la télécommande, pour les essais.
			case JennyActionPayload.ACTION_DEMANDE -> demander(player, jenny, trust, payload.argument());
			case JennyActionPayload.ACTION_INVENTAIRE ->
					player.openMenu(new dev.pyrofetch.jn1.screen.JennyScreenFactory(jenny));
			case JennyActionPayload.ACTION_SUIVRE -> jenny.basculerSuivi(player);
			case JennyActionPayload.ACTION_PARLER -> Conversation.parler(player, jenny, payload.argument());
			case JennyActionPayload.ACTION_RYTHME -> {
				// Seul le partenaire de la scène en règle le rythme.
				if (jenny.isScenePartner(player)) {
					jenny.changerRythme(payload.argument().equals("+") ? 1 : -1);
				}
			}
			default -> {
				// Une action inconnue vient forcément d'ailleurs que de l'écran.
			}
		}
	}

	/**
	 * Une demande du menu : revérifiée ici (seuil, brutalité au maximum, ce
	 * qu'elle sait jouer), et refusée si elle est occupée — en scène avec un
	 * autre, menottée, en veille.
	 */
	private static void demander(ServerPlayer player, CompanionEntity jenny, int trust, String argument) {
		boolean brutale = argument.endsWith("!");
		dev.pyrofetch.jn1.entity.Demande demande = dev.pyrofetch.jn1.entity.Demande.byId(
				brutale ? argument.substring(0, argument.length() - 1) : argument);
		if (demande == null || trust < demande.seuil() || (brutale && trust < PlayerProgress.BRUTAL_TRUST)
				|| !demande.possible(jenny.kind())) {
			return;
		}
		if (jenny.isCuffed() || jenny.isPassenger()
				|| (jenny.getScenePartner().isPresent() && !jenny.isScenePartner(player))
				|| jenny.estEnVeille()) {
			return;
		}
		jenny.commencerDemande(player, demande, brutale);
	}

	/**
	 * Du milieu du joueur au sien, en blocs, pendant le câlin : la valeur
	 * d'après laquelle ses bras sont calculés ({@code tools/gen_hug.py}, ÉCART,
	 * 8,5 pixels : sa poitrine contre lui) — à changer aux deux endroits.
	 */
	private static final double ÉCART_CÂLIN = 8.5 / 16.0;

	private static void hug(ServerPlayer player, CompanionEntity jenny) {
		// De face (0.56.9) :
		// elle vient juste devant lui, tournée vers lui, et lui passe les bras
		// autour du cou. (0.56.5 et 0.56.6 : côte à côte, le bras sur ses
		// épaules ; avant, elle enlaçait un mannequin masqué.) Faute de place
		// devant lui, elle reste où elle est, tournée vers lui.
		jenny.getNavigation().stop();
		jenny.retenirPlaceAvantCâlin();
		float cap = player.getYRot();
		double rad = Math.toRadians(cap);
		net.minecraft.world.phys.Vec3 place = player.position().add(
				-Math.sin(rad) * ÉCART_CÂLIN, 0.0, Math.cos(rad) * ÉCART_CÂLIN);
		float face = cap + 180.0F;
		if (jenny.level().noCollision(jenny, jenny.getDimensions(jenny.getPose()).makeBoundingBox(place))) {
			jenny.snapTo(place.x, place.y, place.z, face, 0.0F);
		} else {
			face = (float) Math.toDegrees(Math.atan2(player.getZ() - jenny.getZ(), player.getX() - jenny.getX())) - 90.0F;
		}
		jenny.setYRot(face);
		jenny.setYBodyRot(face);
		jenny.setYHeadRot(face);

		jenny.playGesture(CompanionGesture.HUG);
		// Pas d'expression « ravie » ici (0.85.2) : elle lui plissait les yeux en
		// deux arcs pendant tout le geste, et le câlin la veut les yeux dans ses
		// yeux — l'animation porte son propre visage (yeux ouverts, sourire, joues).
		jenny.showHearts(12);

		// Le joueur reste en place le temps du geste : elle vient l'enlacer, et
		// le voir s'éloigner en courant pendant ce temps n'aurait aucun sens.
		// La durée vient du serveur, seul à savoir quand le geste s'achève.
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
				new dev.pyrofetch.jn1.network.HugPayload(CompanionGesture.HUG.durationTicks()));
	}
}
