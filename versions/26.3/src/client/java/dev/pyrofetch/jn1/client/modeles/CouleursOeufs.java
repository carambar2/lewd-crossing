package dev.pyrofetch.jn1.client.modeles;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Les deux couleurs de l'œuf d'apparition de chaque créature, telles que
 * Minecraft 1.21.1 et le mod les donnaient : le fond, puis les taches.
 *
 * <p>Écrit par {@code outils/convertir_ressources.py} (ne pas retoucher à la
 * main) : en 26.3, un œuf n'a plus de couleurs ({@code SpawnEggItem.getColor}
 * n'existe plus, chaque œuf a sa planche), or la capsule pleine prend celles de
 * l'œuf de sa captive. La table est relevée dans le jar du jeu 1.21.1 et dans
 * {@code ModItems} du projet 1.21.1. Une créature apparue après 1.21.1 n'y est
 * pas : comme une créature sans œuf, la capsule garde alors ses couleurs à elle.
 */
public final class CouleursOeufs {
	private static final Map<String, int[]> COULEURS = Map.ofEntries(
			Map.entry("jn1:aura", new int[]{0x1C2A5C, 0x46DCF0}),
			Map.entry("jn1:ellie", new int[]{0x161616, 0x980000}),
			Map.entry("jn1:gaster", new int[]{0x111114, 0xE8E8E8}),
			Map.entry("jn1:jenny", new int[]{0x322640, 0xC11A85}),
			Map.entry("minecraft:allay", new int[]{0x00DAFF, 0x00ADFF}),
			Map.entry("minecraft:armadillo", new int[]{0xAD716D, 0x824848}),
			Map.entry("minecraft:axolotl", new int[]{0xFBC1E3, 0xA62D74}),
			Map.entry("minecraft:bat", new int[]{0x4C3E30, 0x0F0F0F}),
			Map.entry("minecraft:bee", new int[]{0xEDC343, 0x43241B}),
			Map.entry("minecraft:blaze", new int[]{0xF6B201, 0xFFF87E}),
			Map.entry("minecraft:bogged", new int[]{0x8A9C72, 0x314D1B}),
			Map.entry("minecraft:breeze", new int[]{0xAF94DF, 0x9166DF}),
			Map.entry("minecraft:camel", new int[]{0xFCC369, 0xCB9337}),
			Map.entry("minecraft:cat", new int[]{0xEFC88E, 0x957256}),
			Map.entry("minecraft:cave_spider", new int[]{0x0C424E, 0xA80E0E}),
			Map.entry("minecraft:chicken", new int[]{0xA1A1A1, 0xFF0000}),
			Map.entry("minecraft:cod", new int[]{0xC1A76A, 0xE5C48B}),
			Map.entry("minecraft:cow", new int[]{0x443626, 0xA1A1A1}),
			Map.entry("minecraft:creeper", new int[]{0x0DA70B, 0x000000}),
			Map.entry("minecraft:dolphin", new int[]{0x223B4D, 0xF9F9F9}),
			Map.entry("minecraft:donkey", new int[]{0x534539, 0x867566}),
			Map.entry("minecraft:drowned", new int[]{0x8FF1D7, 0x799C65}),
			Map.entry("minecraft:elder_guardian", new int[]{0xCECCBA, 0x747693}),
			Map.entry("minecraft:ender_dragon", new int[]{0x1C1C1C, 0xE079FA}),
			Map.entry("minecraft:enderman", new int[]{0x161616, 0x000000}),
			Map.entry("minecraft:endermite", new int[]{0x161616, 0x6E6E6E}),
			Map.entry("minecraft:evoker", new int[]{0x959B9B, 0x1E1C1A}),
			Map.entry("minecraft:fox", new int[]{0xD5B69F, 0xCC6920}),
			Map.entry("minecraft:frog", new int[]{0xD07444, 0xFFC77C}),
			Map.entry("minecraft:ghast", new int[]{0xF9F9F9, 0xBCBCBC}),
			Map.entry("minecraft:glow_squid", new int[]{0x095656, 0x85F1BC}),
			Map.entry("minecraft:goat", new int[]{0xA5947C, 0x55493E}),
			Map.entry("minecraft:guardian", new int[]{0x5A8272, 0xF17D30}),
			Map.entry("minecraft:hoglin", new int[]{0xC66E55, 0x5F6464}),
			Map.entry("minecraft:horse", new int[]{0xC09E7D, 0xEEE500}),
			Map.entry("minecraft:husk", new int[]{0x797061, 0xE6CC94}),
			Map.entry("minecraft:iron_golem", new int[]{0xDBCDC2, 0x74A332}),
			Map.entry("minecraft:llama", new int[]{0xC09E7D, 0x995F40}),
			Map.entry("minecraft:magma_cube", new int[]{0x340000, 0xFCFC00}),
			Map.entry("minecraft:mooshroom", new int[]{0xA00F10, 0xB7B7B7}),
			Map.entry("minecraft:mule", new int[]{0x1B0200, 0x51331D}),
			Map.entry("minecraft:ocelot", new int[]{0xEFDE7D, 0x564434}),
			Map.entry("minecraft:panda", new int[]{0xE7E7E7, 0x1B1B22}),
			Map.entry("minecraft:parrot", new int[]{0x0DA70B, 0xFF0000}),
			Map.entry("minecraft:phantom", new int[]{0x43518A, 0x88FF00}),
			Map.entry("minecraft:pig", new int[]{0xF0A5A2, 0xDB635F}),
			Map.entry("minecraft:piglin", new int[]{0x995F40, 0xF9F3A4}),
			Map.entry("minecraft:piglin_brute", new int[]{0x592A10, 0xF9F3A4}),
			Map.entry("minecraft:pillager", new int[]{0x532F36, 0x959B9B}),
			Map.entry("minecraft:polar_bear", new int[]{0xEEEEDE, 0xD5D6CD}),
			Map.entry("minecraft:pufferfish", new int[]{0xF6B201, 0x37C3F2}),
			Map.entry("minecraft:rabbit", new int[]{0x995F40, 0x734831}),
			Map.entry("minecraft:ravager", new int[]{0x757470, 0x5B5049}),
			Map.entry("minecraft:salmon", new int[]{0xA00F10, 0x0E8474}),
			Map.entry("minecraft:sheep", new int[]{0xE7E7E7, 0xFFB5B5}),
			Map.entry("minecraft:shulker", new int[]{0x946794, 0x4D3852}),
			Map.entry("minecraft:silverfish", new int[]{0x6E6E6E, 0x303030}),
			Map.entry("minecraft:skeleton", new int[]{0xC1C1C1, 0x494949}),
			Map.entry("minecraft:skeleton_horse", new int[]{0x68684F, 0xE5E5D8}),
			Map.entry("minecraft:slime", new int[]{0x51A03E, 0x7EBF6E}),
			Map.entry("minecraft:sniffer", new int[]{0x871E09, 0x25AB70}),
			Map.entry("minecraft:snow_golem", new int[]{0xD9F2F2, 0x81A4A4}),
			Map.entry("minecraft:spider", new int[]{0x342D27, 0xA80E0E}),
			Map.entry("minecraft:squid", new int[]{0x223B4D, 0x708899}),
			Map.entry("minecraft:stray", new int[]{0x617677, 0xDDEAEA}),
			Map.entry("minecraft:strider", new int[]{0x9C3436, 0x4D494D}),
			Map.entry("minecraft:tadpole", new int[]{0x6D533D, 0x160A00}),
			Map.entry("minecraft:trader_llama", new int[]{0xEAA430, 0x456296}),
			Map.entry("minecraft:tropical_fish", new int[]{0xEF6915, 0xFFF9EF}),
			Map.entry("minecraft:turtle", new int[]{0xE7E7E7, 0x00AFAF}),
			Map.entry("minecraft:vex", new int[]{0x7A90A4, 0xE8EDF1}),
			Map.entry("minecraft:villager", new int[]{0x563C33, 0xBD8B72}),
			Map.entry("minecraft:vindicator", new int[]{0x959B9B, 0x275E61}),
			Map.entry("minecraft:wandering_trader", new int[]{0x456296, 0xEAA430}),
			Map.entry("minecraft:warden", new int[]{0x0F4649, 0x39D6E0}),
			Map.entry("minecraft:witch", new int[]{0x340000, 0x51A03E}),
			Map.entry("minecraft:wither", new int[]{0x141414, 0x4D72A0}),
			Map.entry("minecraft:wither_skeleton", new int[]{0x141414, 0x474D4D}),
			Map.entry("minecraft:wolf", new int[]{0xD7D3D3, 0xCEAF96}),
			Map.entry("minecraft:zoglin", new int[]{0xC66E55, 0xE6E6E6}),
			Map.entry("minecraft:zombie", new int[]{0x00AFAF, 0x799C65}),
			Map.entry("minecraft:zombie_horse", new int[]{0x315234, 0x97C284}),
			Map.entry("minecraft:zombie_villager", new int[]{0x563C33, 0x799C65}),
			Map.entry("minecraft:zombified_piglin", new int[]{0xEA9393, 0x4C7129}));

	private CouleursOeufs() {
	}

	/** Fond et taches (RVB, sans alpha) de l'œuf de cette créature ({@code "minecraft:cow"}), ou {@code null}. */
	public static int @Nullable [] de(String créature) {
		int[] c = COULEURS.get(créature);
		return c == null ? null : c.clone();
	}
}
