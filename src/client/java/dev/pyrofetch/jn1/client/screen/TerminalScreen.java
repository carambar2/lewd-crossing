package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.block.ComputerBlock;
import dev.pyrofetch.jn1.client.ClientProgress;
import dev.pyrofetch.jn1.terminal.Radio;
import net.minecraft.block.BlockState;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import dev.pyrofetch.jn1.client.terminal.TerminalSession;
import dev.pyrofetch.jn1.network.ShopPurchasePayload;
import dev.pyrofetch.jn1.network.TerminalActionPayload;
import dev.pyrofetch.jn1.shop.GirlEntry;
import dev.pyrofetch.jn1.shop.ShopEntry;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * L'écran du terminal : un système d'exploitation qui n'a jamais existé.
 *
 * <p>Tout est dessiné au rectangle, boutons compris. Ceux de Minecraft ont leur
 * propre texture, étirée et adoucie, qui jure à côté d'aplats nets et se lit
 * comme du flou. Rien ne se superpose non plus au texte : les lignes de balayage
 * restent sur le fond du bureau.
 *
 * <p>Les zones cliquables sont redéclarées à chaque image puis relues au clic,
 * plutôt que d'entretenir une arborescence de widgets pour une poignée de
 * boutons qui changent de place selon la fenêtre ouverte.
 */
public class TerminalScreen extends Screen {
	private static final int DESKTOP = 0xFF15282A;
	private static final int FACE = 0xFFC6C6C6;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADE = 0xFF5A5A5A;
	private static final int DEEP = 0xFF3A3A3A;
	private static final int TITLE_BAR = 0xFF8A1414;
	private static final int TITLE_TEXT = 0xFFF4E4E4;
	private static final int TEXT = 0xFF1A1A1A;
	private static final int TEXT_DIM = 0xFF6A6A6A;
	private static final int TASKBAR_HEIGHT = 20;

	private enum Window { NONE, SPAWN, SHOP, GIRLS, README, TRANSIT, RADIO, ONLYFAPS }

	// Chaque application a son dessin, tracé au pixel comme le reste du système
	// plutôt que chargé depuis une texture : à cette taille, une image
	// redimensionnée baverait.
	private static final String[] ICON_SPAWN = {
			"................",
			"......####......",
			".....######.....",
			".....######.....",
			"......####......",
			"...##########...",
			"..###.####.###..",
			".###..####..###.",
			"......####......",
			"......####......",
			"......####......",
			".....##..##.....",
			".....##..##.....",
			"....###..###....",
			"...###....###...",
			"................",
	};

	private static final String[] ICON_SHOP = {
			"................",
			".....######.....",
			"....##....##....",
			"....##....##....",
			"....##....##....",
			"................",
			".##############.",
			".##############.",
			".##..##..##..##.",
			".##..##..##..##.",
			".##..##..##..##.",
			".##..##..##..##.",
			".##..##..##..##.",
			"..############..",
			"...##########...",
			"................",
	};

	private static final String[] ICON_README = {
			"................",
			"..###########...",
			"..#.........##..",
			"..#..#####..#.#.",
			"..#.........#..#",
			"..#..#####..####",
			"..#.........#..#",
			"..#..#####..#..#",
			"..#.........#..#",
			"..#..####...#..#",
			"..#.........#..#",
			"..#..#..#...#..#",
			"..#.........#..#",
			"..###########..#",
			"................",
			"................",
	};

	private static final String[] ICON_GIRLS = {
			"................",
			"................",
			"................",
			"..##...##...##..",
			"..##...##...##..",
			"................",
			".####.####.####.",
			".####.####.####.",
			".####.####.####.",
			".####.####.####.",
			"................",
			"................",
			"................",
			"................",
			"................",
			"................",
	};

	/** Teintes des trois icônes, du plus clair au plus sombre. */
	private static final int[] ICON_SPAWN_TINTS = {0xFFBFE8E4, 0xFF5E9A96};
	private static final int[] ICON_SHOP_TINTS = {0xFF7BD98E, 0xFF2F7E45};
	private static final int[] ICON_GIRLS_TINTS = {0xFFE59AC0, 0xFF9A4A6E};
	private static final String[] ICON_TRANSIT = {
			"................",
			"...##########...",
			"...#........#...",
			"...#..####..#...",
			"...#........#...",
			"...#..####..#...",
			"...#........#...",
			"...#..#####.#...",
			"...#........#...",
			"...#...##...#...",
			"...#..####..#...",
			"...#...##...#...",
			"...#........#...",
			"...##########...",
			"................",
			"................",
	};

	private static final int[] ICON_README_TINTS = {0xFFC9B5E8, 0xFF6A4E9A};

	/** Un poste à antenne, cadran à gauche, haut-parleur à droite. */
	private static final String[] ICON_RADIO = {
			"................",
			"..........#.....",
			"...........#....",
			"............#...",
			"..#############.",
			"..#...........#.",
			"..#.####..#.#.#.",
			"..#.#..#..#.#.#.",
			"..#.####..#.#.#.",
			"..#.......#.#.#.",
			"..#.##.##.....#.",
			"..#############.",
			"...#.........#..",
			"................",
			"................",
			"................",
	};
	private static final int[] ICON_RADIO_TINTS = {0xFFF0D890, 0xFF9A7A2E};
	private static final int[] ICON_TRANSIT_TINTS = {0xFFE8C9B5, 0xFF9A6A4E};

	/** Un cœur traversé d'un objectif : OnlyFaps, où l'on publie ses photos. */
	private static final String[] ICON_ONLYFAPS = {
			"................",
			"...###....###...",
			"..#####..#####..",
			".##############.",
			".#####....#####.",
			".####.####.####.",
			".###.##..##.###.",
			".###.#....#.###.",
			"..##.##..##.##..",
			"...##.####.##...",
			"....###..###....",
			".....######.....",
			"......####......",
			".......##.......",
			"................",
			"................",
	};
	private static final int[] ICON_ONLYFAPS_TINTS = {0xFFF59AC8, 0xFF9A2E64};
	private static final int ONLYFAPS_ROSE = 0xFFE0559A;
	/** Lignes de photos par page d'OnlyFaps.exe. */
	private static final int ONLYFAPS_LIGNES = 4;
	private int onlyFapsPage;

	/** Une zone cliquable, redéclarée à chaque image. */
	private record Hotspot(int x, int y, int width, int height, Runnable action, boolean enabled) {
		boolean contains(double mx, double my) {
			return mx >= this.x && mx < this.x + this.width
					&& my >= this.y && my < this.y + this.height;
		}
	}

	private final BlockPos terminal;
	private final List<Hotspot> hotspots = new ArrayList<>();
	private Window window = Window.NONE;
	private boolean startMenu;
	private int animationTicks;

	// --- La grille de la boutique ---
	private static final int TILE_WIDTH = 74;
	private static final int TILE_HEIGHT = 74;
	private static final int TILE_GAP = 6;

	// --- Le présentoir des compagnes ---
	private static final int CARD_WIDTH = 88;
	private static final int CARD_HEIGHT = 132;
	private static final int CARD_GAP = 8;
	/** Hauteur du bouton d'achat, sous chaque carte. */
	private static final int BUY_HEIGHT = 18;
	/** Piste de défilement, en haut du présentoir. */
	private static final int TRACK_HEIGHT = 11;

	private int scroll;
	private boolean draggingTrack;
	/**
	 * Les modèles présentés, construits une fois.
	 *
	 * <p>Ces entités ne sont jamais ajoutées au monde : elles n'existent que
	 * pour poser devant la caméra du présentoir. En reconstruire une par image
	 * coûterait un modèle entier par image.
	 */
	private final java.util.Map<String, LivingEntity> previews = new java.util.HashMap<>();
	/** Géométrie de la piste, relue au clic : x, y, largeur, curseur, débord. */
	private int[] trackBounds;

	private boolean quitté;

	public TerminalScreen(BlockPos terminal) {
		super(Text.translatable("screen.jn1.terminal.title"));
		this.terminal = terminal;
	}

	@Override
	public void tick() {
		super.tick();
		this.animationTicks++;
	}

	// ------------------------------------------------------------------
	// Rendu
	// ------------------------------------------------------------------

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.hotspots.clear();

		context.fill(0, 0, this.width, this.height, DESKTOP);
		// Sous le contenu : par-dessus, elles hachent le texte et tout paraît flou.
		for (int y = 0; y < this.height; y += 4) {
			context.fill(0, y, this.width, y + 1, 0x0E000000);
		}

		this.renderDesktopIcons(context);
		switch (this.window) {
			case SPAWN -> this.renderSpawnWindow(context, mouseX, mouseY);
			case SHOP -> this.renderShopWindow(context, mouseX, mouseY);
			case GIRLS -> this.renderGirlsWindow(context, mouseX, mouseY);
			case README -> this.renderReadmeWindow(context, mouseX, mouseY);
			case TRANSIT -> this.renderTransitWindow(context, mouseX, mouseY);
			case RADIO -> this.renderRadioWindow(context, mouseX, mouseY);
			case ONLYFAPS -> this.renderOnlyFapsWindow(context, mouseX, mouseY);
			case NONE -> { }
		}
		this.renderTaskbar(context, mouseX, mouseY);
		if (this.startMenu) {
			this.renderStartMenu(context, mouseX, mouseY);
		}
		this.renderVignette(context);
	}

	private void renderDesktopIcons(DrawContext context) {
		this.icon(context, 34, 16, "screen.jn1.terminal.window",
				ICON_SPAWN, ICON_SPAWN_TINTS, () -> this.window = Window.SPAWN);
		this.icon(context, 34, 68, "screen.jn1.shop.title",
				ICON_SHOP, ICON_SHOP_TINTS, () -> this.window = Window.SHOP);
		this.icon(context, 34, 120, "screen.jn1.girls.title",
				ICON_GIRLS, ICON_GIRLS_TINTS, this::openGirls);
		this.icon(context, 34, 172, "screen.jn1.readme.title",
				ICON_README, ICON_README_TINTS, () -> this.window = Window.README);
		this.icon(context, 110, 16, "screen.jn1.transit.title",
				ICON_TRANSIT, ICON_TRANSIT_TINTS, () -> this.window = Window.TRANSIT);
		this.icon(context, 110, 68, "screen.jn1.radio.title",
				ICON_RADIO, ICON_RADIO_TINTS, () -> this.window = Window.RADIO);
		this.icon(context, 110, 120, "screen.jn1.onlyfaps.title",
				ICON_ONLYFAPS, ICON_ONLYFAPS_TINTS, this::openOnlyFaps);
	}

	private void icon(DrawContext context, int x, int y, String key,
	                  String[] sprite, int[] tints, Runnable action) {
		context.fill(x, y, x + 32, y + 32, 0x22FFFFFF);
		this.sprite(context, x, y, sprite, tints);

		Text label = Text.translatable(key);
		int textX = Math.max(2, x + 16 - this.textRenderer.getWidth(label) / 2);
		context.drawText(this.textRenderer, label, textX, y + 35, 0xFFD8D8D8, false);
		this.hotspots.add(new Hotspot(Math.min(x, textX), y,
				Math.max(32, this.textRenderer.getWidth(label)), 46, action, true));
	}

	/**
	 * Trace un dessin de seize pixels à l'échelle deux.
	 *
	 * <p>La teinte sombre est posée un pixel plus bas et à droite : c'est
	 * l'ombre portée, qui détache le dessin du bureau sans avoir à le cerner.
	 */
	private void sprite(DrawContext context, int x, int y, String[] rows, int[] tints) {
		for (int pass = tints.length - 1; pass >= 0; pass--) {
			int offset = pass;
			for (int row = 0; row < rows.length; row++) {
				String line = rows[row];
				for (int column = 0; column < line.length(); column++) {
					if (line.charAt(column) == '.') {
						continue;
					}
					int px = x + column * 2 + offset;
					int py = y + row * 2 + offset;
					context.fill(px, py, px + 2, py + 2, tints[pass]);
				}
			}
		}
	}

	/**
	 * spawn.exe : une ligne par compagne, celles qu'on n'a pas encore restant
	 * visibles mais inertes — leur existence est le seul indice qu'il en reste
	 * à acquérir.
	 */
	private void renderSpawnWindow(DrawContext context, int mouseX, int mouseY) {
		List<GirlEntry> catalogue = GirlEntry.catalogue();
		int w = 240;
		int h = 52 + catalogue.size() * 24;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;

		this.frame(context, left, top, w, h, "screen.jn1.terminal.window");
		context.drawText(this.textRenderer, Text.translatable("screen.jn1.terminal.body"),
				left + 12, top + 24, TEXT, false);

		int row = top + 40;
		for (GirlEntry girl : catalogue) {
			boolean unlocked = ClientProgress.hasFlag(girl.unlockFlag());
			context.drawText(this.textRenderer, Text.translatable(girl.translationKey()),
					left + 12, row + 6, unlocked ? TEXT : TEXT_DIM, false);
			Text label = Text.translatable(unlocked
					? "screen.jn1.terminal.spawn"
					: "screen.jn1.terminal.need_unlock");
			this.button(context, left + w - 118, row, 106, 20, label,
					unlocked, () -> this.spawn(girl), mouseX, mouseY);
			row += 24;
		}
	}

	/**
	 * Shop.exe : une grille de vignettes plutôt qu'une liste.
	 *
	 * <p>Les lignes se ressemblaient toutes et l'objet vendu se réduisait à une
	 * icône de seize pixels perdue dans la marge. Chaque article a maintenant sa
	 * case, avec son objet au centre, et le prix se lit en rouge dès qu'il passe
	 * hors de portée de la bourse.
	 */
	private void renderShopWindow(DrawContext context, int mouseX, int mouseY) {
		// Ce que vend ce terminal-ci : la matière noire n'apparaît que sur ceux
		// du vide.
		List<ShopEntry> catalogue = this.client == null || this.client.world == null
				? List.of()
				: ShopEntry.catalogue(ShopEntry.Guichet.TERMINAL, this.client.world);
		int columns = catalogue.size() > 6 ? 4 : 3;
		int rows = (catalogue.size() + columns - 1) / columns;

		int w = 20 + columns * TILE_WIDTH + (columns - 1) * TILE_GAP;
		int h = 44 + rows * TILE_HEIGHT + (rows - 1) * TILE_GAP + 8;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;
		int wallet = this.countCurrency();

		this.frame(context, left, top, w, h, "screen.jn1.shop.title");

		// La bourse en évidence : c'est elle qui décide de ce qui est cliquable.
		int walletY = top + 21;
		context.fill(left + 8, walletY, left + w - 8, walletY + 14, DEEP);
		context.drawItem(new ItemStack(ShopEntry.CURRENCY), left + 10, walletY - 1);
		context.drawText(this.textRenderer, Text.translatable("screen.jn1.shop.wallet", wallet),
				left + 30, walletY + 3, 0xFFE0E0E0, false);

		int gridTop = walletY + 22;
		for (int index = 0; index < catalogue.size(); index++) {
			ShopEntry entry = catalogue.get(index);
			int column = index % columns;
			int row = index / columns;
			int x = left + 10 + column * (TILE_WIDTH + TILE_GAP);
			int y = gridTop + row * (TILE_HEIGHT + TILE_GAP);
			this.renderShopTile(context, entry, x, y, wallet, mouseX, mouseY);
		}
	}

	private void renderShopTile(DrawContext context, ShopEntry entry, int x, int y,
	                            int wallet, int mouseX, int mouseY) {
		boolean owned = entry.isUnlock() && ClientProgress.hasFlag(entry.unlockFlag());
		boolean affordable = wallet >= entry.price();
		boolean hover = mouseX >= x && mouseX < x + TILE_WIDTH
				&& mouseY >= y && mouseY < y + TILE_HEIGHT;

		context.fill(x, y, x + TILE_WIDTH, y + TILE_HEIGHT, hover ? 0xFF32323C : 0xFF2A2A32);
		context.fill(x, y, x + TILE_WIDTH, y + 1, SHADE);
		context.fill(x, y, x + 1, y + TILE_HEIGHT, SHADE);
		context.fill(x + TILE_WIDTH - 1, y, x + TILE_WIDTH, y + TILE_HEIGHT, LIGHT);
		context.fill(x, y + TILE_HEIGHT - 1, x + TILE_WIDTH, y + TILE_HEIGHT, LIGHT);

		// L'objet, en grand et centré : c'est lui qu'on vient chercher.
		context.drawItem(entry.stack(), x + TILE_WIDTH / 2 - 8, y + 6);
		context.drawItemInSlot(this.textRenderer, entry.stack(),
				x + TILE_WIDTH / 2 - 8, y + 6);

		Text name = Text.translatable(entry.translationKey());
		this.texteAjusté(context, name, x + TILE_WIDTH / 2, y + 27, TILE_WIDTH - 4, 0xFFDCDCDC);

		Text price = Text.translatable("screen.jn1.shop.price", entry.price());
		int priceX = x + (TILE_WIDTH - this.textRenderer.getWidth(price)) / 2;
		context.drawText(this.textRenderer, price, priceX, y + 38,
				owned || affordable ? 0xFF9AD8A2 : 0xFFC06060, false);

		Text label = Text.translatable(owned ? "screen.jn1.shop.owned" : "screen.jn1.shop.buy");
		this.button(context, x + 4, y + TILE_HEIGHT - 20, TILE_WIDTH - 8, 16, label,
				!owned && affordable, () -> this.buy(entry), mouseX, mouseY);
	}

	/** Centré, et réduit s'il ne tient pas dans la largeur : un nom long débordait de sa vignette. */
	private void texteAjusté(DrawContext context, Text texte, int centreX, int y, int largeurMax, int couleur) {
		int largeur = this.textRenderer.getWidth(texte);
		if (largeur <= largeurMax) {
			context.drawText(this.textRenderer, texte, centreX - largeur / 2, y, couleur, false);
			return;
		}
		float échelle = (float) largeurMax / largeur;
		context.getMatrices().push();
		context.getMatrices().translate(centreX - largeurMax / 2.0F, y + (1.0F - échelle) * 4.0F, 0.0F);
		context.getMatrices().scale(échelle, échelle, 1.0F);
		context.drawText(this.textRenderer, texte, 0, 0, couleur, false);
		context.getMatrices().pop();
	}

	private void openGirls() {
		this.window = Window.GIRLS;
		this.scroll = 0;
	}

	/** Largeur qu'occupent toutes les cartes bout à bout. */
	private int girlsContentWidth() {
		int count = GirlEntry.catalogue().size();
		return count * CARD_WIDTH + Math.max(0, count - 1) * CARD_GAP;
	}

	/**
	 * Le présentoir : une carte par compagne, faites défiler à l'horizontale.
	 *
	 * <p>Chaque carte montre le modèle en perspective plutôt qu'une icône : c'est
	 * ce qu'on achète, et une tête d'objet ne dirait rien de la silhouette.
	 */
	private void renderGirlsWindow(DrawContext context, int mouseX, int mouseY) {
		List<GirlEntry> catalogue = GirlEntry.catalogue();
		int visible = Math.min(3, Math.max(1, catalogue.size()));
		int viewWidth = visible * CARD_WIDTH + (visible - 1) * CARD_GAP;

		int w = viewWidth + 20;
		int h = 22 + TRACK_HEIGHT + 6 + CARD_HEIGHT + 6 + BUY_HEIGHT + 10;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;

		this.frame(context, left, top, w, h, "screen.jn1.girls.title");

		int viewX = left + 10;
		int trackY = top + 22;
		int cardsY = trackY + TRACK_HEIGHT + 6;

		int overflow = Math.max(0, this.girlsContentWidth() - viewWidth);
		this.scroll = Math.clamp(this.scroll, 0, overflow);
		this.renderTrack(context, viewX, trackY, viewWidth, overflow);

		// Hors de la fenêtre, les cartes ne doivent pas déborder sur le bureau.
		context.enableScissor(viewX, cardsY, viewX + viewWidth, cardsY + CARD_HEIGHT + 6 + BUY_HEIGHT);
		int wallet = this.countCurrency();
		for (int index = 0; index < catalogue.size(); index++) {
			int cardX = viewX - this.scroll + index * (CARD_WIDTH + CARD_GAP);
			if (cardX + CARD_WIDTH < viewX || cardX > viewX + viewWidth) {
				continue;
			}
			this.renderGirlCard(context, catalogue.get(index), cardX, cardsY,
					viewX, viewX + viewWidth, wallet, mouseX, mouseY);
		}
		context.disableScissor();
	}

	/** Une carte : le modèle, le nom, le prix, et le bouton dessous. */
	private void renderGirlCard(DrawContext context, GirlEntry girl, int x, int y,
	                            int clipLeft, int clipRight, int wallet, int mouseX, int mouseY) {
		context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF2A2A32);
		context.fill(x, y, x + CARD_WIDTH, y + 1, SHADE);
		context.fill(x, y, x + 1, y + CARD_HEIGHT, SHADE);
		context.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, LIGHT);
		context.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, LIGHT);

		this.renderGirlModel(context, girl, x, y);

		Text name = Text.translatable(girl.translationKey());
		context.drawText(this.textRenderer, name,
				x + (CARD_WIDTH - this.textRenderer.getWidth(name)) / 2,
				y + CARD_HEIGHT - 24, 0xFFE8E8E8, false);

		boolean owned = ClientProgress.hasFlag(girl.unlockFlag());
		boolean affordable = wallet >= girl.price();
		Text price = Text.translatable("screen.jn1.shop.price", girl.price());
		context.drawText(this.textRenderer, price,
				x + (CARD_WIDTH - this.textRenderer.getWidth(price)) / 2,
				y + CARD_HEIGHT - 13, owned || affordable ? 0xFFB8B8B8 : 0xFFC06060, false);

		// Un bouton à cheval sur le bord serait cliquable là où on ne le voit
		// plus : sous la découpe, il ne compte pas.
		boolean reachable = x >= clipLeft && x + CARD_WIDTH <= clipRight;
		Text label = Text.translatable(owned ? "screen.jn1.girls.owned" : "screen.jn1.girls.buy");
		this.button(context, x, y + CARD_HEIGHT + 6, CARD_WIDTH, BUY_HEIGHT, label,
				reachable && !owned && affordable, () -> this.buyGirl(girl), mouseX, mouseY);
	}

	/**
	 * Le modèle, vu de trois quarts.
	 *
	 * <p>{@code drawEntity} déduit l'orientation d'une position de souris ; on
	 * lui en fournit une fictive, calculée pour donner l'angle voulu, ce qui
	 * fige la pose au lieu de la faire suivre le curseur.
	 */
	private void renderGirlModel(DrawContext context, GirlEntry girl, int x, int y) {
		LivingEntity entity = this.preview(girl);
		if (entity == null) {
			return;
		}
		int boxTop = y + 6;
		int boxBottom = y + CARD_HEIGHT - 28;
		float centreX = x + CARD_WIDTH / 2.0F;
		float centreY = (boxTop + boxBottom) / 2.0F;

		// tan(35°) et tan(12°) : trois quarts, vu très légèrement de haut.
		float fakeMouseX = centreX - 40.0F * 0.70F;
		float fakeMouseY = centreY - 40.0F * 0.21F;

		int size = (int) ((boxBottom - boxTop) / 2 * girl.previewScale());
		// Par Portrait, pas par drawEntity : dessinée à l'instant 1,0, l'aperçu
		// tournait la tête jusqu'à la butée, de dos (vu en jeu, 0.56.7 ; le
		// piège de 0.53.3). Hors du monde, l'aperçu ne bat pas : on lui donne
		// l'âge du joueur pour que ses animations coulent.
		if (this.client != null && this.client.player != null) {
			entity.age = this.client.player.age;
		}
		Portrait.dessiner(context, x + 4, boxTop, x + CARD_WIDTH - 4, boxBottom,
				size, 0.0625F, fakeMouseX, fakeMouseY, entity);
	}

	private LivingEntity preview(GirlEntry girl) {
		return this.previews.computeIfAbsent(girl.id(), id -> {
			if (this.client == null || this.client.world == null) {
				return null;
			}
			return girl.type().create(this.client.world);
		});
	}

	/** La piste de défilement, en relief creux comme le reste du système. */
	private void renderTrack(DrawContext context, int x, int y, int w, int overflow) {
		context.fill(x, y, x + w, y + TRACK_HEIGHT, DEEP);
		context.fill(x, y, x + w, y + 1, SHADE);
		context.fill(x, y, x + 1, y + TRACK_HEIGHT, SHADE);

		int thumbWidth = overflow == 0
				? w
				: Math.max(24, (int) (w * (w / (float) this.girlsContentWidth())));
		int travel = w - thumbWidth;
		int thumbX = overflow == 0 ? x : x + (int) (travel * (this.scroll / (float) overflow));

		context.fill(thumbX, y + 1, thumbX + thumbWidth, y + TRACK_HEIGHT, FACE);
		context.fill(thumbX, y + 1, thumbX + thumbWidth, y + 2, LIGHT);
		context.fill(thumbX + thumbWidth - 1, y + 1, thumbX + thumbWidth, y + TRACK_HEIGHT, SHADE);

		if (overflow > 0) {
			this.trackBounds = new int[] { x, y, w, thumbWidth, overflow };
		} else {
			this.trackBounds = null;
		}
	}

	private void buyGirl(GirlEntry girl) {
		ClientPlayNetworking.send(new ShopPurchasePayload(this.terminal, girl.id()));
	}

	/**
	 * flux.txt : ce que la machine prétend faire.
	 *
	 * <p>C'est le seul endroit du mod où la mécanique est expliquée en toutes
	 * lettres, et il faut aller le chercher : la cave le raconte déjà en blocs,
	 * ce fichier ne fait que confirmer ce qu'on y a vu.
	 */
	private void renderReadmeWindow(DrawContext context, int mouseX, int mouseY) {
		int w = 300;
		int h = 196;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;

		this.frame(context, left, top, w, h, "screen.jn1.readme.title");

		// Fond de page : du texte sur fond clair, comme un éditeur de l'époque.
		context.fill(left + 8, top + 21, left + w - 8, top + h - 8, 0xFF10161C);
		context.fill(left + 8, top + 21, left + w - 8, top + 22, DEEP);

		int y = top + 28;
		for (int line = 0; line < README_LINES; line++) {
			Text text = Text.translatable("screen.jn1.readme.line" + line);
			// Les titres de section sont préfixés d'un chevron dans la langue,
			// on les teinte sans avoir à tenir une liste séparée.
			String raw = text.getString();
			int colour = raw.startsWith(">") ? 0xFF9AD8C8 : 0xFFB8C4CC;
			context.drawText(this.textRenderer, text, left + 16, y, colour, false);
			y += 11;
		}
	}

	private static final int README_LINES = 13;

	/**
	 * transit.txt : pourquoi l'on ne ramène pas une compagne à la main.
	 *
	 * <p>Le même rendu que flux.txt, à la clé de traduction près. La règle
	 * qu'il énonce est celle que le jeu applique déjà — sans capsule, rien ne
	 * traverse — mais rien ne la disait, et le joueur ne pouvait que la
	 * découvrir en butant dessus.
	 */
	private void renderTransitWindow(DrawContext context, int mouseX, int mouseY) {
		int w = 300;
		int h = 196;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;

		this.frame(context, left, top, w, h, "screen.jn1.transit.title");
		context.fill(left + 8, top + 21, left + w - 8, top + h - 8, 0xFF10161C);
		context.fill(left + 8, top + 21, left + w - 8, top + 22, DEEP);

		int y = top + 28;
		for (int line = 0; line < TRANSIT_LINES; line++) {
			Text text = Text.translatable("screen.jn1.transit.line" + line);
			String raw = text.getString();
			int colour = raw.startsWith(">") ? 0xFF9AD8C8 : 0xFFB8C4CC;
			context.drawText(this.textRenderer, text, left + 16, y, colour, false);
			y += 11;
		}
	}

	private static final int TRANSIT_LINES = 14;

	/**
	 * Radio.exe : une ligne par station, et un bouton pour couper.
	 *
	 * <p>La station en cours s'agite d'un petit égaliseur. Ce qu'on choisit ici
	 * joue depuis la machine, pour toute la pièce, et continue quand on quitte
	 * l'écran : c'est le terminal qui est allumé, pas la fenêtre.
	 *
	 * <p>La dernière station a un nom qu'on ne lit pas : il est écrit dans
	 * l'alphabet de la table d'enchantement.
	 */
	private void renderRadioWindow(DrawContext context, int mouseX, int mouseY) {
		List<Radio.Station> stations = Radio.STATIONS;
		int w = 220;
		int h = 52 + stations.size() * 24 + 26;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;
		this.frame(context, left, top, w, h, "screen.jn1.radio.title");

		int enCours = this.stationEnCours();
		context.drawText(this.textRenderer, Text.translatable("screen.jn1.radio.body"),
				left + 12, top + 24, TEXT, false);

		int row = top + 40;
		for (int index = 0; index < stations.size(); index++) {
			Radio.Station station = stations.get(index);
			int numéro = index + 1;
			boolean joue = enCours == numéro;
			context.fill(left + 10, row, left + w - 10, row + 20, joue ? 0xFF2A2A32 : 0xFFB8B8B8);
			Text nom = Text.translatable(station.translationKey());
			if ("void".equals(station.id())) {
				nom = nom.copy().setStyle(Style.EMPTY.withFont(Identifier.ofVanilla("alt")));
			}
			context.drawText(this.textRenderer, nom, left + 16, row + 6, joue ? 0xFFE8E8E8 : TEXT, false);
			if (joue) {
				this.égaliseur(context, left + w - 96, row + 4);
			}
			this.button(context, left + w - 64, row + 2, 50, 16,
					Text.translatable(joue ? "screen.jn1.radio.playing" : "screen.jn1.radio.play"),
					!joue, () -> this.régler(numéro), mouseX, mouseY);
			row += 24;
		}
		this.button(context, left + w / 2 - 40, row + 2, 80, 18, Text.translatable("screen.jn1.radio.stop"),
				enCours != 0, () -> this.régler(0), mouseX, mouseY);
	}

	/** Cinq barres qui montent et descendent : juste de quoi dire que ça joue. */
	private void égaliseur(DrawContext context, int x, int y) {
		for (int barre = 0; barre < 5; barre++) {
			float phase = this.animationTicks * 0.35F + barre * 1.7F;
			int hauteur = 3 + (int) ((MathHelper.sin(phase) * 0.5F + 0.5F) * 9.0F);
			context.fill(x + barre * 4, y + 12 - hauteur, x + barre * 4 + 3, y + 12, 0xFF6CF08A);
		}
	}

	/** La station lue sur la machine elle-même : c'est elle qui fait foi. */
	private int stationEnCours() {
		if (this.client == null || this.client.world == null) {
			return 0;
		}
		BlockState état = this.client.world.getBlockState(this.terminal);
		return état.getBlock() instanceof ComputerBlock ? état.get(ComputerBlock.STATION) : 0;
	}

	private void régler(int station) {
		ClientPlayNetworking.send(new TerminalActionPayload(
				this.terminal, TerminalActionPayload.ACTION_RADIO, String.valueOf(station)));
	}

	private void openOnlyFaps() {
		this.window = Window.ONLYFAPS;
		this.onlyFapsPage = 0;
		dev.pyrofetch.jn1.client.onlyfaps.OnlyFapsClient.oublier();
		this.demanderOnlyFaps(dev.pyrofetch.jn1.network.OnlyFapsPayload.LISTE, 0);
	}

	private void demanderOnlyFaps(int action, int numéro) {
		ClientPlayNetworking.send(new dev.pyrofetch.jn1.network.OnlyFapsPayload(this.terminal, action, numéro));
	}

	/**
	 * OnlyFaps.exe : les photos de l'inventaire, chacune avec ce qu'elle
	 * rapporterait, et un bouton pour la publier. Tout ce qui est chiffré vient
	 * du serveur ; la fenêtre ne fait que montrer.
	 */
	private void renderOnlyFapsWindow(DrawContext context, int mouseX, int mouseY) {
		var état = dev.pyrofetch.jn1.client.onlyfaps.OnlyFapsClient.état();
		int w = 300;
		int ligne = 34;
		int h = 64 + ONLYFAPS_LIGNES * ligne + 40;
		int left = (this.width - w) / 2;
		int top = (this.height - h - TASKBAR_HEIGHT) / 2;
		this.frame(context, left, top, w, h, "screen.jn1.onlyfaps.title");

		// L'en-tête : le nom en rose, les abonnés, la bourse.
		int y = top + 22;
		context.fill(left + 8, y, left + w - 8, y + 18, DEEP);
		context.drawText(this.textRenderer, Text.literal("OnlyFaps").formatted(net.minecraft.util.Formatting.BOLD),
				left + 13, y + 5, ONLYFAPS_ROSE, false);
		Text abonnés = Text.translatable("screen.jn1.onlyfaps.subscribers", état == null ? "…" : état.abonnés());
		context.drawText(this.textRenderer, abonnés, left + 72, y + 5, 0xFFE0E0E0, false);
		context.drawItem(new ItemStack(ShopEntry.CURRENCY), left + w - 50, y + 1);
		context.drawText(this.textRenderer, String.valueOf(this.countCurrency()), left + w - 32, y + 5, 0xFF9AD8A2, false);

		int listeTop = y + 24;
		if (état == null) {
			context.drawText(this.textRenderer, Text.translatable("screen.jn1.onlyfaps.loading"),
					left + 14, listeTop + 6, TEXT_DIM, false);
		} else if (état.offres().isEmpty()) {
			context.drawText(this.textRenderer, Text.translatable("screen.jn1.onlyfaps.empty"),
					left + 14, listeTop + 6, TEXT_DIM, false);
		} else {
			var offres = état.offres();
			int pages = (offres.size() + ONLYFAPS_LIGNES - 1) / ONLYFAPS_LIGNES;
			this.onlyFapsPage = Math.min(this.onlyFapsPage, pages - 1);
			for (int i = 0; i < ONLYFAPS_LIGNES; i++) {
				int index = this.onlyFapsPage * ONLYFAPS_LIGNES + i;
				if (index >= offres.size()) {
					break;
				}
				this.renderOffre(context, offres.get(index), left + 8, listeTop + i * ligne, w - 16, ligne - 4, mouseX, mouseY);
			}
			if (pages > 1) {
				int py = listeTop + ONLYFAPS_LIGNES * ligne;
				this.button(context, left + 8, py, 16, 12, Text.literal("<"), this.onlyFapsPage > 0,
						() -> this.onlyFapsPage--, mouseX, mouseY);
				context.drawText(this.textRenderer, (this.onlyFapsPage + 1) + "/" + pages, left + 30, py + 2, TEXT, false);
				this.button(context, left + 52, py, 16, 12, Text.literal(">"), this.onlyFapsPage < pages - 1,
						() -> this.onlyFapsPage++, mouseX, mouseY);
			}
		}

		// Le dernier commentaire d'un abonné, et ce que la publication a rapporté.
		String commentaire = dev.pyrofetch.jn1.client.onlyfaps.OnlyFapsClient.commentaire();
		int by = top + h - 26;
		context.fill(left + 8, by, left + w - 8, by + 18, 0xFFE8E0E4);
		if (!commentaire.isEmpty()) {
			int gain = dev.pyrofetch.jn1.client.onlyfaps.OnlyFapsClient.gain();
			Text texte = Text.translatable("screen.jn1.onlyfaps.comment", Text.translatable(commentaire));
			context.drawText(this.textRenderer, this.textRenderer.trimToWidth(texte, w - (gain > 0 ? 64 : 24)).getString(),
					left + 12, by + 5, 0xFF7A1E50, false);
			if (gain > 0) {
				context.drawText(this.textRenderer, "+" + gain, left + w - 44, by + 5, 0xFF2E8A3A, false);
				context.drawItem(new ItemStack(ShopEntry.CURRENCY), left + w - 28, by + 1);
			}
		}
	}

	private void renderOffre(DrawContext context, dev.pyrofetch.jn1.onlyfaps.Offre offre, int x, int y, int w, int h,
	                         int mouseX, int mouseY) {
		context.fill(x, y, x + w, y + h, 0xFF2A2A32);
		// La vignette, à son format, dans une case de 40 × 26.
		var format = offre.format();
		float échelle = Math.min(40f / format.largeurEnPixels(), 26f / format.hauteurEnPixels());
		int tw = Math.max(1, Math.round(format.largeurEnPixels() * échelle));
		int th = Math.max(1, Math.round(format.hauteurEnPixels() * échelle));
		int tx = x + 2 + (40 - tw) / 2;
		int ty = y + 2 + (26 - th) / 2;
		Identifier texture = dev.pyrofetch.jn1.client.photo.PhotoTextures.get(offre.numéro());
		if (texture != null) {
			context.drawTexture(texture, tx, ty, tw, th, 0f, 0f, format.largeurEnPixels(), format.hauteurEnPixels(),
					format.largeurEnPixels(), format.hauteurEnPixels());
		} else {
			context.fill(tx, ty, tx + tw, ty + th, 0xFF3A3A44);
		}

		// Ce qu'il y a dessus, en deux lignes.
		Text titre;
		Text détail;
		if (offre.ancienne()) {
			titre = Text.translatable("screen.jn1.onlyfaps.photo", offre.numéro());
			détail = Text.translatable("screen.jn1.onlyfaps.old");
		} else if (offre.sujets().isEmpty()) {
			titre = Text.translatable("screen.jn1.onlyfaps.photo", offre.numéro());
			détail = Text.translatable("screen.jn1.onlyfaps.nobody");
		} else {
			var sujet = offre.sujets().get(0);
			titre = Text.translatable("girl.jn1." + sujet.fille());
			if (offre.sujets().size() > 1) {
				titre = Text.translatable("screen.jn1.onlyfaps.and_more", titre, offre.sujets().size() - 1);
			}
			détail = sujet.enScène()
					? Text.translatableWithFallback("scene.jn1." + sujet.scène(), sujet.scène())
					: Text.translatableWithFallback("outfit.jn1." + sujet.tenue(), sujet.tenue());
			if (sujet.traces() > 0) {
				détail = Text.translatable("screen.jn1.onlyfaps.with_traces", détail);
			}
			if (sujet.menottée()) {
				détail = Text.translatable("screen.jn1.onlyfaps.cuffed", détail);
			}
		}
		context.drawText(this.textRenderer, titre, x + 48, y + 5, 0xFFE8E8E8, false);
		context.drawText(this.textRenderer, this.textRenderer.trimToWidth(détail, w - 150).getString(),
				x + 48, y + 17, 0xFFA8A8B0, false);

		// Le prix et le bouton.
		boolean publiable = !offre.publiée() && !offre.ancienne();
		if (publiable && offre.prix() > 0) {
			context.drawText(this.textRenderer, String.valueOf(offre.prix()), x + w - 98, y + 11, 0xFF9AD8A2, false);
			context.drawItem(new ItemStack(ShopEntry.CURRENCY), x + w - 84, y + 7);
		}
		Text libellé = Text.translatable(offre.publiée() ? "screen.jn1.onlyfaps.published" : "screen.jn1.onlyfaps.publish");
		this.button(context, x + w - 62, y + 7, 58, 16, libellé, publiable,
				() -> this.demanderOnlyFaps(dev.pyrofetch.jn1.network.OnlyFapsPayload.PUBLIER, offre.numéro()),
				mouseX, mouseY);
	}

	private void renderTaskbar(DrawContext context, int mouseX, int mouseY) {
		int top = this.height - TASKBAR_HEIGHT;
		context.fill(0, top, this.width, this.height, FACE);
		context.fill(0, top, this.width, top + 1, LIGHT);

		this.button(context, 4, top + 3, 58, 14, Text.translatable("screen.jn1.terminal.start"),
				true, () -> this.startMenu = !this.startMenu, mouseX, mouseY);

		String clock = this.worldClock();
		context.drawText(this.textRenderer, clock, this.width - 34, top + 6, TEXT, false);
	}

	/**
	 * L'heure du monde, pas celle de la machine.
	 *
	 * <p>Minecraft compte 24000 ticks par jour et place minuit à 18000 : on
	 * décale avant de convertir, sinon l'écran afficherait six heures de retard.
	 */
	private String worldClock() {
		if (this.client == null || this.client.world == null) {
			return "--:--";
		}
		long time = Math.floorMod(this.client.world.getTimeOfDay() + 6000L, 24000L);
		return String.format("%02d:%02d", time / 1000L, (time % 1000L) * 60L / 1000L);
	}

	private void renderStartMenu(DrawContext context, int mouseX, int mouseY) {
		int w = 100;
		// Sept entrées et le séparateur : seize pixels par entrée ajoutée.
		int h = 142;
		int left = 4;
		int top = this.height - TASKBAR_HEIGHT - h;

		context.fill(left, top, left + w, top + h, FACE);
		context.fill(left, top, left + w, top + 1, LIGHT);
		context.fill(left, top, left + 1, top + h, LIGHT);
		context.fill(left + w - 1, top, left + w, top + h, SHADE);

		this.menuItem(context, left + 3, top + 4, w - 6, "screen.jn1.terminal.window",
				() -> { this.window = Window.SPAWN; this.startMenu = false; }, mouseX, mouseY);
		this.menuItem(context, left + 3, top + 20, w - 6, "screen.jn1.shop.title",
				() -> { this.window = Window.SHOP; this.startMenu = false; }, mouseX, mouseY);
		this.menuItem(context, left + 3, top + 36, w - 6, "screen.jn1.girls.title",
				() -> { this.openGirls(); this.startMenu = false; }, mouseX, mouseY);
		this.menuItem(context, left + 3, top + 52, w - 6, "screen.jn1.readme.title",
				() -> { this.window = Window.README; this.startMenu = false; }, mouseX, mouseY);
		this.menuItem(context, left + 3, top + 68, w - 6, "screen.jn1.transit.title",
				() -> { this.window = Window.TRANSIT; this.startMenu = false; }, mouseX, mouseY);
		this.menuItem(context, left + 3, top + 84, w - 6, "screen.jn1.radio.title",
				() -> { this.window = Window.RADIO; this.startMenu = false; }, mouseX, mouseY);
		this.menuItem(context, left + 3, top + 100, w - 6, "screen.jn1.onlyfaps.title",
				() -> { this.openOnlyFaps(); this.startMenu = false; }, mouseX, mouseY);
		context.fill(left + 5, top + 118, left + w - 5, top + 119, SHADE);
		// C'est ici qu'on quitte : un système ne se ferme pas par un bouton
		// flottant posé sur le bureau.
		this.menuItem(context, left + 3, top + 122, w - 6, "screen.jn1.terminal.exit",
				this::close, mouseX, mouseY);
	}

	private void menuItem(DrawContext context, int x, int y, int w, String key,
	                      Runnable action, int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 14;
		if (hover) {
			context.fill(x, y, x + w, y + 14, TITLE_BAR);
		}
		context.drawText(this.textRenderer, Text.translatable(key), x + 5, y + 3,
				hover ? TITLE_TEXT : TEXT, false);
		this.hotspots.add(new Hotspot(x, y, w, 14, action, true));
	}

	/** Cadre de fenêtre : relief clair en haut à gauche, sombre en bas à droite. */
	private void frame(DrawContext context, int left, int top, int w, int h, String titleKey) {
		context.fill(left, top, left + w, top + h, FACE);
		context.fill(left, top, left + w, top + 1, LIGHT);
		context.fill(left, top, left + 1, top + h, LIGHT);
		context.fill(left, top + h - 1, left + w, top + h, SHADE);
		context.fill(left + w - 1, top, left + w, top + h, SHADE);

		context.fill(left + 3, top + 3, left + w - 3, top + 17, TITLE_BAR);
		context.drawText(this.textRenderer, Text.translatable(titleKey),
				left + 7, top + 7, TITLE_TEXT, false);

		int closeX = left + w - 16;
		context.fill(closeX, top + 5, closeX + 11, top + 15, FACE);
		context.fill(closeX, top + 5, closeX + 11, top + 6, LIGHT);
		context.drawText(this.textRenderer, "x", closeX + 4, top + 7, TEXT, false);
		this.hotspots.add(new Hotspot(closeX, top + 5, 11, 10,
				() -> this.window = Window.NONE, true));
	}

	/** Bouton du système : aplats et relief, aucune texture. */
	private void button(DrawContext context, int x, int y, int w, int h, Text label,
	                    boolean enabled, Runnable action, int mouseX, int mouseY) {
		boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

		context.fill(x, y, x + w, y + h, hover ? 0xFFD8D8D8 : FACE);
		context.fill(x, y, x + w, y + 1, LIGHT);
		context.fill(x, y, x + 1, y + h, LIGHT);
		context.fill(x, y + h - 1, x + w, y + h, DEEP);
		context.fill(x + w - 1, y, x + w, y + h, DEEP);

		int textX = x + (w - this.textRenderer.getWidth(label)) / 2;
		int textY = y + (h - 8) / 2;
		context.drawText(this.textRenderer, label, textX, textY, enabled ? TEXT : TEXT_DIM, false);

		this.hotspots.add(new Hotspot(x, y, w, h, action, enabled));
	}

	private void renderVignette(DrawContext context) {
		for (int depth = 0; depth < 6; depth++) {
			int colour = ((6 - depth) * 6) << 24;
			context.fill(depth, depth, this.width - depth, depth + 1, colour);
			context.fill(depth, this.height - depth - 1, this.width - depth, this.height - depth, colour);
			context.fill(depth, depth, depth + 1, this.height - depth, colour);
			context.fill(this.width - depth - 1, depth, this.width - depth, this.height - depth, colour);
		}
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.window == Window.GIRLS && this.trackBounds != null
				&& mouseX >= this.trackBounds[0]
				&& mouseX < this.trackBounds[0] + this.trackBounds[2]
				&& mouseY >= this.trackBounds[1]
				&& mouseY < this.trackBounds[1] + TRACK_HEIGHT) {
			this.draggingTrack = true;
			this.scrollToThumb(mouseX);
			return true;
		}
		if (button == 0) {
			// Du dernier dessiné au premier : le menu Démarrer passe devant.
			for (int index = this.hotspots.size() - 1; index >= 0; index--) {
				Hotspot spot = this.hotspots.get(index);
				if (spot.contains(mouseX, mouseY)) {
					if (spot.enabled()) {
						spot.action().run();
						this.playClick();
					}
					return true;
				}
			}
			this.startMenu = false;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** Un cran de molette fait glisser d'une carte. */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (this.window == Window.GIRLS && this.trackBounds != null) {
			this.scroll = Math.clamp(
					this.scroll - (int) ((vertical + horizontal) * (CARD_WIDTH + CARD_GAP)),
					0, this.trackBounds[4]);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
	                            double deltaX, double deltaY) {
		if (this.draggingTrack && this.trackBounds != null) {
			this.scrollToThumb(mouseX);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.draggingTrack = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	/** Place le curseur sous la souris, et le défilement avec lui. */
	private void scrollToThumb(double mouseX) {
		int x = this.trackBounds[0];
		int width = this.trackBounds[2];
		int thumbWidth = this.trackBounds[3];
		int overflow = this.trackBounds[4];
		int travel = width - thumbWidth;
		if (travel <= 0) {
			return;
		}
		double position = mouseX - x - thumbWidth / 2.0;
		this.scroll = Math.clamp((int) Math.round(position / travel * overflow), 0, overflow);
	}

	private void playClick() {
		if (this.client != null) {
			this.client.getSoundManager().play(
					PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.35F));
		}
	}

	private int countCurrency() {
		if (this.client == null || this.client.player == null) {
			return 0;
		}
		int total = 0;
		var inventory = this.client.player.getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isOf(ShopEntry.CURRENCY)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private void spawn(GirlEntry girl) {
		ClientPlayNetworking.send(new TerminalActionPayload(
				this.terminal, TerminalActionPayload.ACTION_SPAWN, girl.id()));
		this.close();
	}

	private void buy(ShopEntry entry) {
		ClientPlayNetworking.send(new ShopPurchasePayload(this.terminal, entry.id()));
	}

	@Override
	public void close() {
		this.quitter();
		super.close();
	}

	/**
	 * Remplacé par un autre écran sans être fermé — la mort, un changement de
	 * dimension : sans cela la caméra restait collée au terminal.
	 */
	@Override
	public void removed() {
		this.quitter();
		super.removed();
	}

	private void quitter() {
		if (this.quitté) {
			return;
		}
		this.quitté = true;
		ClientPlayNetworking.send(
				TerminalActionPayload.of(this.terminal, TerminalActionPayload.ACTION_CLOSE));
		TerminalSession.leave();
	}

	@Override
	public boolean shouldPause() {
		// La caméra doit continuer de glisser derrière l'écran.
		return false;
	}
}
