package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.network.PogoPayload;
import dev.pyrofetch.jn1.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/**
 * Le dildo pogo : un bâton sauteur dont le pied est un dildo. Tenu en main,
 * on rebondit à chaque atterrissage et on se dirige en l'air ; c'est un moyen
 * de transport, pas un jouet.
 *
 * <p>Le mouvement est fait par le client ({@code PogoClient}), comme tout
 * déplacement du joueur dans le jeu : le serveur ne fait qu'entendre le
 * rebond (le bruit pour les autres, un peu de fatigue) et retirer les dégâts
 * de chute tant qu'on est sur le ressort ({@link #amortit}).
 */
public class DildoPogoItem extends Item {
	/** Fatigue d'un rebond : celle d'un saut du jeu, trois fois pour un parfait. */
	private static final float FATIGUE = 0.05f;

	public DildoPogoItem(Properties settings) {
		super(settings);
	}

	/** Le pogo est dans l'une des deux mains. */
	public static boolean tenu(Player joueur) {
		return joueur.getMainHandItem().is(ModItems.DILDO_POGO) || joueur.getOffhandItem().is(ModItems.DILDO_POGO);
	}

	/**
	 * Sur le ressort : tenu, pas accroupi, ni en vol, ni monté, ni à la nage.
	 * Ce sont les conditions du rebond ; la chute n'abîme alors personne.
	 */
	public static boolean actif(Player joueur) {
		return tenu(joueur) && !joueur.isShiftKeyDown() && !joueur.getAbilities().flying && !joueur.isPassenger()
				&& !joueur.isInWater() && !joueur.isInLava() && !joueur.isFallFlying() && !joueur.onClimbable()
				&& !joueur.isSpectator();
	}

	/** Le ressort absorbe la chute : appelé par {@code LivingEntityMixin}. */
	public static boolean amortit(Player joueur) {
		return actif(joueur);
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PogoPayload.ID,
				(payload, context) -> context.server().execute(() -> {
					var joueur = context.player();
					if (!tenu(joueur) || !Float.isFinite(payload.qualité())) {
						return;
					}
					float q = Mth.clamp(payload.qualité(), 0f, 1f);
					joueur.causeFoodExhaustion(FATIGUE * (1 + 2 * q));
					// Le bruit pour les autres : le client a déjà joué le sien.
					joueur.level().playSound(joueur, joueur.getX(), joueur.getY(), joueur.getZ(),
							SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.35f + 0.4f * q,
							1.3f - 0.5f * q + joueur.getRandom().nextFloat() * 0.1f);
				}));
	}

	@Override
	public void appendHoverText(ItemStack pile, TooltipContext contexte, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> lignes, TooltipFlag type) {
		lignes.accept(Component.translatable("item.jn1.dildo_pogo.tooltip").withStyle(ChatFormatting.GRAY));
		lignes.accept(Component.translatable("item.jn1.dildo_pogo.tooltip2").withStyle(ChatFormatting.GRAY));
	}
}
