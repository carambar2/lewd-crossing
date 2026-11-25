package dev.pyrofetch.jn1.brewing;

import dev.pyrofetch.jn1.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Les potions du mod se brassent (0.83.9).
 *
 * <p>En 1.21.1, l'alambic du jeu ne passait que d'une potion du jeu à une
 * autre, et trois mixins le branchaient sur cette table. En 26.3, l'alambic
 * lit des recettes en données ({@code minecraft:brewing}) : nos huit recettes
 * sont dans {@code data/jn1/recipe/brewing/}, et c'est par elles qu'il
 * reconnaît nos ingrédients, brasse, et accepte nos potions dans ses
 * emplacements à fioles (l'ensemble {@code BREWING_INPUTS} des recettes). Les
 * mixins sont retirés ; cette table doit rester identique aux fichiers.
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
			this(stack -> stack.is(base), () -> new ItemStack(base), ingrédient, résultat);
		}
	}

	private static List<Recette> recettes;

	private Brassage() {
	}

	/** Une potion étrange : une fiole du jeu dont le contenu est « awkward ». */
	private static boolean étrange(ItemStack stack) {
		if (!stack.is(Items.POTION)) {
			return false;
		}
		PotionContents contenu = stack.get(DataComponents.POTION_CONTENTS);
		return contenu != null && contenu.is(Potions.AWKWARD);
	}

	public static List<Recette> recettes() {
		if (recettes == null) {
			recettes = List.of(
					new Recette(Brassage::étrange, () -> PotionContents.createItemStack(Items.POTION, Potions.AWKWARD),
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
			if (ingrédient.is(r.ingrédient()) && r.base().test(base)) {
				return r;
			}
		}
		return null;
	}

	public static boolean estIngrédient(ItemStack stack) {
		return recettes().stream().anyMatch(r -> stack.is(r.ingrédient()));
	}

	/** Une de nos potions : elle peut tenir dans un emplacement à fiole. */
	public static boolean estPotion(ItemStack stack) {
		return recettes().stream().anyMatch(r -> stack.is(r.résultat()));
	}
}
