package dev.pyrofetch.jn1.terminal;

import dev.pyrofetch.jn1.block.VendingMachineBlock;
import dev.pyrofetch.jn1.block.VendingMachineBlockEntity;
import dev.pyrofetch.jn1.shop.VendingOffer;
import dev.pyrofetch.jn1.registry.ModSounds;

import dev.pyrofetch.jn1.block.ComputerBlock;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.network.ShopPurchasePayload;
import dev.pyrofetch.jn1.network.TerminalActionPayload;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.lab.StasisPodBlockEntity;
import dev.pyrofetch.jn1.shop.GirlEntry;
import dev.pyrofetch.jn1.shop.ShopEntry;
import dev.pyrofetch.jn1.registry.ModEntities;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Ce que le serveur fait des demandes venues d'un terminal.
 *
 * <p>Le client pilote l'affichage, mais rien ne se produit dans le monde sans
 * passer par ici : la portée est revérifiée à chaque demande, sinon n'importe
 * quel client pourrait faire apparaître ce qu'il veut où il veut.
 */
public final class TerminalHandler {
	/** Au-delà, le joueur n'est plus devant l'écran. */
	private static final double MAX_RANGE_SQUARED = 36.0;

	private TerminalHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(TerminalActionPayload.ID,
				(payload, context) -> context.server().execute(
						() -> handle(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(ShopPurchasePayload.ID,
				(payload, context) -> context.server().execute(
						() -> purchase(context.player(), payload)));
	}

	/**
	 * Un achat n'est jamais accordé sur parole : le catalogue est relu côté
	 * serveur, le prix aussi, et la monnaie réellement retirée de l'inventaire.
	 */
	/** Une compagne se paie une fois, et reste acquise. */
	private static void purchaseGirl(ServerPlayer player, GirlEntry girl) {
		if (ModAttachments.get(player).hasFlag(girl.unlockFlag())) {
			return;
		}
		if (countCurrency(player) < girl.price()) {
			player.sendOverlayMessage(Component.translatable("shop.jn1.too_poor"));
			return;
		}
		takeCurrency(player, girl.price());
		DialogueEngine.setFlag(player, girl.unlockFlag());
		// Elle est déposée dans un tube libre : l'achat se voit dans la pièce,
		// et le terminal aura de quoi la relâcher.
		StasisPodBlockEntity libre = findPod(player.level(), player.blockPosition(), girl.id());
		if (libre != null && libre.isEmpty()) {
			libre.setOccupant(girl.id());
			player.level().playSound(null, libre.getBlockPos(),
					SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.4F);
		}
		player.sendOverlayMessage(Component.translatable("shop.jn1.unlocked",
				Component.translatable(girl.translationKey())));
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.8F);
	}

	private static void purchase(ServerPlayer player, ShopPurchasePayload payload) {
		if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > MAX_RANGE_SQUARED) {
			return;
		}
		// Le guichet : un terminal ou un distributeur, rien d'autre. Sans ce
		// contrôle, n'importe quel bloc à portée ferait office de boutique.
		BlockState guichet = player.level().getBlockState(payload.pos());
		ShopEntry.Guichet sorte;
		if (guichet.getBlock() instanceof ComputerBlock) {
			sorte = ShopEntry.Guichet.TERMINAL;
		} else if (guichet.getBlock() instanceof VendingMachineBlock) {
			sorte = ShopEntry.Guichet.DISTRIBUTEUR;
		} else {
			return;
		}
		// Les compagnes ont leur propre catalogue, mais empruntent le même
		// guichet : mêmes vérifications de portée, de prix et de monnaie. Le
		// distributeur ne les vend pas.
		GirlEntry girl = GirlEntry.byId(payload.entryId());
		if (girl != null && sorte == ShopEntry.Guichet.TERMINAL) {
			purchaseGirl(player, girl);
			return;
		}

		boolean distributeur = sorte == ShopEntry.Guichet.DISTRIBUTEUR;
		// Un distributeur garni à la main ne vend que ses rayons : le catalogue
		// n'y a plus cours, et l'offre est relue dans la machine elle-même — sa
		// moitié basse, même si la demande vise le haut.
		if (distributeur && player.level().getBlockEntity(VendingMachineBlock.bas(payload.pos(), guichet))
				instanceof VendingMachineBlockEntity machine && machine.garnie()) {
			VendingOffer offre = machine.offre(payload.entryId());
			if (offre != null) {
				vendre(player, payload.pos(), offre.stack(), offre.price(), offre.libellé(), true);
			}
			return;
		}

		ShopEntry entry = ShopEntry.byId(payload.entryId());
		if (entry == null || !entry.proposé(sorte, player.level())) {
			return;
		}

		PlayerProgress progress = ModAttachments.get(player);
		if (entry.isUnlock() && progress.hasFlag(entry.unlockFlag())) {
			return;
		}
		if (!entry.isUnlock()) {
			vendre(player, payload.pos(), entry.stack(), entry.price(),
					Component.translatable(entry.translationKey()), distributeur);
			return;
		}
		if (countCurrency(player) < entry.price()) {
			player.sendOverlayMessage(Component.translatable("shop.jn1.too_poor"));
			return;
		}
		takeCurrency(player, entry.price());
		DialogueEngine.setFlag(player, entry.unlockFlag());
		player.sendOverlayMessage(Component.translatable("shop.jn1.unlocked",
				Component.translatable(entry.translationKey())));
		player.level().playSound(null, payload.pos(),
				SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.8F);
	}

	/** Encaisse, remet une copie de la pile, et le fait entendre au guichet. */
	private static void vendre(ServerPlayer player, BlockPos guichet, ItemStack pile, int prix,
	                           Component libellé, boolean distributeur) {
		if (countCurrency(player) < prix) {
			player.sendOverlayMessage(Component.translatable("shop.jn1.too_poor"));
			if (distributeur) {
				player.level().playSound(null, guichet, ModSounds.VENDING_DENY,
						SoundSource.BLOCKS, 0.7F, 1.0F);
			}
			return;
		}
		takeCurrency(player, prix);
		ItemStack bought = pile.copy();
		if (!player.getInventory().add(bought)) {
			player.drop(bought, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
		player.sendOverlayMessage(Component.translatable("shop.jn1.bought", libellé));
		if (distributeur) {
			player.level().playSound(null, guichet, ModSounds.VENDING_BUY,
					SoundSource.BLOCKS, 0.8F, 1.0F);
		} else {
			player.level().playSound(null, guichet,
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.8F);
		}
	}

	private static int countCurrency(ServerPlayer player) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ShopEntry.CURRENCY)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void takeCurrency(ServerPlayer player, int amount) {
		int left = amount;
		for (int slot = 0; slot < player.getInventory().getContainerSize() && left > 0; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.is(ShopEntry.CURRENCY)) {
				continue;
			}
			int taken = Math.min(left, stack.getCount());
			stack.shrink(taken);
			left -= taken;
		}
	}

	private static void handle(ServerPlayer player, TerminalActionPayload payload) {
		BlockPos pos = payload.pos();
		ServerLevel world = player.level();
		BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof ComputerBlock)) {
			return;
		}
		if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_RANGE_SQUARED) {
			// Trop loin pour agir, mais la fermeture vaut : sans elle, les mains
			// restaient pour toujours sur le clavier.
			if (payload.action() == TerminalActionPayload.ACTION_CLOSE) {
				ComputerBlock.release(world, pos);
			}
			return;
		}
		if (payload.action() == TerminalActionPayload.ACTION_RADIO) {
			// On reste devant l'écran : la session continue, pas de relâche.
			régler(world, pos, state, payload.target());
			return;
		}
		if (payload.action() == TerminalActionPayload.ACTION_SPAWN) {
			GirlEntry girl = GirlEntry.byId(payload.target());
			// Elle doit avoir été acquise au présentoir.
			if (girl == null) {
				return;
			}
			if (!ModAttachments.get(player).hasFlag(girl.unlockFlag())) {
				player.sendOverlayMessage(Component.translatable("shop.jn1.locked"));
			} else {
				spawnCompanion(world, pos, state, player, girl);
			}
		}
		ComputerBlock.release(world, pos);
	}

	/** Change la station ; un numéro hors de la liste est ignoré. */
	private static void régler(ServerLevel world, BlockPos pos, BlockState state, String cible) {
		int station;
		try {
			station = Integer.parseInt(cible);
		} catch (NumberFormatException exception) {
			return;
		}
		if (station < 0 || station > Radio.STATIONS.size() || station == state.getValue(ComputerBlock.STATION)) {
			return;
		}
		world.setBlock(pos, state.setValue(ComputerBlock.STATION, station), net.minecraft.world.level.block.Block.UPDATE_ALL);
		world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.8F);
	}

	/** Rayon de recherche d'un tube autour du terminal. */
	private static final int POD_RANGE = 12;

	/**
	 * Le tube d'où la compagne va sortir.
	 *
	 * <p>On préfère celui qui la garde déjà ; à défaut, n'importe quel tube
	 * libre fera l'affaire. Sans tube du tout, l'écran n'a rien à commander et
	 * le spawn est refusé : c'est ce qui donne un rôle à ces vitrines, au lieu
	 * de les laisser en décor.
	 */
	private static StasisPodBlockEntity findPod(ServerLevel world, BlockPos terminal, String girl) {
		StasisPodBlockEntity libre = null;
		for (BlockPos pos : BlockPos.betweenClosed(terminal.offset(-POD_RANGE, -4, -POD_RANGE),
				terminal.offset(POD_RANGE, 4, POD_RANGE))) {
			if (!(world.getBlockEntity(pos) instanceof StasisPodBlockEntity pod)) {
				continue;
			}
			if (girl.equals(pod.occupant())) {
				return pod;
			}
			if (libre == null && pod.isEmpty()) {
				libre = pod;
			}
		}
		return libre;
	}

	private static void spawnCompanion(ServerLevel world, BlockPos pos, BlockState state,
	                                   ServerPlayer player, GirlEntry girl) {
		// Le tube n'est plus exigé. Il reste la mise en scène quand il y en a un
		// à portée — elle en sort, le vitrage s'éteint — mais son absence
		// n'empêche plus la machine de rendre la compagne.
		StasisPodBlockEntity pod = findPod(world, pos, girl.id());

		// Elle arrive devant l'écran, tournée vers la pièce, à une place où
		// personne ne se tient déjà (0.56.7 : Jenny apparaissait dans FapGPT,
		// qui attendait là — vu en jeu).
		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		BlockPos target = placeLibre(world, pos, facing, girl);

		// La sienne est déjà là (dans un chunk chargé) : elle vient, au lieu
		// d'en faire une seconde — ce qui doublait sous-vêtements et photos.
		for (var déjà : world.getEntities(girl.type(),
				e -> e instanceof CompanionEntity c && player.getUUID().equals(c.getCompanionUuid()))) {
			if (déjà instanceof CompanionEntity elle && !elle.hasScene()) {
				elle.stopRiding();
				elle.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
						facing.getOpposite().toYRot(), 0.0F);
				elle.getNavigation().stop();
				elle.setMode(JennyMode.FOLLOW);
				world.playSound(null, target, SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.5F, 1.6F);
				dev.pyrofetch.jn1.interaction.Conversation.événement(player, elle, "arrival");
				return;
			}
		}

		LivingEntity spawned = girl.type().spawn(world, target, EntitySpawnReason.TRIGGERED);
		if (!(spawned instanceof CompanionEntity jenny)) {
			return;
		}
		jenny.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
				facing.getOpposite().toYRot(), 0.0F);
		jenny.setCompanion(player);
		jenny.setMode(JennyMode.FOLLOW);

		// Le tube se vide en la relâchant, et son vitrage s'éteint. S'il n'y en
		// avait pas, elle paraît simplement devant l'écran.
		if (pod != null) {
			pod.setOccupant(null);
			world.playSound(null, pod.getBlockPos(), SoundEvents.BEACON_DEACTIVATE,
					SoundSource.BLOCKS, 0.5F, 1.7F);
			world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
					pod.getBlockPos().getX() + 0.5, pod.getBlockPos().getY() + 0.5, pod.getBlockPos().getZ() + 0.5,
					18, 0.25, 0.4, 0.25, 0.03);
		}

		world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE,
				SoundSource.BLOCKS, 0.6F, 1.4F);
		// Elle dit bonjour en arrivant : elle paraissait sans un mot.
		dev.pyrofetch.jn1.interaction.Conversation.événement(player, jenny, "arrival");
	}

	/**
	 * Devant l'écran, à deux blocs, puis à côté, puis plus loin : la première
	 * place où elle tient debout sans toucher une autre créature (le joueur assis,
	 * FapGPT qui attend). Faute de mieux, juste devant l'écran.
	 */
	private static BlockPos placeLibre(ServerLevel world, BlockPos pos, Direction facing, GirlEntry girl) {
		Direction côté = facing.getClockWise();
		int[][] essais = {{2, 0}, {2, 1}, {2, -1}, {3, 0}, {3, 1}, {3, -1}, {1, 1}, {1, -1}, {1, 0}};
		for (int[] e : essais) {
			BlockPos ici = pos.relative(facing, e[0]).relative(côté, e[1]);
			net.minecraft.world.phys.AABB boîte = girl.type().getDimensions()
					.makeBoundingBox(ici.getX() + 0.5, ici.getY(), ici.getZ() + 0.5);
			if (world.noCollision(boîte) && world.getEntities((net.minecraft.world.entity.Entity) null, boîte.inflate(0.2),
					en -> en instanceof LivingEntity).isEmpty()) {
				return ici;
			}
		}
		return pos.relative(facing);
	}
}
