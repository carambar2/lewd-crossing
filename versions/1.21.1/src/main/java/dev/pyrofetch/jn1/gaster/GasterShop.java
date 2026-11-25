package dev.pyrofetch.jn1.gaster;

import dev.pyrofetch.jn1.network.GasterShopActionPayload;
import dev.pyrofetch.jn1.network.GasterShopPayload;
import dev.pyrofetch.jn1.registry.ModItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;

/**
 * La boutique de Gaster.
 *
 * <ul>
 * <li><strong>On y entre en le touchant</strong> ; en sortant, il s'en va en
 * lâchant son œuf, comme avant. Il ne tient donc boutique qu'une visite.</li>
 * <li><strong>Quatre articles</strong>, tirés à sa première ouverture et
 * gardés avec lui ({@code Boutique} au NBT) : <em>uniquement des objets
 * illégaux</em>, qu'on ne fabrique pas — œufs d'apparition (les plus
 * fréquents), blocs naturels qu'on ne récolte pas, générateurs, blocs de
 * commande et de structure.</li>
 * <li><strong>On paie en Cum Crystals</strong>, d'autant plus que l'article
 * est rare ({@link Rang}). Le créatif ne paie pas.</li>
 * <li><strong>Une règle de jeu</strong>, {@code jn1GasterShop} (vraie par
 * défaut), pour les serveurs : fausse, le toucher donne l'œuf aussitôt, sans
 * boutique.</li>
 * </ul>
 *
 * <p>Le serveur tient tout : le client n'envoie qu'un numéro d'article, et
 * tout est revérifié (Gaster vivant, à {@link #PORTÉE} blocs, l'article
 * invendu, les cristaux en poche).
 */
public final class GasterShop {
	public static final GameRules.Key<GameRules.BooleanRule> RÈGLE = GameRuleRegistry.register(
			"jn1GasterShop", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

	public static final int ARTICLES = 4;
	private static final double PORTÉE = 8.0;

	/** Le rang d'un article : son prix en cristaux, sa chance d'être tiré, sa couleur. */
	public enum Rang {
		COMMUN(1, 60, Formatting.WHITE),
		RARE(2, 25, Formatting.AQUA),
		INTERDIT(3, 11, Formatting.LIGHT_PURPLE),
		IMPOSSIBLE(5, 4, Formatting.RED);

		public final int prix;
		final int poids;
		public final Formatting couleur;

		Rang(int prix, int poids, Formatting couleur) {
			this.prix = prix;
			this.poids = poids;
			this.couleur = couleur;
		}

		public String clé() {
			return "gaster_shop.jn1.rang." + this.name().toLowerCase(java.util.Locale.ROOT);
		}

		public static Rang de(int ordinal) {
			Rang[] tous = values();
			return tous[Math.floorMod(ordinal, tous.length)];
		}
	}

	public record Article(ItemStack objet, Rang rang) {
	}

	/** Les œufs qui font venir un boss ou une bête qu'on ne croise pas tous les jours. */
	private static final Set<String> ŒUFS_RARES = Set.of("warden_spawn_egg", "elder_guardian_spawn_egg",
			"wither_spawn_egg", "ender_dragon_spawn_egg", "iron_golem_spawn_egg", "snow_golem_spawn_egg");
	/** Des blocs du monde que rien ne récolte. */
	private static final List<String> RARES = List.of("budding_amethyst", "reinforced_deepslate", "farmland",
			"dirt_path", "frogspawn", "infested_stone", "infested_deepslate", "petrified_oak_slab", "chorus_plant",
			"suspicious_sand", "suspicious_gravel");
	private static final List<String> INTERDITS = List.of("spawner", "trial_spawner", "vault", "bedrock",
			"end_portal_frame", "barrier", "light", "structure_void", "knowledge_book", "debug_stick", "player_head");
	private static final List<String> IMPOSSIBLES = List.of("command_block", "chain_command_block",
			"repeating_command_block", "command_block_minecart", "structure_block", "jigsaw");

	private static Map<Rang, List<Item>> catalogue;

	private GasterShop() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(GasterShopActionPayload.ID,
				(payload, context) -> context.player().server.execute(() -> recevoir(context.player(), payload)));
	}

	public static boolean ouverte(ServerWorld monde) {
		return monde.getGameRules().getBoolean(RÈGLE);
	}

	/** Lu une fois dans les registres : un objet qu'un autre jeu n'a pas est simplement absent. */
	private static Map<Rang, List<Item>> catalogue() {
		if (catalogue != null) {
			return catalogue;
		}
		Map<Rang, List<Item>> c = new EnumMap<>(Rang.class);
		for (Rang r : Rang.values()) {
			c.put(r, new ArrayList<>());
		}
		for (SpawnEggItem œuf : SpawnEggItem.getAll()) {
			String nom = Registries.ITEM.getId(œuf).getPath();
			boolean rare = ŒUFS_RARES.contains(nom) && Registries.ITEM.getId(œuf).getNamespace().equals("minecraft");
			c.get(rare ? Rang.RARE : Rang.COMMUN).add(œuf);
		}
		ajouter(c.get(Rang.RARE), RARES);
		ajouter(c.get(Rang.INTERDIT), INTERDITS);
		ajouter(c.get(Rang.IMPOSSIBLE), IMPOSSIBLES);
		catalogue = c;
		return c;
	}

	private static void ajouter(List<Item> liste, List<String> noms) {
		for (String nom : noms) {
			Identifier id = Identifier.ofVanilla(nom);
			if (Registries.ITEM.containsId(id)) {
				liste.add(Registries.ITEM.get(id));
			}
		}
	}

	/** Quatre articles, tous différents, chacun tiré d'abord à son rang. */
	public static List<Article> tirer(Random hasard) {
		Map<Rang, List<Item>> c = catalogue();
		int total = 0;
		for (Rang r : Rang.values()) {
			total += c.get(r).isEmpty() ? 0 : r.poids;
		}
		List<Article> étal = new ArrayList<>();
		Set<Item> pris = new HashSet<>();
		for (int essai = 0; étal.size() < ARTICLES && essai < 200 && total > 0; essai++) {
			int t = hasard.nextInt(total);
			Rang rang = Rang.COMMUN;
			for (Rang r : Rang.values()) {
				if (c.get(r).isEmpty()) {
					continue;
				}
				if (t < r.poids) {
					rang = r;
					break;
				}
				t -= r.poids;
			}
			List<Item> liste = c.get(rang);
			Item objet = liste.get(hasard.nextInt(liste.size()));
			if (pris.add(objet)) {
				étal.add(new Article(new ItemStack(objet), rang));
			}
		}
		return étal;
	}

	/** Ouvre la boutique à ce joueur — l'étal est tiré à la première ouverture. */
	public static void ouvrir(ServerPlayerEntity joueur, GasterEntity gaster) {
		envoyer(joueur, gaster, GasterShopPayload.RIEN);
	}

	private static void envoyer(ServerPlayerEntity joueur, GasterEntity gaster, int retour) {
		List<Article> étal = gaster.étal();
		List<ItemStack> objets = new ArrayList<>();
		List<Integer> rangs = new ArrayList<>();
		for (Article a : étal) {
			objets.add(a.objet().copy());
			rangs.add(a.rang().ordinal());
		}
		ServerPlayNetworking.send(joueur, new GasterShopPayload(gaster.getId(), objets, rangs, gaster.vendus(), retour));
	}

	private static void recevoir(ServerPlayerEntity joueur, GasterShopActionPayload demande) {
		if (!(joueur.getServerWorld().getEntityById(demande.entityId()) instanceof GasterEntity gaster)
				|| !gaster.isAlive() || joueur.squaredDistanceTo(gaster) > PORTÉE * PORTÉE
				|| !ouverte(joueur.getServerWorld())) {
			return;
		}
		if (demande.action() == GasterShopActionPayload.SORTIR) {
			gaster.partir(joueur);
			return;
		}
		List<Article> étal = gaster.étal();
		int i = demande.index();
		if (i < 0 || i >= étal.size()) {
			return;
		}
		if ((gaster.vendus() & (1 << i)) != 0) {
			envoyer(joueur, gaster, GasterShopPayload.DÉJÀ);
			return;
		}
		Article article = étal.get(i);
		if (!joueur.isCreative()) {
			if (joueur.getInventory().count(ModItems.CUM_CRYSTAL) < article.rang().prix) {
				envoyer(joueur, gaster, GasterShopPayload.PAUVRE);
				return;
			}
			payer(joueur, article.rang().prix);
		}
		gaster.vendre(i);
		joueur.getInventory().offerOrDrop(article.objet().copy());
		envoyer(joueur, gaster, GasterShopPayload.MERCI);
	}

	private static void payer(ServerPlayerEntity joueur, int prix) {
		var inventaire = joueur.getInventory();
		for (int s = 0; s < inventaire.size() && prix > 0; s++) {
			ItemStack pile = inventaire.getStack(s);
			if (pile.isOf(ModItems.CUM_CRYSTAL)) {
				int pris = Math.min(prix, pile.getCount());
				pile.decrement(pris);
				prix -= pris;
			}
		}
		inventaire.markDirty();
	}
}
