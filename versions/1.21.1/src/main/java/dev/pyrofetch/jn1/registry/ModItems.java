package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleItem;
import net.minecraft.block.Block;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

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
	public static final Item FIRE_AXE = register("fire_axe",
			new AxeItem(ToolMaterials.IRON, new Item.Settings()
					.attributeModifiers(AxeItem.createAttributeModifiers(
							ToolMaterials.IRON, 7.0F, -3.2F))));

	/** La capsule de capture. Une seule à la fois : son contenu la distingue. */
	public static final Item CAPSULE = register("capsule",
			new CapsuleItem(new Item.Settings().maxCount(1)));

	/** L'appareil photo, et le tirage qu'il produit. */
	public static final Item CAMERA = register("camera",
			new dev.pyrofetch.jn1.photo.CameraItem(new Item.Settings().maxCount(1)));
	public static final Item PHOTO = register("photo",
			new dev.pyrofetch.jn1.photo.PhotoItem(new Item.Settings().maxCount(1)));

	/**
	 * La matière noire : ce que vend le vide, et ce qu'on y porte.
	 *
	 * <p>Elle ne s'achète qu'à un guichet situé dans la dimension du vide (voir
	 * {@link dev.pyrofetch.jn1.shop.ShopEntry}) et sert à coudre la tenue qui
	 * permet d'y nager.
	 */
	public static final Item DARK_MATTER = register("dark_matter",
			new dev.pyrofetch.jn1.lab.DarkMatterItem(new Item.Settings()));

	/** La tenue du vide, en quatre pièces. Il les faut toutes pour nager. */
	public static final Item VOID_HELMET = register("void_helmet", tenue(ArmorItem.Type.HELMET));
	public static final Item VOID_SUIT = register("void_suit", tenue(ArmorItem.Type.CHESTPLATE));
	public static final Item VOID_LEGGINGS = register("void_leggings", tenue(ArmorItem.Type.LEGGINGS));
	public static final Item VOID_BOOTS = register("void_boots", tenue(ArmorItem.Type.BOOTS));

	/** Le lidar : il repère les structures du vide. Rangé avec la tenue, dans les sites. */
	public static final Item LIDAR = register("lidar",
			new dev.pyrofetch.jn1.lab.LidarItem(new Item.Settings().maxCount(1)));

	/**
	 * La télécommande de scènes : sur une compagne, elle ouvre la liste des
	 * animations qu'elle sait jouer (voir {@link dev.pyrofetch.jn1.entity.CompanionScenes}).
	 * C'est la compagne qui la reconnaît, dans {@code interactMob} : l'objet
	 * lui-même ne fait rien.
	 */
	public static final Item SCENE_REMOTE = register("scene_remote",
			new dev.pyrofetch.jn1.interaction.SceneRemoteItem(new Item.Settings().maxCount(1)));

	/** Le carnet de terrain, qui remplace le manuel du laboratoire. */
	public static final Item NOTEBOOK = register("notebook",
			new dev.pyrofetch.jn1.notebook.FieldNotebookItem(new Item.Settings().maxCount(1)));

	/** La Horny Potion : on la boit pour se transformer en l'une des compagnes. */
	public static final Item HORNY_POTION = register("horny_potion",
			new dev.pyrofetch.jn1.morph.HornyPotionItem(new Item.Settings().maxCount(16)));

	/** Le cum, en fiole : il se verse sur un bloc ou sur une fille. */
	public static final Item CUM = register("cum",
			new dev.pyrofetch.jn1.cum.CumItem(new Item.Settings().maxCount(16)));

	/** Le cum séché, ramassé à la main sur une trace : il se mange. */
	public static final Item DRIED_CUM = register("dried_cum", new Item(new Item.Settings()
			.food(new net.minecraft.component.type.FoodComponent.Builder().nutrition(1).saturationModifier(0.2F).snack().build())));

	/** Le fromage, sorti d'un chaudron de cum caillé. */
	public static final Item CUM_CHEESE = register("cum_cheese", new Item(new Item.Settings()
			.food(new net.minecraft.component.type.FoodComponent.Builder().nutrition(4).saturationModifier(0.6F).build())));

	/** La flèche collante : elle ralentit ce qu'elle touche et laisse une trace où elle se plante. */
	public static final Item CUM_ARROW = register("cum_arrow",
			new dev.pyrofetch.jn1.cum.CumArrowItem(new Item.Settings()));

	/** La fiole à jeter : elle éclate en traces, aveugle et ralentit alentour. */
	public static final Item SPLASH_CUM = register("splash_cum",
			new dev.pyrofetch.jn1.cum.SplashCumItem(new Item.Settings().maxCount(16)));

	/** Le bocal à cum : son contenu (69 fioles, une figurine) voyage avec lui. */
	public static final Item CUM_JAR = register("cum_jar",
			new dev.pyrofetch.jn1.cum.CumJarItem(ModBlocks.CUM_JAR, new Item.Settings().maxCount(16)));

	/** La figurine de poney, qui va au fond du bocal (la référence à MLP). */
	public static final Item FIGURINE = register("figurine", new Item(new Item.Settings()));

	/** Le Cum Crystal : du cum compressé, la monnaie de Gaster. */
	public static final Item CUM_CRYSTAL = register("cum_crystal",
			new Item(new Item.Settings().rarity(net.minecraft.util.Rarity.RARE)));

	/** Le dildo en main : il se pose, ou sert de plaisir à une femme. */
	public static final Item DILDO = register("dildo",
			new dev.pyrofetch.jn1.cum.DildoItem(ModBlocks.DILDO, new Item.Settings().maxCount(1)));

	/** Quatre dildos en carré. Deux fois plus vite. */
	public static final Item HORSE_DILDO = register("horse_dildo",
			new dev.pyrofetch.jn1.cum.DildoItem(ModBlocks.HORSE_DILDO, new Item.Settings().maxCount(1), 150));

	/**
	 * Excalibur, le dildo d'éléphant : quatre de cheval. Légendaire, il ne brûle
	 * pas, et frappe comme une épée de netherite un peu plus lente.
	 */
	public static final Item EXCALIBUR = register("excalibur",
			new dev.pyrofetch.jn1.cum.DildoItem(ModBlocks.EXCALIBUR, new Item.Settings().maxCount(1)
					.rarity(net.minecraft.util.Rarity.EPIC)
					.fireproof()
					.attributeModifiers(net.minecraft.component.type.AttributeModifiersComponent.builder()
							.add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,
									new net.minecraft.entity.attribute.EntityAttributeModifier(
											Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 8.0,
											net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE),
									net.minecraft.component.type.AttributeModifierSlot.MAINHAND)
							.add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_SPEED,
									new net.minecraft.entity.attribute.EntityAttributeModifier(
											Item.BASE_ATTACK_SPEED_MODIFIER_ID, -2.6,
											net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE),
									net.minecraft.component.type.AttributeModifierSlot.MAINHAND)
							.build()), 75));

	/** Le masturbateur : le plaisir d'un homme. */
	public static final Item MASTURBATOR = register("masturbator",
			new dev.pyrofetch.jn1.excitation.MasturbatorItem(new Item.Settings().maxCount(1)));

	/** La potion d'inversion : homme ou femme, l'autre. */
	public static final Item GENDER_POTION = register("gender_potion",
			new dev.pyrofetch.jn1.gender.GenderPotionItem(new Item.Settings().maxCount(16)));

	/** Les menottes : menotter une fille, tenir sa chaîne. */
	public static final Item HANDCUFFS = register("handcuffs",
			new dev.pyrofetch.jn1.jouets.HandcuffsItem(new Item.Settings().maxCount(1)));

	/** Le fouet : une arme légère, qui fait gémir les filles sans les blesser. */
	public static final Item WHIP = register("whip",
			new dev.pyrofetch.jn1.jouets.WhipItem(new Item.Settings().maxCount(1)));

	/** Le dildo pogo : on rebondit dessus, un moyen de transport. */
	public static final Item DILDO_POGO = register("dildo_pogo",
			new dev.pyrofetch.jn1.jouets.DildoPogoItem(new Item.Settings().maxCount(1)));

	/** Les sous-vêtements d'une fille, tombés quand elle se déshabille ({@link dev.pyrofetch.jn1.jouets.Lingerie}). */
	public static final Item PANTIES = register("panties", new Item(new Item.Settings().maxCount(16)));
	public static final Item BRA = register("bra", new Item(new Item.Settings().maxCount(16)));

	// Les potions de forme : poitrine, croupe, taille, d'un cran en plus ou en moins.
	public static final Item BOOBS_UP_POTION = forme("boobs_up_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS, 1);
	public static final Item BOOBS_DOWN_POTION = forme("boobs_down_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS, -1);
	public static final Item BOOTY_UP_POTION = forme("booty_up_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY, 1);
	public static final Item BOOTY_DOWN_POTION = forme("booty_down_potion", dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY, -1);
	public static final Item SIZE_UP_POTION = forme("size_up_potion", dev.pyrofetch.jn1.shape.Shapes.Part.SIZE, 1);
	public static final Item SIZE_DOWN_POTION = forme("size_down_potion", dev.pyrofetch.jn1.shape.Shapes.Part.SIZE, -1);

	private static Item forme(String nom, dev.pyrofetch.jn1.shape.Shapes.Part part, int delta) {
		return register(nom, new dev.pyrofetch.jn1.shape.ShapePotionItem(part, delta, new Item.Settings().maxCount(16)));
	}

	// Les œufs d'apparition, pour la commodité : le modèle générique du jeu,
	// aux deux couleurs principales de chaque fille (fond, taches).
	// Les couleurs des œufs du mod d'origine (Jenny Mod 1.9.0, `EntityRegistry.registerModEntity`
	// dans `com.schnurritv.sexmod.r`) : violet sombre et rose pour Jenny, noir et rouge sang pour Ellie.
	public static final Item JENNY_SPAWN_EGG = register("jenny_spawn_egg",
			new net.minecraft.item.SpawnEggItem(ModEntities.JENNY, 0x322640, 0xC11A85, new Item.Settings()));
	public static final Item ELLIE_SPAWN_EGG = register("ellie_spawn_egg",
			new net.minecraft.item.SpawnEggItem(ModEntities.ELLIE, 0x161616, 0x980000, new Item.Settings()));
	// L'œuf opaque d'abord : le jeu ne retient qu'un œuf par créature (le dernier
	// déclaré), celui que donnent le clic molette et la boutique de Gaster. Ce
	// doit être l'œuf normal, l'hologramme.
	/** Aura pleine, sans hologramme : pour juger sa planche telle qu'elle est. */
	public static final Item AURA_OPAQUE_SPAWN_EGG = register("aura_opaque_spawn_egg",
			new net.minecraft.item.SpawnEggItem(ModEntities.AURA, 0x1C2A5C, 0xA8B4C8,
					new Item.Settings().component(net.minecraft.component.DataComponentTypes.ENTITY_DATA,
							donnéesAura(false))));
	/** Aura, éveillée : on la trouve en veille au laboratoire, l'œuf sert à l'avoir tout de suite. */
	public static final Item AURA_SPAWN_EGG = register("aura_spawn_egg",
			new net.minecraft.item.SpawnEggItem(ModEntities.AURA, 0x1C2A5C, 0x46DCF0,
					new Item.Settings().component(net.minecraft.component.DataComponentTypes.ENTITY_DATA,
							donnéesAura(true))));

	/** Gaster : son modèle lisse (la forme en cubes a été abandonnée en 0.56.1). */
	public static final Item GASTER_SPAWN_EGG = register("gaster_spawn_egg",
			new net.minecraft.item.SpawnEggItem(ModEntities.GASTER, 0x111114, 0xE8E8E8, new Item.Settings()));

	private static net.minecraft.component.type.NbtComponent donnéesAura(boolean hologramme) {
		net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
		nbt.putString("id", "jn1:aura");
		nbt.putBoolean("Dormant", false);
		nbt.putBoolean("Hologram", hologramme);
		return net.minecraft.component.type.NbtComponent.of(nbt);
	}

	private static Item tenue(ArmorItem.Type pièce) {
		return new ArmorItem(ModArmorMaterials.VOID_SUIT, pièce,
				new Item.Settings().maxDamage(pièce.getMaxDamage(15)));
	}

	private ModItems() {
	}

	/**
	 * L'icône de l'onglet créatif : la tête de Jenny, de face (0.81.3).
	 * Un objet sans usage, enregistré hors de la liste de l'onglet ; sa planche
	 * est un rendu de son modèle (`textures/item/tab_icon.png`, `art/`).
	 */
	public static final Item TAB_ICON = Registry.register(Registries.ITEM, JN1.id("tab_icon"),
			new Item(new Item.Settings()));

	private static Item register(String path, Item item) {
		Item registered = Registry.register(Registries.ITEM, JN1.id(path), item);
		ITEMS.add(registered);
		return registered;
	}

	private static void registerBlockItem(Block block) {
		String path = ModBlocks.idOf(block).getPath();
		BLOCK_ITEMS.add(Registry.register(Registries.ITEM, JN1.id(path),
				new BlockItem(block, new Item.Settings())));
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
