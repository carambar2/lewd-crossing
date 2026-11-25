package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.excitation.Excitation;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Le fouet. Sur un monstre, une arme légère (4 de dégâts, lente) qui claque.
 * Sur une fille, il ne blesse pas ({@code CompanionEntity.damage}) : elle
 * sursaute, gémit, rougit, et celui qui tient le fouet s'échauffe un peu.
 */
public class WhipItem extends Item {
	public WhipItem(Properties settings) {
		super(settings.attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
						3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
						-2.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build()));
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity cible, LivingEntity attaquant) {
		claquer(cible);
	}

	static void claquer(LivingEntity cible) {
		SoundEvent claque = GirlSounds.bruit("slap");
		cible.level().playSound(null, cible.getX(), cible.getY() + 1, cible.getZ(),
				claque != null ? claque : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.1F);
	}

	/** Un coup de fouet sur une fille : rien de blessant. Côté serveur. */
	public static void fouetter(CompanionEntity fille, ServerPlayer joueur) {
		claquer(fille);
		String[] cris = {"ahh", "moan", "huh"};
		GirlSounds.crier(fille, cris[fille.getRandom().nextInt(cris.length)], 1.0F);
		fille.setDelightedFor(40);
		fille.showHearts(2);
		fille.getLookControl().setLookAt(joueur);
		// Le fouet échauffe, il ne fait pas jouir : la barre plafonne juste sous le
		// plein, sinon la scène suivante passait à sa fin dès son premier tick.
		Excitation.ajouter(joueur, 6.0F);
		if (Excitation.get(joueur) > Excitation.MAX - 1) {
			Excitation.set(joueur, Excitation.MAX - 1);
		}
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "fouet");
		dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "whip");
	}
}
