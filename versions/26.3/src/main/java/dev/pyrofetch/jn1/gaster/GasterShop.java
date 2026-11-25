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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.gamerules.GameRules;

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
	/**
	 * 26.3 : une règle de jeu est nommée par un identifiant, en minuscules :
	 * {@code jn1:gaster_shop} (c'était {@code jn1GasterShop}). La valeur réglée
	 * dans un monde de la 1.21.1 n'est pas reprise : elle repart à vrai.
	 */
	public static final net.minecraft.world.level.gamerules.GameRule<Boolean> RÈGLE =
			net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder.forBoolean(true)
					.category(net.minecraft.world.level.gamerules.GameRuleCategory.MISC)
					.buildAndRegister(dev.pyrofetch.jn1.JN1.id("gaster_shop"));

	public static final int ARTICLES = 4;
	private static final double PORTÉE = 8.0;

	/** Le rang d'un article : son prix en cristaux, sa chance d'être tiré, sa couleur. */
	public enum Rang {
		COMMUN(1, 60, ChatFormatting.WHITE),
		RARE(2, 25, ChatFormatting.AQUA),
		INTERDIT(3, 11, ChatFormatting.LIGHT_PURPLE),
		IMPOSSIBLE(5, 4, ChatFormatting.RED);

		public final int prix;
		final int poids;
		public final ChatFormatting couleur;

		Rang(int prix, int poids, ChatFormatting couleur) {
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
				(payload, context) -> context.server().execute(() -> recevoir(context.player(), payload)));
	}

	public static boolean ouverte(ServerLevel monde) {
		return monde.getGameRules().get(RÈGLE);
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
		// 26.3 : plus de liste des œufs ; on les prend dans le registre des objets.
		for (Item objet : BuiltInRegistries.ITEM) {
			if (!(objet instanceof SpawnEggItem œuf)) {
				continue;
			}
			String nom = BuiltInRegistries.ITEM.getKey(œuf).getPath();
			boolean rare = ŒUFS_RARES.contains(nom) && BuiltInRegistries.ITEM.getKey(œuf).getNamespace().equals("minecraft");
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
			Identifier id = Identifier.withDefaultNamespace(nom);
			if (BuiltInRegistries.ITEM.containsKey(id)) {
				liste.add(BuiltInRegistries.ITEM.getValue(id));
			}
		}
	}

	/** Quatre articles, tous différents, chacun tiré d'abord à son rang. */
	public static List<Article> tirer(RandomSource hasard) {
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
	public static void ouvrir(ServerPlayer joueur, GasterEntity gaster) {
		envoyer(joueur, gaster, GasterShopPayload.RIEN);
	}

	private static void envoyer(ServerPlayer joueur, GasterEntity gaster, int retour) {
		List<Article> étal = gaster.étal();
		List<ItemStack> objets = new ArrayList<>();
		List<Integer> rangs = new ArrayList<>();
		for (Article a : étal) {
			objets.add(a.objet().copy());
			rangs.add(a.rang().ordinal());
		}
		ServerPlayNetworking.send(joueur, new GasterShopPayload(gaster.getId(), objets, rangs, gaster.vendus(), retour));
	}

	private static void recevoir(ServerPlayer joueur, GasterShopActionPayload demande) {
		if (!(joueur.level().getEntity(demande.entityId()) instanceof GasterEntity gaster)
				|| !gaster.isAlive() || joueur.distanceToSqr(gaster) > PORTÉE * PORTÉE
				|| !ouverte(joueur.level())) {
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
			if (joueur.getInventory().countItem(ModItems.CUM_CRYSTAL) < article.rang().prix) {
				envoyer(joueur, gaster, GasterShopPayload.PAUVRE);
				return;
			}
			payer(joueur, article.rang().prix);
		}
		gaster.vendre(i);
		joueur.getInventory().placeItemBackInInventory(article.objet().copy(), net.minecraft.util.Prediction.SERVER_ONLY);
		envoyer(joueur, gaster, GasterShopPayload.MERCI);
	}

	private static void payer(ServerPlayer joueur, int prix) {
		var inventaire = joueur.getInventory();
		for (int s = 0; s < inventaire.getContainerSize() && prix > 0; s++) {
			ItemStack pile = inventaire.getItem(s);
			if (pile.is(ModItems.CUM_CRYSTAL)) {
				int pris = Math.min(prix, pile.getCount());
				pile.shrink(pris);
				prix -= pris;
			}
		}
		inventaire.setChanged();
	}
}
