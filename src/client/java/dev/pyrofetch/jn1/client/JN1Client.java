package dev.pyrofetch.jn1.client;

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
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import dev.pyrofetch.jn1.capsule.CapsuleItem;
import dev.pyrofetch.jn1.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.particle.EndRodParticle;

public class JN1Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Un plat « au cum » prend sa planche nappée : le prédicat vaut pour tous
		// les objets, et les modèles des nourritures y substituent la leur.
		FabricModelPredicateProviderRegistry.register(JN1.id("au_cum"),
				(stack, world, entity, seed) -> dev.pyrofetch.jn1.cum.AuCumRecipe.déjàAuCum(stack) ? 1.0F : 0.0F);

		// Le bocal à cum en main suit son remplissage, et montre sa figurine.
		FabricModelPredicateProviderRegistry.register(ModItems.CUM_JAR, JN1.id("remplissage"),
				(stack, world, entity, seed) -> dev.pyrofetch.jn1.cum.CumJarItem.niveau(stack) / 8.0F);
		FabricModelPredicateProviderRegistry.register(ModItems.CUM_JAR, JN1.id("figurine"),
				(stack, world, entity, seed) -> dev.pyrofetch.jn1.cum.CumJarItem.figurine(stack) ? 1.0F : 0.0F);
		// Le verre du bocal, et le bocal écrasé dans le compresseur : translucides.
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
				net.minecraft.client.render.RenderLayer.getTranslucent(),
				dev.pyrofetch.jn1.registry.ModBlocks.CUM_JAR,
				dev.pyrofetch.jn1.registry.ModBlocks.COMPRESSOR);
		// La boutique de Gaster : ouverte au toucher, mise à jour après un achat.
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.GasterShopPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().currentScreen instanceof dev.pyrofetch.jn1.client.gaster.GasterShopScreen écran
							&& écran.entité() == payload.entityId()) {
						écran.mettreÀJour(payload);
					} else {
						context.client().setScreen(new dev.pyrofetch.jn1.client.gaster.GasterShopScreen(payload));
					}
				}));

		// Le vide du laboratoire : noir complet, sans ciel ni astres.
		DimensionRenderingRegistry.registerDimensionEffects(JN1.id("lab"),
				new dev.pyrofetch.jn1.client.lab.VoidEffects());
		// Le couloir entre les univers : le ciel du vide, et ce qui y dérive.
		DimensionRenderingRegistry.registerSkyRenderer(dev.pyrofetch.jn1.lab.LabDimension.LAB,
				new dev.pyrofetch.jn1.client.lab.VoidSky());
		dev.pyrofetch.jn1.client.lab.VoidDebris.register();
		dev.pyrofetch.jn1.client.jouets.PogoClient.register();
		dev.pyrofetch.jn1.client.onlyfaps.OnlyFapsClient.register();

		EntityRendererRegistry.register(ModEntities.JENNY, CompanionRenderer::new);
		EntityRendererRegistry.register(ModEntities.ELLIE, CompanionRenderer::new);
		EntityRendererRegistry.register(ModEntities.AURA, CompanionRenderer::new);
		EntityRendererRegistry.register(ModEntities.GASTER, dev.pyrofetch.jn1.client.gaster.GasterRenderer::new);
		dev.pyrofetch.jn1.client.mesh.Maillage.register();
		// La capsule en vol est rendue comme son objet : elle tourne sur elle-même.
		EntityRendererRegistry.register(ModEntities.CAPSULE,
				context -> new net.minecraft.client.render.entity.FlyingItemEntityRenderer<>(context));
		// La flèche collante : celle du jeu, sa planche nappée ; la fiole à jeter, son objet.
		EntityRendererRegistry.register(ModEntities.CUM_ARROW, dev.pyrofetch.jn1.client.render.CumArrowRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPLASH_CUM,
				context -> new net.minecraft.client.render.entity.FlyingItemEntityRenderer<>(context));
		HandledScreens.register(ModScreenHandlers.JENNY, JennyMenuScreen::new);
		HandledScreens.register(ModScreenHandlers.VENDING_EDIT,
				dev.pyrofetch.jn1.client.screen.VendingEditScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(SpeakPayload.ID, (payload, context) ->
				context.client().execute(() -> {
					SpeechBubbles.show(payload.entityId(), payload.text(), payload.durationTicks());
					if (JN1Config.get().actionbarEcho()) {
						context.client().inGameHud.setOverlayMessage(payload.text(), false);
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.HugPayload.ID,
				(payload, context) -> context.client().execute(
						() -> dev.pyrofetch.jn1.client.interaction.HugSession.begin(payload.ticks())));

		ClientPlayNetworking.registerGlobalReceiver(ProgressSyncPayload.ID, (payload, context) ->
				context.client().execute(() -> ClientProgress.update(payload.flags())));

		ClientPlayNetworking.registerGlobalReceiver(OpenTerminalPayload.ID, (payload, context) ->
				context.client().execute(() -> TerminalSession.begin(
						payload.pos(), net.minecraft.util.math.Direction.byId(payload.facing()))));

		ClientPlayNetworking.registerGlobalReceiver(
				dev.pyrofetch.jn1.network.OpenNotePayload.ID, (payload, context) ->
						context.client().execute(() -> NoteSession.begin(payload.pos(),
								net.minecraft.block.enums.BlockFace.values()[payload.face()],
								net.minecraft.util.math.Direction.byId(payload.facing()))));

		// Le feuillage a des trous : sans couche de rendu découpée, le jeu
		// dessine ses pixels transparents en noir plein.
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
				net.minecraft.client.render.RenderLayer.getCutoutMipped(),
				dev.pyrofetch.jn1.registry.ModBlocks.VOID_LEAVES,
				dev.pyrofetch.jn1.registry.ModBlocks.VOID_LEAVES_DEEP);

		// Les traces de cum sont translucides.
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
				net.minecraft.client.render.RenderLayer.getTranslucent(),
				dev.pyrofetch.jn1.registry.ModBlocks.CUM_TRACE);

		// L'appareil photo : le cadre qui montre les tirages, l'ouverture du
		// viseur, les images reçues du serveur, la capture en fin d'image et
		// l'affichage du viseur.
		net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
				dev.pyrofetch.jn1.registry.ModBlockEntities.PHOTO_FRAME,
				dev.pyrofetch.jn1.client.render.PhotoFrameRenderer::new);
		net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
				dev.pyrofetch.jn1.registry.ModBlockEntities.CAMERA_STAND,
				dev.pyrofetch.jn1.client.render.CameraStandRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenCameraPayload.ID,
				(payload, context) -> context.client().execute(() ->
						dev.pyrofetch.jn1.client.camera.CameraSession.begin(payload.surPied(), payload.pied())));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.PhotoDataPayload.ID,
				(payload, context) -> context.client().execute(() ->
						dev.pyrofetch.jn1.client.photo.PhotoTextures.reçue(payload.numéro(), payload.image())));
		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(
				contexte -> dev.pyrofetch.jn1.client.camera.PhotoCapture.auRenduTerminé());
		// Pas de contour de bloc visé dans le viseur ni sur les photos.
		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(
				(contexte, visé) -> !dev.pyrofetch.jn1.client.camera.CameraSession.isActive()
						&& !dev.pyrofetch.jn1.client.camera.CameraSession.vueDéportée()
						&& !dev.pyrofetch.jn1.client.camera.PhotoCapture.demandée());
		// Les bulles des filles : relevées au rendu du monde, dessinées sous le reste de l'interface.
		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(
				contexte -> dev.pyrofetch.jn1.client.hud.BubbleOverlay.nouvelleImage());
		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(
				contexte -> dev.pyrofetch.jn1.client.hud.BubbleOverlay.retenir(contexte.positionMatrix(),
						contexte.projectionMatrix(), contexte.camera().getPos()));
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(contexte, compteur) -> dev.pyrofetch.jn1.client.hud.BubbleOverlay.render(contexte));
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(contexte, compteur) -> dev.pyrofetch.jn1.client.camera.Viewfinder.render(
						contexte, compteur.getTickDelta(false)));
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(contexte, compteur) -> dev.pyrofetch.jn1.client.excitation.ExcitationBar.render(
						contexte, compteur.getTickDelta(false)));
		// Le rythme d'une demande, réglé par le partenaire (avancer / reculer).
		dev.pyrofetch.jn1.client.excitation.RythmeDemande.register();
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(contexte, compteur) -> dev.pyrofetch.jn1.client.excitation.RythmeDemande.render(contexte));
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(contexte, compteur) -> dev.pyrofetch.jn1.client.excitation.OrgasmeEcran.render(
						contexte, compteur.getTickDelta(false)));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OrgasmePayload.ID,
				(payload, context) -> context.client().execute(dev.pyrofetch.jn1.client.excitation.OrgasmeEcran::déclencher));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.ExcitationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> dev.pyrofetch.jn1.client.excitation.ExcitationBar.recevoir(payload.niveau())));
		dev.pyrofetch.jn1.client.gender.GenderScreen.register();
		// Les sous-vêtements prennent la couleur de la fille qui les portait.
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
				(stack, index) -> index == 0 ? 0xFF000000 | dev.pyrofetch.jn1.jouets.Lingerie.couleur(stack) : -1,
				dev.pyrofetch.jn1.registry.ModItems.PANTIES, dev.pyrofetch.jn1.registry.ModItems.BRA);
		CameraKeys.register();

		// Les néons : un seul modèle, teinté par bloc et par objet.
		for (var néon : dev.pyrofetch.jn1.registry.ModBlocks.NEONS.entrySet()) {
			int teinte = 0xFF000000 | dev.pyrofetch.jn1.registry.ModBlocks.NEON_COLOURS.get(néon.getKey());
			net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
					(state, world, pos, index) -> index == 0 ? teinte : -1, néon.getValue());
			net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
					(stack, index) -> index == 0 ? teinte : -1, néon.getValue().asItem());
		}

		EntityRendererRegistry.register(ModEntities.RIFT, dev.pyrofetch.jn1.client.render.RiftRenderer::new);
		net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
				dev.pyrofetch.jn1.registry.ModBlockEntities.RIFT_STABILIZER,
				dev.pyrofetch.jn1.client.render.RiftStabilizerRenderer::new);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				dev.pyrofetch.jn1.registry.ModBlocks.RIFT_STABILIZER, net.minecraft.client.render.RenderLayer.getCutout());
		EntityRendererRegistry.register(ModEntities.LIFT_PLATFORM,
				dev.pyrofetch.jn1.client.render.PlatformEntityRenderer::new);
		// La plateforme d'ascenseur se camoufle : son modèle dessine celui du bloc
		// qu'elle imite, et sa teinte (herbe, feuilles) est celle de ce bloc.
		net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(contexte ->
				contexte.modifyModelAfterBake().register((modèle, ctx) ->
						modèle != null && ctx.topLevelId() != null
								&& ctx.topLevelId().id().equals(dev.pyrofetch.jn1.JN1.id("lift_platform"))
								&& !"inventory".equals(ctx.topLevelId().getVariant())
								? new dev.pyrofetch.jn1.client.render.CamoModel(modèle) : modèle));
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, world, pos, index) ->
				world != null && pos != null && world.getBlockEntityRenderData(pos) instanceof net.minecraft.block.BlockState imité
						? net.minecraft.client.MinecraftClient.getInstance().getBlockColors().getColor(imité, world, pos, index)
						: -1, dev.pyrofetch.jn1.registry.ModBlocks.LIFT_PLATFORM);
		HandledScreens.register(dev.pyrofetch.jn1.registry.ModScreenHandlers.LIFT_PLATFORM,
				dev.pyrofetch.jn1.client.screen.PlatformScreen::new);
		net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
				dev.pyrofetch.jn1.registry.ModBlockEntities.WALL_SCREEN,
				dev.pyrofetch.jn1.client.render.WallScreenRenderer::new);
		// Le corps du lidar et l'appareil du trépied sont des modèles de bloc
		// qu'aucun état ne réclame : sans cette demande, le jeu ne les
		// chargerait pas.
		net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(contexte -> contexte.addModels(
				dev.pyrofetch.jn1.client.render.LidarItemRenderer.CORPS,
				dev.pyrofetch.jn1.client.render.CameraStandRenderer.TÊTE));

		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenVendingPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().setScreen(
						new dev.pyrofetch.jn1.client.screen.VendingScreen(payload.pos()))));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenPanelPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().setScreen(
						new NoteScreen(payload.pos(), NoteScreen.Support.ÉCRAN))));
		// L'écran d'une compagne, au clic main vide.
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenCompanionPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().world != null && context.client().world.getEntityById(payload.entityId())
							instanceof dev.pyrofetch.jn1.entity.CompanionEntity elle) {
						context.client().setScreen(new dev.pyrofetch.jn1.client.screen.CompanionScreen(
								elle, payload.affection(), payload.salut()));
					}
				}));
		// Sa réponse à « Talk » : à l'écran ouvert pour elle.
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.TalkPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().currentScreen instanceof dev.pyrofetch.jn1.client.screen.TalkScreen écran
							&& écran.compagne().getId() == payload.entityId()) {
						écran.recevoir(payload);
					}
				}));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenNotebookPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().setScreen(
						new dev.pyrofetch.jn1.client.notebook.NotebookScreen())));
		// La question d'âge, au premier lancement avec le mod.
		dev.pyrofetch.jn1.client.agegate.AgeGate.register();
		// La Horny Potion : écran de choix, apparence des joueurs, bouton d'inventaire.
		dev.pyrofetch.jn1.client.morph.MorphClient.register();
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenScenesPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().setScreen(
						new dev.pyrofetch.jn1.client.screen.SceneScreen(payload))));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenSelectorPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().currentScreen instanceof dev.pyrofetch.jn1.client.screen.SelectorScreen écran
							&& écran.pos().equals(payload.pos())) {
						écran.mettreÀJour(payload);
					} else {
						context.client().setScreen(new dev.pyrofetch.jn1.client.screen.SelectorScreen(payload));
					}
				}));
		// Le lidar : sa touche de vue 3D, les échos reçus du serveur, le
		// battement de ses impulsions, et l'objet lui-même, dont l'écran se
		// dessine en direct.
		dev.pyrofetch.jn1.client.lab.LidarTracker.register();
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.LidarPayload.ID,
				(payload, context) -> context.client().execute(
						() -> dev.pyrofetch.jn1.client.lab.LidarTracker.recevoir(payload.échos())));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
				dev.pyrofetch.jn1.client.lab.LidarTracker::tick);
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
				dev.pyrofetch.jn1.registry.ModItems.LIDAR, new dev.pyrofetch.jn1.client.render.LidarItemRenderer());
		// La capsule, en vraie sphère ; vide, elle bâille (seul indice de son contenu).
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
				dev.pyrofetch.jn1.registry.ModItems.CAPSULE, new dev.pyrofetch.jn1.client.render.CapsuleItemRenderer());

		// Les sons qui durent : radio des terminaux, bourdon des failles,
		// moteur des cabines.
		dev.pyrofetch.jn1.terminal.Radio.setClientTicker(
				dev.pyrofetch.jn1.client.sound.ModSoundInstances::radio);
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_LOAD.register(
				(entité, monde) -> dev.pyrofetch.jn1.client.sound.ModSoundInstances.entitéChargée(entité));

		ParticleFactoryRegistry.getInstance().register(ModParticles.GLYPH_MOTE, EndRodParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.SILENCE, EndRodParticle.Factory::new);

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
