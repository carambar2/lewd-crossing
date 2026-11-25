package dev.pyrofetch.jn1.brewing;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Les potions du mod se brassent (0.83.9).
 *
 * <p>L'alambic du jeu ne passe que d'une potion du jeu à une autre ; les
 * nôtres sont des objets à part. Trois mixins le branchent sur cette table :
 * il reconnaît nos ingrédients, connaît nos recettes, et accepte nos potions
 * dans ses emplacements à fioles, pour qu'elles servent de base à la suite.
 *
 * <p>La logique est celle du jeu : la potion étrange pour base, l'œil
 * d'araignée fermenté qui inverse.
 *
 * <p>JEI ne les voit pas de lui-même (sous Fabric, il ne cherche qu'à partir
 * des mélanges du jeu) : {@code client.compat.JeiCompat} les lui déclare.
 */
public final class Brassage {
	/**
	 * Une recette : ce qui est dans la fiole, l'ingrédient, ce qui en sort ;
	 * {@code montrée} est la base telle qu'on la montre (JEI).
	 */
	public record Recette(Predicate<ItemStack> base, Supplier<ItemStack> montrée, Item ingrédient, Item résultat) {
		Recette(Item base, Item ingrédient, Item résultat) {
			this(stack -> stack.isOf(base), () -> new ItemStack(base), ingrédient, résultat);
		}
	}

	private static List<Recette> recettes;

	private Brassage() {
	}

	/** Une potion étrange : une fiole du jeu dont le contenu est « awkward ». */
	private static boolean étrange(ItemStack stack) {
		if (!stack.isOf(Items.POTION)) {
			return false;
		}
		PotionContentsComponent contenu = stack.get(DataComponentTypes.POTION_CONTENTS);
		return contenu != null && contenu.matches(Potions.AWKWARD);
	}

	public static List<Recette> recettes() {
		if (recettes == null) {
			recettes = List.of(
					new Recette(Brassage::étrange, () -> PotionContentsComponent.createStack(Items.POTION, Potions.AWKWARD),
							Items.POPPY, ModItems.HORNY_POTION),
					new Recette(ModItems.HORNY_POTION, Items.FERMENTED_SPIDER_EYE, ModItems.GENDER_POTION),
					new Recette(ModItems.HORNY_POTION, Items.SLIME_BALL, ModItems.BOOBS_UP_POTION),
					new Recette(ModItems.HORNY_POTION, Items.MELON_SLICE, ModItems.BOOTY_UP_POTION),
					new Recette(ModItems.HORNY_POTION, Items.BONE_MEAL, ModItems.SIZE_UP_POTION),
					new Recette(ModItems.BOOBS_UP_POTION, Items.FERMENTED_SPIDER_EYE, ModItems.BOOBS_DOWN_POTION),
					new Recette(ModItems.BOOTY_UP_POTION, Items.FERMENTED_SPIDER_EYE, ModItems.BOOTY_DOWN_POTION),
					new Recette(ModItems.SIZE_UP_POTION, Items.FERMENTED_SPIDER_EYE, ModItems.SIZE_DOWN_POTION));
		}
		return recettes;
	}

	@Nullable
	public static Recette trouver(ItemStack base, ItemStack ingrédient) {
		if (base.isEmpty() || ingrédient.isEmpty()) {
			return null;
		}
		for (Recette r : recettes()) {
			if (ingrédient.isOf(r.ingrédient()) && r.base().test(base)) {
				return r;
			}
		}
		return null;
	}

	public static boolean estIngrédient(ItemStack stack) {
		return recettes().stream().anyMatch(r -> stack.isOf(r.ingrédient()));
	}

	/** Une de nos potions : elle peut tenir dans un emplacement à fiole. */
	public static boolean estPotion(ItemStack stack) {
		return recettes().stream().anyMatch(r -> stack.isOf(r.résultat()));
	}
}
