package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.network.LidarPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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

	public LidarItem(Properties settings) {
		super(settings);
	}

	/** La touche de la vue 3D, telle que le joueur l'a réglée : le client la résout lui-même. */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
	                            java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		tooltip.accept(Component.translatable("item.jn1.lidar.tooltip_3d", Component.keybind("key.jn1.lidar_3d"))
				.withStyle(ChatFormatting.GRAY));
	}

	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel world, Entity entity,
	                          @org.jetbrains.annotations.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
		// 26.3 : appelé côté serveur seulement.
		if (!(entity instanceof ServerPlayer joueur) || joueur.tickCount % PÉRIODE != 0) {
			return;
		}
		// Seulement tenu : dans le sac, il se tait.
		if (joueur.getMainHandItem() != stack && joueur.getOffhandItem() != stack) {
			return;
		}
		// Hors du vide, il n'entend rien : l'écran le dit (LidarItemRenderer).
		if (!VoidSwim.dansLeVide(joueur)) {
			return;
		}
		List<BlockPos> portes = VoidLabs.repérer(joueur.level().getServer(), joueur.blockPosition(), PORTÉE).stream()
				.limit(ÉCHOS_AU_PLUS)
				.map(VoidLabs::porteDuVide)
				.toList();
		ServerPlayNetworking.send(joueur, new LidarPayload(portes));
	}
}
