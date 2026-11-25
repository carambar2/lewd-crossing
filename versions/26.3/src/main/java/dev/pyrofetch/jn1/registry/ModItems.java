package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	private static final List<Item> BLOCK_ITEMS = new ArrayList<>();
	private static final List<Item> ITEMS = new ArrayList<>();

	/**
	 * La hache de pompier : de l'acier, donc les caractéristiques du fer.
	 *
	 * <p>Elle frappe un peu plus fort qu'une hache ordinaire et se manie un peu
	 * plus lentement — c'est un outil de force, pas de finesse. Le reste vient
	 * du matériau, sans quoi il aurait fallu écrire une classe pour une seule
	 * différence de chiffres.
	 */
	public static final Item FIRE_AXE = register("fire_axe", Item::new,
			// 26.3 : plus de classe AxeItem ; la hache est un objet ordinaire aux
			// composants d'outil (minage, écorçage, dégâts), comme la hache en fer du jeu.
			new Item.Properties().axe(net.minecraft.world.item.ToolMaterial.IRON, 7.0F, -3.2F));

	/** La capsule de capture. Une seule à la fois : son contenu la distingue. */
	public static final Item CAPSULE = register("capsule", CapsuleItem::new,
			new Item.Properties().stacksTo(1));

	/** L'appareil photo, et le tirage qu'il produit. */
	public static final Item CAMERA = register("camera", dev.pyrofetch.jn1.photo.CameraItem::new,
			new Item.Properties().stacksTo(1));
	public static final Item PHOTO = register("photo", dev.pyrofetch.jn1.photo.PhotoItem::new,
			new Item.Properties().stacksTo(1));

	/**
	 * La matière noire : ce que vend le vide, et ce qu'on y porte.
	 *
	 * <p>Elle ne s'achète qu'à un guichet situé dans la dimension du vide (voir
	 * {@link dev.pyrofetch.jn1.shop.ShopEntry}) et sert à coudre la tenue qui
	 * permet d'y nager.
	 */
	public static final Item DARK_MATTER = register("dark_matter", dev.pyrofetch.jn1.lab.DarkMatterItem::new,
			new Item.Properties());

	/** La tenue du vide, en quatre pièces. Il les faut toutes pour nager. */
	public static final Item VOID_HELMET = tenue("void_helmet", ArmorType.HELMET);
	public static final Item VOID_SUIT = tenue("void_suit", ArmorType.CHESTPLATE);
	public static final Item VOID_LEGGINGS = tenue("void_leggings", ArmorType.LEGGINGS);
	public static final Item VOID_BOOTS = tenue("void_boots", ArmorType.BOOTS);

	/** Le lidar : il repère les structures du vide. Rangé avec la tenue, dans les sites. */
	public static final Item LIDAR = register("lidar", dev.pyrofetch.jn1.lab.LidarItem::new,
			new Item.Properties().stacksTo(1));

	/**
	 * La télécommande de scènes : sur une compagne, elle ouvre la liste des
	 * animations qu'elle sait jouer (voir {@link dev.pyrofetch.jn1.entity.CompanionScenes}).
	 * C'est la compagne qui la reconnaît, dans {@code interactMob} : l'objet
	 * lui-même ne fait rien.
	 */
	public static final Item SCENE_REMOTE = register("scene_remote", dev.pyrofetch.jn1.interaction.SceneRemoteItem::new,
			new Item.Properties().stacksTo(1));

	/** Le carnet de terrain, qui remplace le manuel du laboratoire. */
	public static final Item NOTEBOOK = register("notebook", dev.pyrofetch.jn1.notebook.FieldNotebookItem::new,
			new Item.Properties().stacksTo(1));

	/** La Horny Potion : on la boit pour se transformer en l'une des compagnes. */
	public static final Item HORNY_POTION = register("horny_potion", dev.pyrofetch.jn1.morph.HornyPotionItem::new,
			new Item.Properties().stacksTo(16));

	/** Le cum, en fiole : il se verse sur un bloc ou sur une fille. */
	public static final Item CUM = register("cum", dev.pyrofetch.jn1.cum.CumItem::new,
			new Item.Properties().stacksTo(16));

	/** Le cum séché, ramassé à la main sur une trace : il se mange. */
	public static final Item DRIED_CUM = register("dried_cum", Item::new, new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build(),
					// « fast » de 1.21.1 : mangé en 16 ticks au lieu de 32.
					net.minecraft.world.item.component.Consumables.defaultFood().consumeSeconds(0.8F).build()));

	/** Le fromage, sorti d'un chaudron de cum caillé. */
	public static final Item CUM_CHEESE = register("cum_cheese", Item::new, new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build()));

	/** La flèche collante : elle ralentit ce qu'elle touche et laisse une trace où elle se plante. */
	public static final Item CUM_ARROW = register("cum_arrow", dev.pyrofetch.jn1.cum.CumArrowItem::new,
			new Item.Properties());

	/** La fiole à jeter : elle éclate en traces, aveugle et ralentit alentour. */
	public static final Item SPLASH_CUM = register("splash_cum", dev.pyrofetch.jn1.cum.SplashCumItem::new,
			new Item.Properties().stacksTo(16));

	/** Le bocal à cum : son contenu (69 fioles, une figurine) voyage avec lui. */
	public static final Item CUM_JAR = register("cum_jar", p -> new dev.pyrofetch.jn1.cum.CumJarItem(ModBlocks.CUM_JAR, p),
			new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));

	/** La figurine de poney, qui va au fond du bocal (la référence à MLP). */
	public static final Item FIGURINE = register("figurine", Item::new, new Item.Properties());

	/** Le Cum Crystal : du cum compressé, la monnaie de Gaster. */
	public static final Item CUM_CRYSTAL = register("cum_crystal", Item::new,
			new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));

	/** Le dildo en main : il se pose, ou sert de plaisir à une femme. */
	public static final Item DILDO = register("dildo", p -> new dev.pyrofetch.jn1.cum.DildoItem(ModBlocks.DILDO, p),
			new Item.Properties().useBlockDescriptionPrefix().stacksTo(1));

	/** Quatre dildos en carré. Deux fois plus vite. */
	public static final Item HORSE_DILDO = register("horse_dildo", p -> new dev.pyrofetch.jn1.cum.DildoItem(ModBlocks.HORSE_DILDO, p, 150),
			new Item.Properties().useBlockDescriptionPrefix().stacksTo(1));

	/**
	 * Excalibur, le dildo d'éléphant : quatre de cheval. Légendaire, il ne brûle
	 * pas, et frappe comme une épée de netherite un peu plus lente.
	 */
	public static final Item EXCALIBUR = register("excalibur", p -> new dev.pyrofetch.jn1.cum.DildoItem(ModBlocks.EXCALIBUR, p, 75),
			new Item.Properties().useBlockDescriptionPrefix().stacksTo(1)
					.rarity(net.minecraft.world.item.Rarity.EPIC)
					.fireResistant()
					.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
							.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
									new net.minecraft.world.entity.ai.attributes.AttributeModifier(
											Item.BASE_ATTACK_DAMAGE_ID, 8.0,
											net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
									net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
							.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
									new net.minecraft.world.entity.ai.attributes.AttributeModifier(
											Item.BASE_ATTACK_SPEED_ID, -2.6,
											net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
									net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
							.build()));

	/** Le masturbateur : le plaisir d'un homme. */
	public static final Item MASTURBATOR = register("masturbator", dev.pyrofetch.jn1.excitation.MasturbatorItem::new,
			new Item.Properties().stacksTo(1));

	/** La potion d'inversion : homme ou femme, l'autre. */
	public static final Item GENDER_POTION = register("gender_potion", dev.pyrofetch.jn1.gender.GenderPotionItem::new,
			new Item.Properties().stacksTo(16));

	/** Les menottes : menotter une fille, tenir sa chaîne. */
	public static final Item HANDCUFFS = register("handcuffs", dev.pyrofetch.jn1.jouets.HandcuffsItem::new,
			new Item.Properties().stacksTo(1));

	/** Le fouet : une arme légère, qui fait gémir les filles sans les blesser. */
	public static final Item WHIP = register("whip", dev.pyrofetch.jn1.jouets.WhipItem::new,
			new Item.Properties().stacksTo(1));

	/** Le dildo pogo : on rebondit dessus, un moyen de transport. */
	public static final Item DILDO_POGO = register("dildo_pogo", dev.pyrofetch.jn1.jouets.DildoPogoItem::new,
			new Item.Properties().stacksTo(1));

	/** Les sous-vêtements d'une fille, tombés quand elle se déshabille ({@link dev.pyrofetch.jn1.jouets.Lingerie}). */
	public static final Item PANTIES = register("panties", Item::new, new Item.Properties().stacksTo(16));
	public static final Item BRA = register("bra", Item::new, new Item.Properties().stacksTo(16));

	// Les potions de forme : poitrine, croupe, taille, d'un cran en plus ou en moins.
	public static final Item BOOBS_UP_POTION = forme("boobs_up_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS, 1);
	public static final Item BOOBS_DOWN_POTION = forme("boobs_down_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS, -1);
	public static final Item BOOTY_UP_POTION = forme("booty_up_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY, 1);
	public static final Item BOOTY_DOWN_POTION = forme("booty_down_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY, -1);
	public static final Item SIZE_UP_POTION = forme("size_up_potion", dev.pyrofetch.jn1.shape.Shapes.Part.SIZE, 1);
	public static final Item SIZE_DOWN_POTION = forme("size_down_potion", dev.pyrofetch.jn1.shape.Shapes.Part.SIZE, -1);

	private static Item forme(String nom, dev.pyrofetch.jn1.shape.Shapes.Part part, int delta) {
		return register(nom, p -> new dev.pyrofetch.jn1.shape.ShapePotionItem(part, delta, p), new Item.Properties().stacksTo(16));
	}

	// Les œufs d'apparition, pour la commodité : le modèle générique du jeu,
	// aux deux couleurs principales de chaque fille (fond, taches).
	// Les couleurs des œufs du mod d'origine (Jenny Mod 1.9.0, `EntityRegistry.registerModEntity`
	// dans `com.schnurritv.sexmod.r`) : violet sombre et rose pour Jenny, noir et rouge sang pour Ellie.
	// 26.3 : les œufs n'ont plus de couleurs dans le code (depuis 1.21.5, chaque œuf a
	// sa planche, `assets/jn1/items/<œuf>.json`) ; les couleurs restent notées ici
	// pour les planches : Jenny 0x322640 / 0xC11A85, Ellie 0x161616 / 0x980000,
	// FapGPT 0x1C2A5C / 0x46DCF0 (opaque : 0x1C2A5C / 0xA8B4C8), Gaster 0x111114 / 0xE8E8E8.
	public static final Item JENNY_SPAWN_EGG = register("jenny_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.JENNY));
	public static final Item ELLIE_SPAWN_EGG = register("ellie_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.ELLIE));
	// L'œuf normal (l'hologramme) d'abord : le jeu ne retient qu'un œuf par
	// créature, celui que donnent le clic molette et la boutique de Gaster. En
	// 1.21.1, c'était le dernier déclaré ; en 26.3 (`SpawnEggItem.byId`), c'est
	// le premier trouvé dans le registre, donc le premier déclaré.
	/** Aura, éveillée : on la trouve en veille au laboratoire, l'œuf sert à l'avoir tout de suite. */
	public static final Item AURA_SPAWN_EGG = register("aura_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.AURA)
					.component(net.minecraft.core.component.DataComponents.ENTITY_DATA, donnéesAura(true)));
	/** Aura pleine, sans hologramme : pour juger sa planche telle qu'elle est. */
	public static final Item AURA_OPAQUE_SPAWN_EGG = register("aura_opaque_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.AURA)
					.component(net.minecraft.core.component.DataComponents.ENTITY_DATA, donnéesAura(false)));

	/** Gaster : son modèle lisse (la forme en cubes a été abandonnée en 0.56.1). */
	public static final Item GASTER_SPAWN_EGG = register("gaster_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.GASTER));

	private static net.minecraft.world.item.component.TypedEntityData<net.minecraft.world.entity.EntityType<?>> donnéesAura(
			boolean hologramme) {
		net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
		nbt.putBoolean("Dormant", false);
		nbt.putBoolean("Hologram", hologramme);
		return net.minecraft.world.item.component.TypedEntityData.of(ModEntities.AURA, nbt);
	}

	private static Item tenue(String nom, ArmorType pièce) {
		return register(nom, Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.VOID_SUIT, pièce));
	}

	private ModItems() {
	}

	/**
	 * L'icône de l'onglet créatif : la tête de Jenny, de face (0.81.3).
	 * Un objet sans usage, enregistré hors de la liste de l'onglet ; sa planche
	 * est un rendu de son modèle (`textures/item/tab_icon.png`, `art/`).
	 */
	public static final Item TAB_ICON = enregistrer("tab_icon", Item::new, new Item.Properties());

	private static Item register(String path, Function<Item.Properties, Item> fabrique, Item.Properties propriétés) {
		Item registered = enregistrer(path, fabrique, propriétés);
		ITEMS.add(registered);
		return registered;
	}

	/**
	 * Enregistre un objet comme {@code Items.registerItem} du jeu : depuis 1.21.2,
	 * les propriétés portent l'identifiant avant la construction, et un objet de
	 * bloc se déclare lui-même dans {@code Item.BY_BLOCK}.
	 */
	private static Item enregistrer(String path, Function<Item.Properties, Item> fabrique, Item.Properties propriétés) {
		ResourceKey<Item> clé = ResourceKey.create(Registries.ITEM, JN1.id(path));
		Item item = fabrique.apply(propriétés.setId(clé));
		if (item instanceof BlockItem objetDeBloc) {
			objetDeBloc.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, clé, item);
	}

	private static void registerBlockItem(Block block) {
		String path = ModBlocks.idOf(block).getPath();
		// La description du bloc (« block.jn1.… »), comme le faisait BlockItem en 1.21.1.
		BLOCK_ITEMS.add(enregistrer(path, p -> new BlockItem(block, p),
				new Item.Properties().useBlockDescriptionPrefix()));
	}

	public static void register() {
		for (Block block : ModBlocks.all()) {
			registerBlockItem(block);
		}
		JN1.LOGGER.debug("Objets enregistrés.");
	}

	public static List<Item> blockItems() {
		return BLOCK_ITEMS;
	}

	public static List<Item> all() {
		return ITEMS;
	}
}
