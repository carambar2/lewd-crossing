package dev.pyrofetch.jn1.excitation;

import dev.pyrofetch.jn1.cum.Cum;
import dev.pyrofetch.jn1.gender.Gender;
import dev.pyrofetch.jn1.registry.ModItems;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Se faire plaisir avec un objet : le masturbateur pour un homme, le dildo pour
 * une femme. On le tient (clic droit maintenu ; le va-et-vient à la première
 * personne est dessiné par le client, {@code PleasureHandMixin}) : l'excitation monte, pleine en {@link #DURÉE} ticks. Au
 * bout, on jouit : un flacon vide tenu dans l'autre main se remplit d'une
 * fiole de cum, sinon une trace tombe devant soi.
 *
 * <p>Partagé par les deux objets, qui ne diffèrent que par qui peut s'en
 * servir.
 */
public final class Plaisir {
	/** Quinze secondes, depuis une barre vide. */
	public static final int DURÉE = 300;
	/** Assez long pour ne jamais s'arrêter de lui-même avant la fin. */
	public static final int USAGE_MAX = 72000;

	private Plaisir() {
	}

	/** Le début du geste : refusé si l'objet n'est pas pour ce genre. */
	public static InteractionResult commencer(Level world, Player joueur, InteractionHand main, boolean pourFemme) {
		ItemStack pile = joueur.getItemInHand(main);
		// Le genre n'est pas synchronisé : le client croirait toute joueuse
		// homme, et passerait l'usage à l'autre main. Le serveur seul juge.
		if (!world.isClientSide() && Gender.femme(joueur) != pourFemme) {
			if (!world.isClientSide()) {
				joueur.sendOverlayMessage(Component.translatable("message.jn1.pleasure.not_for_you"));
			}
			return InteractionResult.FAIL;
		}
		joueur.startUsingItem(main);
		return InteractionResult.CONSUME;
	}

	/** Pas de geste du jeu : le client dessine le sien ({@code PleasureHandMixin}). */
	public static ItemUseAnimation geste() {
		return ItemUseAnimation.NONE;
	}

	/** À chaque tick du geste. */
	public static void pendant(Level world, LivingEntity qui, ItemStack pile, int restant) {
		pendant(world, qui, pile, restant, DURÉE);
	}

	/** {@code durée} : en combien de ticks une barre vide se remplit avec cet objet. */
	public static void pendant(Level world, LivingEntity qui, ItemStack pile, int restant, int durée) {
		if (world.isClientSide() || !(qui instanceof ServerPlayer joueur)) {
			return;
		}
		int écoulé = USAGE_MAX - restant;
		if (écoulé % 10 == 0) {
			SoundEvent bruit = GirlSounds.bruit(écoulé % 20 == 0 ? "slide" : "smallinserts");
			if (bruit != null) {
				world.playSound(null, joueur.getX(), joueur.getY() + 1, joueur.getZ(), bruit,
						SoundSource.PLAYERS, 0.5F, 1.0F + world.getRandom().nextFloat() * 0.1F);
			}
		}
		if (Excitation.ajouter(joueur, Excitation.MAX / durée)) {
			jouir(joueur);
			joueur.releaseUsingItem();
		}
	}

	/** La fin : la barre retombe, et ce qui jaillit va au flacon ou au sol. */
	public static void jouir(ServerPlayer joueur) {
		Excitation.set(joueur, 0);
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "fait_main");
		Excitation.orgasme(joueur);
		ServerLevel monde = joueur.level();
		SoundEvent bruit = GirlSounds.bruit("cuminflation");
		if (bruit != null) {
			monde.playSound(null, joueur.getX(), joueur.getY() + 1, joueur.getZ(), bruit, SoundSource.PLAYERS, 0.8F, 1.0F);
		}
		Direction devant = joueur.getDirection();
		monde.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.DRIED_CUM),
				joueur.getX() + devant.getStepX() * 0.5, joueur.getY() + 0.8, joueur.getZ() + devant.getStepZ() * 0.5,
				12, 0.15, 0.1, 0.15, 0.08);
		InteractionHand autre = joueur.getUsedItemHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack flacon = joueur.getItemInHand(autre);
		if (flacon.is(Items.GLASS_BOTTLE)) {
			flacon.consume(1, joueur);
			ItemStack fiole = new ItemStack(ModItems.CUM);
			if (flacon.isEmpty()) {
				joueur.setItemInHand(autre, fiole);
			} else if (!joueur.getInventory().add(fiole)) {
				joueur.drop(fiole, false, net.minecraft.util.Prediction.PREDICTED);
			}
			return;
		}
		BlockPos là = joueur.blockPosition().relative(devant);
		if (!Cum.poserTrace(monde, là, Direction.DOWN)) {
			Cum.poserTrace(monde, là.below(), Direction.DOWN);
		}
	}
}
