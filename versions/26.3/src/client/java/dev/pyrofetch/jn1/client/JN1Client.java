package dev.pyrofetch.jn1.client;

import java.util.List;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.hud.SpeechBubbles;
import dev.pyrofetch.jn1.client.render.CompanionRenderer;
import dev.pyrofetch.jn1.client.screen.JennyMenuScreen;
import dev.pyrofetch.jn1.client.screen.NoteScreen;
import dev.pyrofetch.jn1.config.JN1Config;
import dev.pyrofetch.jn1.network.ProgressSyncPayload;
import dev.pyrofetch.jn1.client.terminal.NoteSession;
import dev.pyrofetch.jn1.client.terminal.TerminalSession;
import dev.pyrofetch.jn1.network.OpenTerminalPayload;
import dev.pyrofetch.jn1.network.SpeakPayload;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModParticles;
import dev.pyrofetch.jn1.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class JN1Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Ce que nomment les définitions d'objets (assets/…/items) et que le jeu ne
		// connaît pas seul : à faire avant le premier chargement des ressources,
		// sinon la définition est refusée. En 1.21.1, les prédicats et teintes
		// d'objets (plats nappés, bocal à cum, néons) étaient enregistrés ici ;
		// en 26.3, les définitions s'en chargent avec les propriétés du jeu, et
		// seule la teinte des sous-vêtements (jn1:lingerie) est à nous.
		dev.pyrofetch.jn1.client.modeles.ModelesObjets.enregistrer();
		// Les modèles spéciaux nommés par les définitions du lidar et de la
		// capsule (jn1:lidar, jn1:capsule) : leur écran et leur sphère se
		// dessinent en direct.
		dev.pyrofetch.jn1.client.render.LidarItemRenderer.register();
		dev.pyrofetch.jn1.client.render.CapsuleItemRenderer.register();
		// Le verre du bocal, le bocal écrasé dans le compresseur, les traces de
		// cum, le feuillage du vide, le stabilisateur : en 26.3, le jeu choisit
		// seul la couche de rendu d'un bloc d'après la transparence de ses
		// planches (plus de BlockRenderLayerMap). Rien à déclarer.

		// La boutique de Gaster : ouverte au toucher, mise à jour après un achat.
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.GasterShopPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().gui.screen() instanceof dev.pyrofetch.jn1.client.gaster.GasterShopScreen écran
							&& écran.entité() == payload.entityId()) {
						écran.mettreÀJour(payload);
					} else {
						context.client().gui.setScreen(new dev.pyrofetch.jn1.client.gaster.GasterShopScreen(payload));
					}
				}));

		// Le vide du laboratoire : noir complet, sans ciel ni astres, ni nuages —
		// en 26.3, c'est le type de dimension (données) qui le dit. Le couloir
		// entre les univers, le ciel du vide, est une passe du rendu du monde
		// (LevelRendererSkyMixin, lab/VoidSky) ; et ce qui y dérive.
		dev.pyrofetch.jn1.client.lab.VoidDebris.register();
		dev.pyrofetch.jn1.client.jouets.PogoClient.register();
		dev.pyrofetch.jn1.client.onlyfaps.OnlyFapsClient.register();
		// Les vues 3D des écrans (portraits, figures du carnet) : une texture par vue.
		dev.pyrofetch.jn1.client.screen.Vignettes.register();

		EntityRendererRegistry.register(ModEntities.JENNY, CompanionRenderer::new);
		EntityRendererRegistry.register(ModEntities.ELLIE, CompanionRenderer::new);
		EntityRendererRegistry.register(ModEntities.AURA, CompanionRenderer::new);
		EntityRendererRegistry.register(ModEntities.GASTER, dev.pyrofetch.jn1.client.gaster.GasterRenderer::new);
		dev.pyrofetch.jn1.client.mesh.Maillage.register();
		// La capsule en vol est rendue comme son objet : elle tourne sur elle-même.
		EntityRendererRegistry.register(ModEntities.CAPSULE, ThrownItemRenderer::new);
		// La flèche collante : celle du jeu, sa planche nappée ; la fiole à jeter, son objet.
		EntityRendererRegistry.register(ModEntities.CUM_ARROW, dev.pyrofetch.jn1.client.render.CumArrowRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPLASH_CUM, ThrownItemRenderer::new);
		MenuScreens.register(ModScreenHandlers.JENNY, JennyMenuScreen::new);
		MenuScreens.register(ModScreenHandlers.VENDING_EDIT,
				dev.pyrofetch.jn1.client.screen.VendingEditScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(SpeakPayload.ID, (payload, context) ->
				context.client().execute(() -> {
					SpeechBubbles.show(payload.entityId(), payload.text(), payload.durationTicks());
					if (JN1Config.get().actionbarEcho()) {
						context.client().gui.hud.setOverlayMessage(payload.text(), false);
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.HugPayload.ID,
				(payload, context) -> context.client().execute(
						() -> dev.pyrofetch.jn1.client.interaction.HugSession.begin(payload.ticks())));

		ClientPlayNetworking.registerGlobalReceiver(ProgressSyncPayload.ID, (payload, context) ->
				context.client().execute(() -> ClientProgress.update(payload.flags())));

		ClientPlayNetworking.registerGlobalReceiver(OpenTerminalPayload.ID, (payload, context) ->
				context.client().execute(() -> TerminalSession.begin(
						payload.pos(), net.minecraft.core.Direction.from3DDataValue(payload.facing()))));

		ClientPlayNetworking.registerGlobalReceiver(
				dev.pyrofetch.jn1.network.OpenNotePayload.ID, (payload, context) ->
						context.client().execute(() -> NoteSession.begin(payload.pos(),
								AttachFace.values()[payload.face()],
								net.minecraft.core.Direction.from3DDataValue(payload.facing()))));

		// L'appareil photo : le cadre qui montre les tirages, l'ouverture du
		// viseur, les images reçues du serveur, la capture en fin d'image et
		// l'affichage du viseur.
		BlockEntityRendererRegistry.register(dev.pyrofetch.jn1.registry.ModBlockEntities.PHOTO_FRAME,
				dev.pyrofetch.jn1.client.render.PhotoFrameRenderer::new);
		BlockEntityRendererRegistry.register(dev.pyrofetch.jn1.registry.ModBlockEntities.CAMERA_STAND,
				dev.pyrofetch.jn1.client.render.CameraStandRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenCameraPayload.ID,
				(payload, context) -> context.client().execute(() ->
						dev.pyrofetch.jn1.client.camera.CameraSession.begin(payload.surPied(), payload.pied())));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.PhotoDataPayload.ID,
				(payload, context) -> context.client().execute(() ->
						dev.pyrofetch.jn1.client.photo.PhotoTextures.reçue(payload.numéro(), payload.image())));
		// La photo est lue à la fin du rendu du monde, avant la main et l'interface.
		LevelRenderEvents.END_MAIN.register(
				contexte -> dev.pyrofetch.jn1.client.camera.PhotoCapture.auRenduTerminé());
		// Pas de contour de bloc visé dans le viseur ni sur les photos.
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(
				(contexte, visé) -> !dev.pyrofetch.jn1.client.camera.CameraSession.isActive()
						&& !dev.pyrofetch.jn1.client.camera.CameraSession.vueDéportée()
						&& !dev.pyrofetch.jn1.client.camera.PhotoCapture.demandée());
		// Les bulles des filles : ancrées à la relève des entités, retenues à la
		// fin de la relève du monde avec la caméra, dessinées sous le reste de
		// l'interface de la même image.
		LevelRenderEvents.END_EXTRACTION.register(
				contexte -> dev.pyrofetch.jn1.client.hud.BubbleOverlay.retenir(contexte.levelState().cameraRenderState));
		// L'interface en jeu, par-dessus celle du jeu (comme HudRenderCallback en 1.21.1).
		HudElementRegistry.addLast(JN1.id("bubbles"),
				(contexte, compteur) -> dev.pyrofetch.jn1.client.hud.BubbleOverlay.render(contexte));
		HudElementRegistry.addLast(JN1.id("viewfinder"),
				(contexte, compteur) -> dev.pyrofetch.jn1.client.camera.Viewfinder.render(
						contexte, compteur.getGameTimeDeltaPartialTick(false)));
		HudElementRegistry.addLast(JN1.id("excitation"),
				(contexte, compteur) -> dev.pyrofetch.jn1.client.excitation.ExcitationBar.render(
						contexte, compteur.getGameTimeDeltaPartialTick(false)));
		// Le rythme d'une demande, réglé par le partenaire (avancer / reculer).
		dev.pyrofetch.jn1.client.excitation.RythmeDemande.register();
		HudElementRegistry.addLast(JN1.id("pace"),
				(contexte, compteur) -> dev.pyrofetch.jn1.client.excitation.RythmeDemande.render(contexte));
		HudElementRegistry.addLast(JN1.id("orgasm"),
				(contexte, compteur) -> dev.pyrofetch.jn1.client.excitation.OrgasmeEcran.render(
						contexte, compteur.getGameTimeDeltaPartialTick(false)));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OrgasmePayload.ID,
				(payload, context) -> context.client().execute(dev.pyrofetch.jn1.client.excitation.OrgasmeEcran::déclencher));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.ExcitationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> dev.pyrofetch.jn1.client.excitation.ExcitationBar.recevoir(payload.niveau())));
		dev.pyrofetch.jn1.client.gender.GenderScreen.register();
		// Les sous-vêtements prennent la couleur de la fille qui les portait :
		// teinte jn1:lingerie de leur définition d'objet (ModelesObjets).
		CameraKeys.register();

		// Les néons : un seul modèle, teinté par bloc ; en main, la teinte
		// constante de leur définition d'objet.
		for (var néon : dev.pyrofetch.jn1.registry.ModBlocks.NEONS.entrySet()) {
			int teinte = 0xFF000000 | dev.pyrofetch.jn1.registry.ModBlocks.NEON_COLOURS.get(néon.getKey());
			BlockTintSource source = état -> teinte;
			BlockColorRegistry.register(List.of(source), néon.getValue());
		}

		EntityRendererRegistry.register(ModEntities.RIFT, dev.pyrofetch.jn1.client.render.RiftRenderer::new);
		BlockEntityRendererRegistry.register(dev.pyrofetch.jn1.registry.ModBlockEntities.RIFT_STABILIZER,
				dev.pyrofetch.jn1.client.render.RiftStabilizerRenderer::new);
		EntityRendererRegistry.register(ModEntities.LIFT_PLATFORM,
				dev.pyrofetch.jn1.client.render.PlatformEntityRenderer::new);
		// La plateforme d'ascenseur se camoufle : son modèle dessine celui du bloc
		// qu'elle imite, et sa teinte (herbe, feuilles) est celle de ce bloc.
		net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(contexte ->
				contexte.modifyBlockModelAfterBake().register((modèle, ctx) ->
						modèle != null && ctx.state().is(dev.pyrofetch.jn1.registry.ModBlocks.LIFT_PLATFORM)
								? new dev.pyrofetch.jn1.client.render.CamoModel(modèle) : modèle));
		BlockColorRegistry.register((state, world, pos, teintes) -> {
			if (world != null && pos != null && world.getBlockEntityRenderData(pos) instanceof BlockState imité) {
				var fabrique = BlockColorRegistry.getFactory(imité);
				if (fabrique != null) {
					fabrique.collect(imité, world, pos, teintes);
				} else {
					for (BlockTintSource source : Minecraft.getInstance().getBlockColors().getTintSources(imité)) {
						teintes.add(source.colorInWorld(imité, world, pos));
					}
				}
			}
		}, dev.pyrofetch.jn1.registry.ModBlocks.LIFT_PLATFORM);
		MenuScreens.register(dev.pyrofetch.jn1.registry.ModScreenHandlers.LIFT_PLATFORM,
				dev.pyrofetch.jn1.client.screen.PlatformScreen::new);
		BlockEntityRendererRegistry.register(dev.pyrofetch.jn1.registry.ModBlockEntities.WALL_SCREEN,
				dev.pyrofetch.jn1.client.render.WallScreenRenderer::new);
		// Le corps du lidar et l'appareil du trépied sont des modèles de bloc
		// qu'aucun état ne réclame : sans cette demande, le jeu ne les
		// chargerait pas.
		net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(contexte -> {
			dev.pyrofetch.jn1.client.render.LidarItemRenderer.ajouterModèle(contexte);
			dev.pyrofetch.jn1.client.render.CameraStandRenderer.ajouterModèle(contexte);
		});

		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenVendingPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().gui.setScreen(
						new dev.pyrofetch.jn1.client.screen.VendingScreen(payload.pos()))));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenPanelPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().gui.setScreen(
						new NoteScreen(payload.pos(), NoteScreen.Support.ÉCRAN))));
		// L'écran d'une compagne, au clic main vide.
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenCompanionPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().level != null && context.client().level.getEntity(payload.entityId())
							instanceof dev.pyrofetch.jn1.entity.CompanionEntity elle) {
						context.client().gui.setScreen(new dev.pyrofetch.jn1.client.screen.CompanionScreen(
								elle, payload.affection(), payload.salut()));
					}
				}));
		// Sa réponse à « Talk » : à l'écran ouvert pour elle.
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.TalkPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().gui.screen() instanceof dev.pyrofetch.jn1.client.screen.TalkScreen écran
							&& écran.compagne().getId() == payload.entityId()) {
						écran.recevoir(payload);
					}
				}));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenNotebookPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().gui.setScreen(
						new dev.pyrofetch.jn1.client.notebook.NotebookScreen())));
		// La question d'âge, au premier lancement avec le mod.
		dev.pyrofetch.jn1.client.agegate.AgeGate.register();
		// La Horny Potion : écran de choix, apparence des joueurs, bouton d'inventaire.
		dev.pyrofetch.jn1.client.morph.MorphClient.register();
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenScenesPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().gui.setScreen(
						new dev.pyrofetch.jn1.client.screen.SceneScreen(payload))));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenSelectorPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().gui.screen() instanceof dev.pyrofetch.jn1.client.screen.SelectorScreen écran
							&& écran.pos().equals(payload.pos())) {
						écran.mettreÀJour(payload);
					} else {
						context.client().gui.setScreen(new dev.pyrofetch.jn1.client.screen.SelectorScreen(payload));
					}
				}));
		// Le lidar : sa touche de vue 3D, les échos reçus du serveur, le
		// battement de ses impulsions, et l'objet lui-même, dont l'écran se
		// dessine en direct (rendu spécial jn1:lidar, nommé par sa définition d'objet).
		dev.pyrofetch.jn1.client.lab.LidarTracker.register();
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.LidarPayload.ID,
				(payload, context) -> context.client().execute(
						() -> dev.pyrofetch.jn1.client.lab.LidarTracker.recevoir(payload.échos())));
		ClientTickEvents.END_CLIENT_TICK.register(dev.pyrofetch.jn1.client.lab.LidarTracker::tick);
		// L'objet lui-même (rendu spécial jn1:lidar) et la capsule en vraie
		// sphère (jn1:capsule) : enregistrés en tête, avant les ressources.

		// Les sons qui durent : radio des terminaux, bourdon des failles,
		// moteur des cabines.
		dev.pyrofetch.jn1.terminal.Radio.setClientTicker(
				dev.pyrofetch.jn1.client.sound.ModSoundInstances::radio);
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_LOAD.register(
				(entité, monde) -> dev.pyrofetch.jn1.client.sound.ModSoundInstances.entitéChargée(entité));

		ParticleProviderRegistry.getInstance().register(ModParticles.GLYPH_MOTE, EndRodParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(ModParticles.SILENCE, EndRodParticle.Provider::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			dev.pyrofetch.jn1.client.gaster.GasterAdieu.tick(client);
			SpeechBubbles.tick();
			TerminalSession.tick();
			NoteSession.tick();
			dev.pyrofetch.jn1.client.interaction.HugSession.tick();
			dev.pyrofetch.jn1.client.camera.CameraSession.tick();
			CameraKeys.tick();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SpeechBubbles.clear();
			ClientProgress.reset();
			dev.pyrofetch.jn1.client.interaction.HugSession.reset();
			TerminalSession.clear();
			NoteSession.clear();
			dev.pyrofetch.jn1.client.sound.ModSoundInstances.clear();
			dev.pyrofetch.jn1.client.camera.CameraSession.clear();
			dev.pyrofetch.jn1.client.photo.PhotoTextures.clear();
			dev.pyrofetch.jn1.client.morph.ClientMorphs.clear();
			dev.pyrofetch.jn1.client.morph.MorphHands.clear();
		});

		JN1.LOGGER.info("[J.N-1] Client prêt.");
	}
}
