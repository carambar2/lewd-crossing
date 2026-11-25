package dev.pyrofetch.jn1.client.modeles;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.jouets.Lingerie;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * La teinte d'une culotte ou d'un soutien-gorge : la couleur de la fille qui
 * le portait, rangée dans les données de la pièce ({@link Lingerie#couleur}).
 *
 * <p>En 1.21.1, c'était un {@code ColorProviderRegistry.ITEM} ; en 26.3, la
 * teinte d'un objet se déclare dans sa définition
 * ({@code assets/jn1/items/panties.json} : {@code "tints": [{"type": "jn1:lingerie"}]})
 * et le type se fait connaître du jeu par {@link ModelesObjets#enregistrer()}.
 * Une pièce lâchée avant le portage garde sa couleur : elle est lue au même
 * endroit qu'avant.
 */
public final class TeinteLingerie implements ItemTintSource {
	public static final TeinteLingerie INSTANCE = new TeinteLingerie();
	public static final MapCodec<TeinteLingerie> MAP_CODEC = MapCodec.unit(INSTANCE);

	private TeinteLingerie() {
	}

	@Override
	public int calculate(ItemStack pile, @Nullable ClientLevel monde, @Nullable LivingEntity porteur) {
		return ARGB.opaque(Lingerie.couleur(pile));
	}

	@Override
	public MapCodec<TeinteLingerie> type() {
		return MAP_CODEC;
	}
}
