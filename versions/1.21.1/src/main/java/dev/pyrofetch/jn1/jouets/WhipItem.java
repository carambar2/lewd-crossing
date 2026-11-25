package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.excitation.Excitation;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Le fouet. Sur un monstre, une arme légère (4 de dégâts, lente) qui claque.
 * Sur une fille, il ne blesse pas ({@code CompanionEntity.damage}) : elle
 * sursaute, gémit, rougit, et celui qui tient le fouet s'échauffe un peu.
 */
public class WhipItem extends Item {
	public WhipItem(Settings settings) {
		super(settings.attributeModifiers(AttributeModifiersComponent.builder()
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID,
						3.0, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
				.add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID,
						-2.6, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
				.build()));
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity cible, LivingEntity attaquant) {
		claquer(cible);
		return true;
	}

	static void claquer(LivingEntity cible) {
		SoundEvent claque = GirlSounds.bruit("slap");
		cible.getWorld().playSound(null, cible.getX(), cible.getY() + 1, cible.getZ(),
				claque != null ? claque : SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.1F);
	}

	/** Un coup de fouet sur une fille : rien de blessant. Côté serveur. */
	public static void fouetter(CompanionEntity fille, ServerPlayerEntity joueur) {
		claquer(fille);
		String[] cris = {"ahh", "moan", "huh"};
		GirlSounds.crier(fille, cris[fille.getRandom().nextInt(cris.length)], 1.0F);
		fille.setDelightedFor(40);
		fille.showHearts(2);
		fille.getLookControl().lookAt(joueur);
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
