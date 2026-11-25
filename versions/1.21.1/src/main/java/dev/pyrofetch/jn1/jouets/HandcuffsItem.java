package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Les menottes. Tendues à une fille (par {@code CompanionEntity.interactMob},
 * sans quoi son menu s'ouvrirait) :
 * <ul>
 *   <li>libre, elle est menottée : mains jointes dans le dos, elle ne bouge
 *   plus d'elle-même ;</li>
 *   <li>menottée, accroupi : on prend sa chaîne en main — la laisse du jeu —,
 *   qu'on attache ensuite à une barrière comme n'importe quelle laisse ;</li>
 *   <li>menottée, debout : on la libère, chaîne comprise.</li>
 * </ul>
 */
public class HandcuffsItem extends Item {
	public HandcuffsItem(Settings settings) {
		super(settings);
	}

	/** Côté serveur. */
	public static void utiliserSur(CompanionEntity fille, ServerPlayerEntity joueur) {
		// Une fille sur un autre squelette n'a pas de pose menottée.
		if (!fille.isCuffed() && !fille.joueLesScènes()) {
			return;
		}
		if (!fille.isCuffed()) {
			fille.setCuffed(true);
			fille.getNavigation().stop();
			fille.getWorld().playSound(null, fille.getBlockPos(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.NEUTRAL, 1.0F, 1.2F);
			joueur.sendMessage(Text.translatable("message.jn1.handcuffs.on", fille.getDisplayName()), true);
			dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "menottes");
			dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "cuffed");
		} else if (joueur.isSneaking()) {
			if (fille.getLeashHolder() == joueur) {
				return;
			}
			fille.attacherChaîne(joueur);
			fille.getWorld().playSound(null, fille.getBlockPos(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
			joueur.sendMessage(Text.translatable("message.jn1.handcuffs.chain", fille.getDisplayName()), true);
		} else {
			fille.setCuffed(false);
			fille.getWorld().playSound(null, fille.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.NEUTRAL, 1.0F, 1.2F);
			joueur.sendMessage(Text.translatable("message.jn1.handcuffs.off", fille.getDisplayName()), true);
			dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "uncuffed");
		}
	}
}
