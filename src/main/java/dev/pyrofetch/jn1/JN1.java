package dev.pyrofetch.jn1;

import dev.pyrofetch.jn1.command.ModCommands;
import dev.pyrofetch.jn1.config.JN1Config;
import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.dialogue.DialogueLoader;
import dev.pyrofetch.jn1.network.ModPackets;
import dev.pyrofetch.jn1.terminal.TerminalHandler;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItemGroups;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import dev.pyrofetch.jn1.registry.ModComponents;
import dev.pyrofetch.jn1.registry.ModItems;
import dev.pyrofetch.jn1.registry.ModParticles;
import dev.pyrofetch.jn1.registry.ModScreenHandlers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JN1 implements ModInitializer {
	public static final String MOD_ID = "jn1";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		JN1Config.get();

		ModAttachments.register();
		dev.pyrofetch.jn1.registry.ModSounds.register();
		dev.pyrofetch.jn1.sound.GirlSounds.register();
		ModBlocks.register();
		ModComponents.register();
		dev.pyrofetch.jn1.registry.ModArmorMaterials.register();
		ModItems.register();
		dev.pyrofetch.jn1.cum.CumCauldronBlock.comportements();
		dev.pyrofetch.jn1.cum.AuCumRecipe.register();
		// Le distributeur tire la flèche et lance la fiole, comme les leurs.
		net.minecraft.block.DispenserBlock.registerProjectileBehavior(dev.pyrofetch.jn1.registry.ModItems.CUM_ARROW);
		net.minecraft.block.DispenserBlock.registerProjectileBehavior(dev.pyrofetch.jn1.registry.ModItems.SPLASH_CUM);
		ModBlockEntities.register();
		ModParticles.register();
		dev.pyrofetch.jn1.worldgen.ModStructures.register();
		ModItemGroups.register();
		ModScreenHandlers.register();
		ModEntities.register();
		ModPackets.register();
		DialogueLoader.register();
		ModCommands.register();
		TerminalHandler.register();
		dev.pyrofetch.jn1.photo.PhotoHandler.register();
		dev.pyrofetch.jn1.block.NoteHandler.register();
		dev.pyrofetch.jn1.block.VendingMachineBlock.registerEditor();
		dev.pyrofetch.jn1.shape.Shapes.register();
		dev.pyrofetch.jn1.excitation.Excitation.register();
		dev.pyrofetch.jn1.gender.Gender.register();
		dev.pyrofetch.jn1.jouets.DildoPogoItem.register();
		dev.pyrofetch.jn1.onlyfaps.OnlyFaps.register();
		dev.pyrofetch.jn1.excitation.Reveil.register();
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> dev.pyrofetch.jn1.excitation.Excitation.oublier(handler.player.getUuid()));
		dev.pyrofetch.jn1.lab.LabDoorWatcher.register();
		JN1.LOGGER.debug("Laboratoire : dimension {} attendue.",
				dev.pyrofetch.jn1.lab.LabDimension.LAB.getValue());
		dev.pyrofetch.jn1.interaction.JennyActionHandler.register();
		// « six seven » dans le chat, à côté de Gaster : il danse.
		dev.pyrofetch.jn1.gaster.SixSeven.register();
		dev.pyrofetch.jn1.gaster.GasterShop.register();
		dev.pyrofetch.jn1.interaction.SceneHandler.register();
		dev.pyrofetch.jn1.morph.MorphHandler.register();
		dev.pyrofetch.jn1.lab.VoidFall.register();
		dev.pyrofetch.jn1.rift.Contamination.register();
		dev.pyrofetch.jn1.lab.VoidLabs.register();
		dev.pyrofetch.jn1.lift.LiftHandler.register();
		dev.pyrofetch.jn1.notebook.FieldNotebook.register();

		ServerTickEvents.END_SERVER_TICK.register(DialogueEngine::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			DialogueEngine.clear();
			dev.pyrofetch.jn1.interaction.Conversation.clear();
		});

		// À la connexion, le client doit connaître l'état du carnet.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				DialogueEngine.syncProgress(handler.getPlayer(), ModAttachments.get(handler.getPlayer())));

		// Toutes les recettes du mod au livre de recettes, dès l'arrivée
		// (0.56.7). Le jeu
		// ne montre une recette qu'une fois débloquée, d'ordinaire par un
		// progrès : le mod n'en avait pour aucune, rien n'y paraissait.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				handler.getPlayer().unlockRecipes(server.getRecipeManager().values().stream()
						.filter(r -> r.id().getNamespace().equals(MOD_ID) && !r.value().isIgnoredInRecipeBook())
						.toList()));

		LOGGER.info("[J.N-1] Le groupe témoin est en ligne.");
	}
}
