package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.network.LidarPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Le lidar : un traqueur qu'on tient à la main, et qui montre sur son propre
 * écran où sont les structures du vide.
 *
 * <p>Pour l'instant, ce sont les laboratoires, un sous chaque maison de la
 * surface (voir {@link VoidLabs}). Il les trouve même là où rien n'est encore
 * bâti, puisqu'il les déduit des maisons, et même sous des régions que
 * personne n'a vues.
 *
 * <p>Il n'y a rien à faire pour s'en servir : tant qu'on le tient dans le vide,
 * le serveur lui envoie les échos toutes les deux secondes, et l'écran les
 * place en direct selon le regard (voir {@code LidarItemRenderer}). À la
 * surface, il n'y a rien pour lui répondre.
 */
public class LidarItem extends Item {
	/** Jusqu'où porte l'écho, en blocs : deux régions de maisons dans chaque direction. */
	public static final int PORTÉE = 1024;
	/** Au-delà, l'écran ne serait plus qu'une nuée de points. */
	private static final int ÉCHOS_AU_PLUS = 12;
	/** Tous les combien le serveur renvoie les échos, en ticks. */
	private static final int PÉRIODE = 40;

	public LidarItem(Settings settings) {
		super(settings);
	}

	/** La touche de la vue 3D, telle que le joueur l'a réglée : le client la résout lui-même. */
	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.jn1.lidar.tooltip_3d", Text.keybind("key.jn1.lidar_3d"))
				.formatted(Formatting.GRAY));
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (world.isClient() || !(entity instanceof ServerPlayerEntity joueur) || joueur.age % PÉRIODE != 0) {
			return;
		}
		// Seulement tenu : dans le sac, il se tait.
		if (joueur.getMainHandStack() != stack && joueur.getOffHandStack() != stack) {
			return;
		}
		// Hors du vide, il n'entend rien : l'écran le dit (LidarItemRenderer).
		if (!VoidSwim.dansLeVide(joueur)) {
			return;
		}
		List<BlockPos> portes = VoidLabs.repérer(joueur.getServer(), joueur.getBlockPos(), PORTÉE).stream()
				.limit(ÉCHOS_AU_PLUS)
				.map(VoidLabs::porteDuVide)
				.toList();
		ServerPlayNetworking.send(joueur, new LidarPayload(portes));
	}
}
