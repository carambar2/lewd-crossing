package dev.pyrofetch.jn1.client.compat;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.brewing.Brassage;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Le greffon JEI (0.84.0).
 *
 * <p>Les recettes d'établi, JEI les lit seul. Celles d'alambic, il les
 * cherche en essayant chaque ingrédient des mélanges du jeu sur chaque potion
 * du jeu (sous Fabric, {@code potionMixes} et {@code containerMixes}) : nos
 * ingrédients n'y sont pas — le coquelicot n'est l'ingrédient d'aucune potion
 * du jeu — et nos potions ne sont pas des potions du jeu. Il ne les trouvait
 * donc jamais. On lui déclare la table de {@link Brassage} telle quelle.
 *
 * <p>Point d'entrée {@code jei_mod_plugin} de {@code fabric.mod.json} : sans
 * JEI, personne ne le charge, et ses classes ne sont jamais lues.
 */
public final class JeiCompat implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return Identifier.of(JN1.MOD_ID, "jei");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		IVanillaRecipeFactory fabrique = registration.getVanillaRecipeFactory();
		List<IJeiBrewingRecipe> recettes = new ArrayList<>();
		for (Brassage.Recette r : Brassage.recettes()) {
			Identifier résultat = Registries.ITEM.getId(r.résultat());
			recettes.add(fabrique.createBrewingRecipe(
					List.of(new ItemStack(r.ingrédient())),
					r.montrée().get(),
					new ItemStack(r.résultat()),
					Identifier.of(JN1.MOD_ID, "brewing/" + résultat.getPath())));
		}
		registration.addRecipes(RecipeTypes.BREWING, recettes);
	}
}
