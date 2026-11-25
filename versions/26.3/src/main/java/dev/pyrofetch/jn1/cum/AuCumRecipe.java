package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
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
public class AuCumRecipe extends CustomRecipe {
	public static final String MARQUE = "jn1_au_cum";
	/** Ce que le plat nourrit de plus, mangé. */
	public static final int BONUS = 1;
	/**
	 * 26.3 : une recette spéciale n'a plus d'état (sa catégorie est « divers ») ;
	 * son sérialiseur est un couple de codecs qui rendent toujours la même. Le
	 * fichier {@code au_cum.json} ({@code "category": "misc"}) se lit tel quel.
	 */
	public static final AuCumRecipe INSTANCE = new AuCumRecipe();
	public static final RecipeSerializer<AuCumRecipe> SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			JN1.id("au_cum"), new RecipeSerializer<>(com.mojang.serialization.MapCodec.unit(INSTANCE),
					net.minecraft.network.codec.StreamCodec.unit(INSTANCE)));

	public AuCumRecipe() {
	}

	public static void register() {
		// L'enregistrement se fait au chargement de la classe.
	}

	@Override
	public boolean matches(CraftingInput input, Level world) {
		return !plat(input).isEmpty();
	}

	/** Le plat, s'il y a exactement un plat et un cum séché, et rien d'autre. */
	private static ItemStack plat(CraftingInput input) {
		ItemStack plat = ItemStack.EMPTY;
		int séchés = 0;
		for (ItemStack pile : input.items()) {
			if (pile.isEmpty()) {
				continue;
			}
			if (pile.is(ModItems.DRIED_CUM) && séchés == 0) {
				séchés++;
			} else if (plat.isEmpty() && pile.has(DataComponents.FOOD) && !déjàAuCum(pile)
					&& !pile.is(ModItems.DRIED_CUM)) {
				plat = pile;
			} else {
				return ItemStack.EMPTY;
			}
		}
		return séchés == 1 ? plat : ItemStack.EMPTY;
	}

	public static boolean déjàAuCum(ItemStack pile) {
		CustomData données = pile.get(DataComponents.CUSTOM_DATA);
		return données != null && données.copyTag().contains(MARQUE);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
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
		résultat.set(DataComponents.ITEM_NAME,
				Component.translatable("item.jn1.au_cum", plat.getItem().getName(plat)));
		CustomData.update(DataComponents.CUSTOM_DATA, résultat, nbt -> nbt.putBoolean(MARQUE, true));
		return résultat;
	}

	/**
	 * Rien ne reste dans la grille : le plat garde son propre reste (le flacon
	 * du miel revient quand on le boit). Sinon le flacon du miel restait aussi
	 * dans la grille — un flacon de plus à chaque plat.
	 */
	@Override
	public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		return net.minecraft.core.NonNullList.withSize(input.size(), ItemStack.EMPTY);
	}

	// 26.3 : `canCraftInDimensions` n'existe plus ; `matches` suffit (il faut deux piles).

	@Override
	public RecipeSerializer<AuCumRecipe> getSerializer() {
		return SERIALIZER;
	}
}
