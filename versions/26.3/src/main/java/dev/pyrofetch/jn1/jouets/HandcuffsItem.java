package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;

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
	public HandcuffsItem(Properties settings) {
		super(settings);
	}

	/** Côté serveur. */
	public static void utiliserSur(CompanionEntity fille, ServerPlayer joueur) {
		// Une fille sur un autre squelette n'a pas de pose menottée.
		if (!fille.isCuffed() && !fille.joueLesScènes()) {
			return;
		}
		if (!fille.isCuffed()) {
			fille.setCuffed(true);
			fille.getNavigation().stop();
			fille.level().playSound(null, fille.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 1.2F);
			joueur.sendOverlayMessage(Component.translatable("message.jn1.handcuffs.on", fille.getDisplayName()));
			dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "menottes");
			dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "cuffed");
		} else if (joueur.isShiftKeyDown()) {
			if (fille.getLeashHolder() == joueur) {
				return;
			}
			fille.attacherChaîne(joueur);
			fille.level().playSound(null, fille.blockPosition(), SoundEvents.LEAD_TIED /* 26.3 : le son du nœud de laisse */, SoundSource.NEUTRAL, 1.0F, 1.0F);
			joueur.sendOverlayMessage(Component.translatable("message.jn1.handcuffs.chain", fille.getDisplayName()));
		} else {
			fille.setCuffed(false);
			fille.level().playSound(null, fille.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
			joueur.sendOverlayMessage(Component.translatable("message.jn1.handcuffs.off", fille.getDisplayName()));
			dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "uncuffed");
		}
	}
}
