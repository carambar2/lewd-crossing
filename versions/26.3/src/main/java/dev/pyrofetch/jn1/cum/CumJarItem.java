package dev.pyrofetch.jn1.cum;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/**
 * Le bocal à cum en main : son contenu est l'état de bloc qu'il porte
 * ({@code minecraft:block_state}, recopié par le butin), lu ici pour
 * l'info-bulle, le compresseur et le modèle (qui suit le remplissage).
 */
public class CumJarItem extends BlockItem {
	public CumJarItem(Block block, Properties settings) {
		super(block, settings);
	}

	public static int cum(ItemStack stack) {
		Integer cum = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
				.get(CumJarBlock.CUM);
		return cum == null ? 0 : cum;
	}

	public static boolean figurine(ItemStack stack) {
		return Boolean.TRUE.equals(stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
				.get(CumJarBlock.FIGURINE));
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
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		super.appendHoverText(stack, context, affichage, tooltip, type);
		tooltip.accept(Component.translatable("tooltip.jn1.cum_jar.cum", cum(stack), CumJarBlock.PLEIN)
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable(figurine(stack) ? "tooltip.jn1.cum_jar.avec_figurine"
				: "tooltip.jn1.cum_jar.sans_figurine").withStyle(ChatFormatting.GRAY));
	}
}
