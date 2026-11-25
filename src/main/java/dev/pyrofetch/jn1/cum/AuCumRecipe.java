package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import dev.pyrofetch.jn1.JN1;

/**
 * Un plat « au cum » : n'importe quelle nourriture et un cum séché, sans
 * forme, à l'établi. Le plat garde tout ce qu'il était, prend un point de
 * nourriture de plus et s'appelle « Steak au cum », « Pomme au cum »…
 *
 * <p>Une seule fois par plat : la marque {@code jn1_au_cum}, dans ses données,
 * l'empêche de revenir. Le plat ainsi changé ne s'empile plus avec le plat
 * d'origine, ce qui est voulu.
 */
public class AuCumRecipe extends SpecialCraftingRecipe {
	public static final String MARQUE = "jn1_au_cum";
	/** Ce que le plat nourrit de plus, mangé. */
	public static final int BONUS = 1;
	public static final RecipeSerializer<AuCumRecipe> SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER,
			JN1.id("au_cum"), new SpecialRecipeSerializer<>(AuCumRecipe::new));

	public AuCumRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	public static void register() {
		// L'enregistrement se fait au chargement de la classe.
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		return !plat(input).isEmpty();
	}

	/** Le plat, s'il y a exactement un plat et un cum séché, et rien d'autre. */
	private static ItemStack plat(CraftingRecipeInput input) {
		ItemStack plat = ItemStack.EMPTY;
		int séchés = 0;
		for (ItemStack pile : input.getStacks()) {
			if (pile.isEmpty()) {
				continue;
			}
			if (pile.isOf(ModItems.DRIED_CUM) && séchés == 0) {
				séchés++;
			} else if (plat.isEmpty() && pile.contains(DataComponentTypes.FOOD) && !déjàAuCum(pile)
					&& !pile.isOf(ModItems.DRIED_CUM)) {
				plat = pile;
			} else {
				return ItemStack.EMPTY;
			}
		}
		return séchés == 1 ? plat : ItemStack.EMPTY;
	}

	public static boolean déjàAuCum(ItemStack pile) {
		NbtComponent données = pile.get(DataComponentTypes.CUSTOM_DATA);
		return données != null && données.contains(MARQUE);
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
		ItemStack plat = plat(input);
		return plat.isEmpty() ? ItemStack.EMPTY : auCum(plat);
	}

	/**
	 * Le plat, « au cum » : son nom et la marque. Aussi pour l'onglet créatif.
	 *
	 * <p>Le point de nourriture de plus se donne quand on le mange
	 * ({@link #BONUS}, {@code LivingEntityMixin}) et non plus dans le plat :
	 * depuis 0.56.7, les nourritures du jeu ont aussi chacune une vraie recette
	 * (`data/jn1/recipe/au_cum/`, `gen_cum.py`), visible au livre de recettes, où la recette
	 * spéciale, seule, ne paraissait pas. Ces recettes ne savent que poser des données
	 * fixes ; le même plat, par l'une ou par l'autre, doit être identique (et
	 * s'empiler).
	 */
	public static ItemStack auCum(ItemStack plat) {
		ItemStack résultat = plat.copyWithCount(1);
		résultat.set(DataComponentTypes.ITEM_NAME,
				Text.translatable("item.jn1.au_cum", plat.getItem().getName(plat)));
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, résultat, nbt -> nbt.putBoolean(MARQUE, true));
		return résultat;
	}

	/**
	 * Rien ne reste dans la grille : le plat garde son propre reste (le flacon
	 * du miel revient quand on le boit). Sinon le flacon du miel restait aussi
	 * dans la grille — un flacon de plus à chaque plat.
	 */
	@Override
	public net.minecraft.util.collection.DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
		return net.minecraft.util.collection.DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY);
	}

	@Override
	public boolean fits(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
