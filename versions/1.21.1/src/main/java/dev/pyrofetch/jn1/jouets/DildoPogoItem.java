package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.network.PogoPayload;
import dev.pyrofetch.jn1.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

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

	public DildoPogoItem(Settings settings) {
		super(settings);
	}

	/** Le pogo est dans l'une des deux mains. */
	public static boolean tenu(PlayerEntity joueur) {
		return joueur.getMainHandStack().isOf(ModItems.DILDO_POGO) || joueur.getOffHandStack().isOf(ModItems.DILDO_POGO);
	}

	/**
	 * Sur le ressort : tenu, pas accroupi, ni en vol, ni monté, ni à la nage.
	 * Ce sont les conditions du rebond ; la chute n'abîme alors personne.
	 */
	public static boolean actif(PlayerEntity joueur) {
		return tenu(joueur) && !joueur.isSneaking() && !joueur.getAbilities().flying && !joueur.hasVehicle()
				&& !joueur.isTouchingWater() && !joueur.isInLava() && !joueur.isFallFlying() && !joueur.isClimbing()
				&& !joueur.isSpectator();
	}

	/** Le ressort absorbe la chute : appelé par {@code LivingEntityMixin}. */
	public static boolean amortit(PlayerEntity joueur) {
		return actif(joueur);
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PogoPayload.ID,
				(payload, context) -> context.player().server.execute(() -> {
					var joueur = context.player();
					if (!tenu(joueur) || !Float.isFinite(payload.qualité())) {
						return;
					}
					float q = MathHelper.clamp(payload.qualité(), 0f, 1f);
					joueur.addExhaustion(FATIGUE * (1 + 2 * q));
					// Le bruit pour les autres : le client a déjà joué le sien.
					joueur.getWorld().playSound(joueur, joueur.getX(), joueur.getY(), joueur.getZ(),
							SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.PLAYERS, 0.35f + 0.4f * q,
							1.3f - 0.5f * q + joueur.getRandom().nextFloat() * 0.1f);
				}));
	}

	@Override
	public void appendTooltip(ItemStack pile, TooltipContext contexte, List<Text> lignes, TooltipType type) {
		lignes.add(Text.translatable("item.jn1.dildo_pogo.tooltip").formatted(Formatting.GRAY));
		lignes.add(Text.translatable("item.jn1.dildo_pogo.tooltip2").formatted(Formatting.GRAY));
	}
}
