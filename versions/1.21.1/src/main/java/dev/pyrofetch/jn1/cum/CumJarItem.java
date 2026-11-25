package dev.pyrofetch.jn1.cum;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Le bocal à cum en main : son contenu est l'état de bloc qu'il porte
 * ({@code minecraft:block_state}, recopié par le butin), lu ici pour
 * l'info-bulle, le compresseur et le modèle (qui suit le remplissage).
 */
public class CumJarItem extends BlockItem {
	public CumJarItem(Block block, Settings settings) {
		super(block, settings);
	}

	public static int cum(ItemStack stack) {
		Integer cum = stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
				.getValue(CumJarBlock.CUM);
		return cum == null ? 0 : cum;
	}

	public static boolean figurine(ItemStack stack) {
		return Boolean.TRUE.equals(stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
				.getValue(CumJarBlock.FIGURINE));
	}

	/** Plein et garni : bon pour le compresseur. */
	public static boolean prêt(ItemStack stack) {
		return cum(stack) >= CumJarBlock.PLEIN && figurine(stack);
	}

	/** De 0 à 8 : la hauteur dessinée (même calcul que {@code tools/gen_boutique_gaster.py}). */
	public static int niveau(ItemStack stack) {
		int cum = cum(stack);
		return cum <= 0 ? 0 : Math.max(1, Math.round(cum * 8.0F / CumJarBlock.PLEIN));
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.jn1.cum_jar.cum", cum(stack), CumJarBlock.PLEIN)
				.formatted(Formatting.GRAY));
		tooltip.add(Text.translatable(figurine(stack) ? "tooltip.jn1.cum_jar.avec_figurine"
				: "tooltip.jn1.cum_jar.sans_figurine").formatted(Formatting.GRAY));
	}
}
