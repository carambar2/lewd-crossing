package dev.pyrofetch.jn1.excitation;

import dev.pyrofetch.jn1.cum.Cum;
import dev.pyrofetch.jn1.gender.Gender;
import dev.pyrofetch.jn1.registry.ModItems;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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
	public static TypedActionResult<ItemStack> commencer(World world, PlayerEntity joueur, Hand main, boolean pourFemme) {
		ItemStack pile = joueur.getStackInHand(main);
		// Le genre n'est pas synchronisé : le client croirait toute joueuse
		// homme, et passerait l'usage à l'autre main. Le serveur seul juge.
		if (!world.isClient() && Gender.femme(joueur) != pourFemme) {
			if (!world.isClient()) {
				joueur.sendMessage(Text.translatable("message.jn1.pleasure.not_for_you"), true);
			}
			return TypedActionResult.fail(pile);
		}
		joueur.setCurrentHand(main);
		return TypedActionResult.consume(pile);
	}

	/** Pas de geste du jeu : le client dessine le sien ({@code PleasureHandMixin}). */
	public static UseAction geste() {
		return UseAction.NONE;
	}

	/** À chaque tick du geste. */
	public static void pendant(World world, LivingEntity qui, ItemStack pile, int restant) {
		pendant(world, qui, pile, restant, DURÉE);
	}

	/** {@code durée} : en combien de ticks une barre vide se remplit avec cet objet. */
	public static void pendant(World world, LivingEntity qui, ItemStack pile, int restant, int durée) {
		if (world.isClient() || !(qui instanceof ServerPlayerEntity joueur)) {
			return;
		}
		int écoulé = USAGE_MAX - restant;
		if (écoulé % 10 == 0) {
			SoundEvent bruit = GirlSounds.bruit(écoulé % 20 == 0 ? "slide" : "smallinserts");
			if (bruit != null) {
				world.playSound(null, joueur.getX(), joueur.getY() + 1, joueur.getZ(), bruit,
						SoundCategory.PLAYERS, 0.5F, 1.0F + world.getRandom().nextFloat() * 0.1F);
			}
		}
		if (Excitation.ajouter(joueur, Excitation.MAX / durée)) {
			jouir(joueur);
			joueur.stopUsingItem();
		}
	}

	/** La fin : la barre retombe, et ce qui jaillit va au flacon ou au sol. */
	public static void jouir(ServerPlayerEntity joueur) {
		Excitation.set(joueur, 0);
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "fait_main");
		Excitation.orgasme(joueur);
		ServerWorld monde = joueur.getServerWorld();
		SoundEvent bruit = GirlSounds.bruit("cuminflation");
		if (bruit != null) {
			monde.playSound(null, joueur.getX(), joueur.getY() + 1, joueur.getZ(), bruit, SoundCategory.PLAYERS, 0.8F, 1.0F);
		}
		Direction devant = joueur.getHorizontalFacing();
		monde.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(ModItems.DRIED_CUM)),
				joueur.getX() + devant.getOffsetX() * 0.5, joueur.getY() + 0.8, joueur.getZ() + devant.getOffsetZ() * 0.5,
				12, 0.15, 0.1, 0.15, 0.08);
		Hand autre = joueur.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
		ItemStack flacon = joueur.getStackInHand(autre);
		if (flacon.isOf(Items.GLASS_BOTTLE)) {
			flacon.decrementUnlessCreative(1, joueur);
			ItemStack fiole = new ItemStack(ModItems.CUM);
			if (flacon.isEmpty()) {
				joueur.setStackInHand(autre, fiole);
			} else if (!joueur.getInventory().insertStack(fiole)) {
				joueur.dropItem(fiole, false);
			}
			return;
		}
		BlockPos là = joueur.getBlockPos().offset(devant);
		if (!Cum.poserTrace(monde, là, Direction.DOWN)) {
			Cum.poserTrace(monde, là.down(), Direction.DOWN);
		}
	}
}
