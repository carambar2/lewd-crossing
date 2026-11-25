package dev.pyrofetch.jn1.client.gaster;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.screen.Portrait;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import dev.pyrofetch.jn1.gaster.GasterShop;
import dev.pyrofetch.jn1.network.GasterShopActionPayload;
import dev.pyrofetch.jn1.network.GasterShopPayload;
import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModItems;
import dev.pyrofetch.jn1.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.jetbrains.annotations.Nullable;

/**
 * La boutique de Gaster, à la manière de celles de Deltarune : le fond de la
 * boutique de Seam en haut (repris du jeu, {@code tools/gen_boutique_gaster.py}),
 * Gaster dessiné au milieu à la place du marchand, et en bas deux cadres
 * blancs sur fond noir — à gauche ce qu'il dit, à droite le menu, le cœur rouge
 * pour curseur.
 *
 * <p>Tout est dessiné dans une page virtuelle de 320 × 240 (le fond fait 320 ×
 * 120), agrandie d'un nombre entier de pixels réels quand l'écran le permet.
 * Flèches ou souris pour choisir, Entrée / Espace / Z pour valider, Échap / X
 * pour revenir ; Échap au menu, c'est sortir — et Gaster s'en va.
 */
public class GasterShopScreen extends Screen {
	private static final Identifier FOND = JN1.id("textures/gui/gaster_shop/fond.png");
	private static final Identifier CŒUR = JN1.id("textures/gui/gaster_shop/coeur.png");
	/** La police de l'intro de Deltarune ({@code fnt_main}, tirée par {@code tools/gen_boutique_gaster.py}). */
	private static final FontDescription POLICE = new FontDescription.Resource(JN1.id("deltarune"));
	private static final Style STYLE = Style.EMPTY.withFont(POLICE);
	private static final int L = 320;
	private static final int H = 240;
	private static final int BLANC = 0xFFFFFFFF;
	private static final int GRIS = 0xFF808080;
	// Combien de répliques de chaque sorte ({@code gaster_shop.jn1.<sorte>.<n>}) :
	// à tenir à jour avec en_us.json.
	private static final int ACCUEILS = 9;
	private static final int PAROLES = 18;
	private static final int AUTRES = 4;
	private static final int MERCIS = 4;
	private static final int PAUVRES = 3;
	private static final int DÉJÀS = 2;
	private static final int ADIEUX = 4;
	private static final int DURÉE_ADIEU = 30;

	// Les cadres du bas, en coordonnées virtuelles.
	private static final int GAUCHE_X1 = 4, GAUCHE_X2 = 214, DROITE_X1 = 218, DROITE_X2 = 316, BAS_Y1 = 124, BAS_Y2 = 236;
	private static final int LIGNE_ARTICLE = 19, PREMIER_ARTICLE = 131;
	private static final int[] MENU_Y = {134, 152, 170};

	private enum Mode { MENU, ACHAT, CONFIRMER, PARLER, ADIEU }

	private final int entityId;
	private List<ItemStack> objets;
	private List<Integer> rangs;
	private int vendus;

	private Mode mode = Mode.MENU;
	private int choixMenu;
	private int choixAchat;
	private int choixOuiNon;
	private int parole;

	/** Ce que dit Gaster, en lignes, écrit lettre à lettre. */
	private List<String> réplique = List.of();
	private float lettres;
	/** Le mot de la boutique dans le cadre de droite (merci, pas assez…). */
	@Nullable
	private Component bulle;
	private int ticks;
	private int adieu;
	private boolean parti;

	@Nullable
	private GasterEntity copie;

	// Page virtuelle → écran : x = ox + k · xv.
	private float k = 1.0F;
	private float ox;
	private float oy;

	public GasterShopScreen(GasterShopPayload payload) {
		super(Component.translatable("gaster_shop.jn1.title"));
		this.entityId = payload.entityId();
		this.objets = payload.objets();
		this.rangs = payload.rangs();
		this.vendus = payload.vendus();
		this.parole = (int) (Math.random() * PAROLES);
	}

	public int entité() {
		return this.entityId;
	}

	public void mettreÀJour(GasterShopPayload payload) {
		this.objets = payload.objets();
		this.rangs = payload.rangs();
		this.vendus = payload.vendus();
		switch (payload.retour()) {
			case GasterShopPayload.MERCI -> {
				this.bulle = une("merci", MERCIS);
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.GASTER_SHOP_BUY, 1.0F, 1.0F));
			}
			case GasterShopPayload.PAUVRE -> {
				this.bulle = une("pauvre", PAUVRES);
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VENDING_DENY, 1.0F, 0.6F));
			}
			case GasterShopPayload.DÉJÀ -> this.bulle = une("deja", DÉJÀS);
			default -> {
			}
		}
	}

	@Override
	protected void init() {
		Window fenêtre = this.minecraft.getWindow();
		int v = Math.max(1, (int) Math.floor(Math.min(fenêtre.getWidth() / (double) L,
				fenêtre.getHeight() / (double) H)));
		this.k = (float) (v / fenêtre.getGuiScale());
		this.ox = (this.width - L * this.k) / 2.0F;
		this.oy = (this.height - H * this.k) / 2.0F;
		if (this.copie == null && this.minecraft.level != null
				&& ModEntities.GASTER.create(this.minecraft.level, EntitySpawnReason.LOAD) instanceof GasterEntity neuve) {
			this.copie = dev.pyrofetch.jn1.client.screen.HorsDuMonde.identifier(neuve);
			neuve.enBoutique();
			if (this.minecraft.level.getEntity(this.entityId) instanceof GasterEntity vrai) {
				vrai.copierApparence(neuve);
			}
		}
		if (this.réplique.isEmpty()) {
			this.dire(une("accueil", ACCUEILS));
		}
	}

	/** Une réplique de cette sorte, au hasard. */
	private static Component une(String sorte, int combien) {
		return Component.translatable("gaster_shop.jn1." + sorte + "." + (int) (Math.random() * combien));
	}

	private void dire(Component texte) {
		List<String> lignes = new ArrayList<>();
		for (String morceau : texte.getString().split("\n")) {
			this.font.getSplitter().splitLines(morceau, GAUCHE_X2 - GAUCHE_X1 - 16,
					STYLE).forEach(l -> lignes.add(l.getString()));
		}
		this.réplique = lignes;
		this.lettres = 0;
	}

	private int longueurRéplique() {
		return this.réplique.stream().mapToInt(String::length).sum();
	}

	private boolean répliqueFinie() {
		return this.lettres >= this.longueurRéplique();
	}

	@Override
	public void tick() {
		this.ticks++;
		this.lettres += 2.0F;
		if (this.copie != null) {
			this.copie.tickCount++;
		}
		Entity vrai = this.minecraft.level == null ? null : this.minecraft.level.getEntity(this.entityId);
		if (vrai == null || vrai.isRemoved()) {
			// Parti sans nous (un autre joueur est sorti) : rien à lui dire.
			this.parti = true;
			this.onClose();
			return;
		}
		if (this.minecraft.player != null && this.minecraft.player.distanceToSqr(vrai) > 64.0) {
			this.onClose();
			return;
		}
		if (this.mode == Mode.ADIEU && ++this.adieu >= DURÉE_ADIEU) {
			this.onClose();
		}
	}

	@Override
	public void removed() {
		if (!this.parti) {
			this.parti = true;
			ClientPlayNetworking.send(new GasterShopActionPayload(this.entityId, GasterShopActionPayload.SORTIR, 0));
		}
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	// ------------------------------------------------------------ dessin

	/** Le fond noir ; le jeu le dessine avant le reste (26.3). */
	@Override
	public void extractBackground(GuiGraphicsExtractor contexte, int mouseX, int mouseY, float delta) {
		contexte.fill(0, 0, this.width, this.height, 0xFF000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor contexte, int mouseX, int mouseY, float delta) {
		var matrices = contexte.pose();

		matrices.pushMatrix();
		matrices.translate(this.ox, this.oy);
		matrices.scale(this.k, this.k);
		contexte.blit(RenderPipelines.GUI_TEXTURED, FOND, 0, 0, 0.0F, 0.0F, L, 120, L, 120);
		matrices.popMatrix();

		// Gaster, à la place du marchand : le buste, le bas caché par les cadres.
		// En 1.21.1, si grand, il passait en partie derrière le fond (le portrait
		// n'était qu'à 50 en profondeur) : on y vidait la profondeur avant et
		// après lui. En 26.3, le portrait est dessiné à part, dans sa vignette,
		// puis posé par-dessus le fond : plus de profondeur à vider. Droit —
		// incliné vers la souris, sa tête passait derrière ; il ne fait que
		// tourner un peu vers elle.
		if (this.copie != null) {
			int x1 = Math.round(this.ox + 70 * this.k);
			int x2 = Math.round(this.ox + 250 * this.k);
			int y1 = Math.round(this.oy);
			int y2 = Math.round(this.oy + H * this.k);
			float cx = (x1 + x2) / 2.0F;
			float cy = (y1 + y2) / 2.0F;
			Portrait.dessiner(contexte, x1, y1, x2, y2, Math.round(86 * this.k), 0.0F,
					cx + (mouseX - cx) * 0.15F, cy, this.copie);
		}

		// Les cadres, par-dessus (ce qui chevauche un élément déjà posé passe devant lui).
		matrices.pushMatrix();
		matrices.translate(this.ox, this.oy);
		matrices.scale(this.k, this.k);
		contexte.fill(0, 120, L, H, 0xFF000000);
		cadre(contexte, GAUCHE_X1, BAS_Y1, GAUCHE_X2, BAS_Y2);
		cadre(contexte, DROITE_X1, BAS_Y1, DROITE_X2, BAS_Y2);
		switch (this.mode) {
			case MENU, PARLER, ADIEU -> {
				this.dessinerRéplique(contexte);
				this.dessinerMenu(contexte, this.mode == Mode.MENU);
			}
			case ACHAT, CONFIRMER -> {
				this.dessinerÉtal(contexte);
				this.dessinerDétail(contexte);
			}
		}
		matrices.popMatrix();
	}

	/** La couleur d'un rang (ses codes de formatage n'en portent plus en 26.3). */
	private static int couleur(GasterShop.Rang rang) {
		TextColor couleur = TextColor.fromLegacyFormat(rang.couleur);
		return (couleur == null ? 0xFFFFFF : couleur.getValue()) | 0xFF000000;
	}

	private static Component p(Component texte) {
		return texte.copy().withStyle(s -> s.withFont(POLICE));
	}

	private static Component p(String texte) {
		return Component.literal(texte).setStyle(STYLE);
	}

	private static void cadre(GuiGraphicsExtractor contexte, int x1, int y1, int x2, int y2) {
		contexte.fill(x1, y1, x2, y1 + 1, BLANC);
		contexte.fill(x1, y2 - 1, x2, y2, BLANC);
		contexte.fill(x1, y1, x1 + 1, y2, BLANC);
		contexte.fill(x2 - 1, y1, x2, y2, BLANC);
	}

	private void cœur(GuiGraphicsExtractor contexte, int x, int y) {
		contexte.blit(RenderPipelines.GUI_TEXTURED, CŒUR, x, y, 0.0F, 0.0F, 8, 8, 16, 16, 16, 16);
	}

	private void dessinerRéplique(GuiGraphicsExtractor contexte) {
		int reste = (int) this.lettres;
		int y = BAS_Y1 + 8;
		for (String ligne : this.réplique) {
			if (reste <= 0) {
				break;
			}
			String vu = ligne.substring(0, Math.min(ligne.length(), reste));
			contexte.text(this.font, p(vu), GAUCHE_X1 + 8, y, BLANC, false);
			reste -= ligne.length();
			y += 12;
		}
	}

	private void dessinerMenu(GuiGraphicsExtractor contexte, boolean actif) {
		String[] clés = {"acheter", "parler", "sortir"};
		for (int i = 0; i < clés.length; i++) {
			contexte.text(this.font, p(Component.translatable("gaster_shop.jn1." + clés[i])),
					DROITE_X1 + 18, MENU_Y[i], actif ? BLANC : GRIS, false);
		}
		if (actif) {
			this.cœur(contexte, DROITE_X1 + 6, MENU_Y[this.choixMenu]);
		}
	}

	private void dessinerÉtal(GuiGraphicsExtractor contexte) {
		for (int i = 0; i <= this.objets.size(); i++) {
			int y = PREMIER_ARTICLE + i * LIGNE_ARTICLE;
			if (i == this.objets.size()) {
				contexte.text(this.font, p(Component.translatable("gaster_shop.jn1.retour")),
						GAUCHE_X1 + 22, y, BLANC, false);
				continue;
			}
			ItemStack objet = this.objets.get(i);
			GasterShop.Rang rang = GasterShop.Rang.de(this.rangs.get(i));
			boolean vendu = (this.vendus & (1 << i)) != 0;
			if (vendu) {
				contexte.text(this.font, p(Component.translatable("gaster_shop.jn1.vendu")),
						GAUCHE_X1 + 22, y, GRIS, false);
				continue;
			}
			contexte.item(objet, GAUCHE_X1 + 18, y - 5);
			// Coupé avec des points de suspension ; écrit en entier dans le cadre de droite.
			String nom = objet.getHoverName().getString();
			if (this.font.width(p(nom)) > 128) {
				nom = this.font.getSplitter().plainHeadByWidth(nom, 128 - this.font.width(p("…")), STYLE)
						.stripTrailing() + "…";
			}
			contexte.text(this.font, p(nom), GAUCHE_X1 + 38, y, couleur(rang), false);
			String prix = String.valueOf(rang.prix);
			int fin = GAUCHE_X2 - 14;
			contexte.text(this.font, p(prix), fin - this.font.width(p(prix)) - 1, y, BLANC, false);
			var matrices = contexte.pose();
			matrices.pushMatrix();
			matrices.translate(fin, y);
			matrices.scale(0.5F, 0.5F);
			contexte.item(new ItemStack(ModItems.CUM_CRYSTAL), 0, -1);
			matrices.popMatrix();
		}
		this.cœur(contexte, GAUCHE_X1 + 6, PREMIER_ARTICLE + this.choixAchat * LIGNE_ARTICLE);
	}

	private void dessinerDétail(GuiGraphicsExtractor contexte) {
		int x = DROITE_X1 + 7;
		int largeur = DROITE_X2 - DROITE_X1 - 12;
		if (this.mode == Mode.CONFIRMER) {
			GasterShop.Rang rang = GasterShop.Rang.de(this.rangs.get(this.choixAchat));
			int y = BAS_Y1 + 8;
			Component question = rang.prix == 1 ? Component.translatable("gaster_shop.jn1.confirmer.un")
					: Component.translatable("gaster_shop.jn1.confirmer", rang.prix);
			for (var ligne : this.font.split(p(question), largeur)) {
				contexte.text(this.font, ligne, x, y, BLANC, false);
				y += 11;
			}
			contexte.text(this.font, p(Component.translatable("gaster_shop.jn1.oui")), x + 14, 182, BLANC, false);
			contexte.text(this.font, p(Component.translatable("gaster_shop.jn1.non")), x + 14, 198, BLANC, false);
			this.cœur(contexte, x + 2, this.choixOuiNon == 0 ? 182 : 198);
		} else {
			int y = BAS_Y1 + 8;
			if (this.choixAchat < this.objets.size() && (this.vendus & (1 << this.choixAchat)) == 0) {
				GasterShop.Rang rang = GasterShop.Rang.de(this.rangs.get(this.choixAchat));
				int couleur = couleur(rang);
				var lignes = this.font.split(p(this.objets.get(this.choixAchat).getHoverName()), largeur);
				for (var ligne : lignes.subList(0, Math.min(3, lignes.size()))) {
					contexte.text(this.font, ligne, x, y, couleur, false);
					y += 10;
				}
				y += 4;
				contexte.text(this.font, p(Component.translatable(rang.clé())), x, y, GRIS, false);
				contexte.text(this.font, p(Component.translatable("gaster_shop.jn1.prix", rang.prix)), x, y + 11,
						BLANC, false);
				y += 26;
			}
			if (this.bulle != null) {
				int yb = Math.max(y, BAS_Y1 + 40);
				for (var ligne : this.font.split(p(this.bulle), largeur)) {
					contexte.text(this.font, ligne, x, yb, BLANC, false);
					yb += 11;
				}
			}
		}
		int cristaux = this.minecraft.player == null ? 0 : this.minecraft.player.getInventory().countItem(ModItems.CUM_CRYSTAL);
		contexte.text(this.font, p(Component.translatable("gaster_shop.jn1.cristaux", cristaux)), x,
				BAS_Y2 - 14, GRIS, false);
	}

	// ------------------------------------------------------------ commandes

	private void bip() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 2.0F, 0.15F));
	}

	private void bouger(int sens) {
		switch (this.mode) {
			case MENU -> this.choixMenu = Math.floorMod(this.choixMenu + sens, MENU_Y.length);
			case ACHAT -> this.choixAchat = Math.floorMod(this.choixAchat + sens, this.objets.size() + 1);
			case CONFIRMER -> this.choixOuiNon = 1 - this.choixOuiNon;
			default -> {
				return;
			}
		}
		this.bip();
	}

	private void valider() {
		if ((this.mode == Mode.MENU || this.mode == Mode.PARLER) && !this.répliqueFinie()) {
			this.lettres = this.longueurRéplique();
			return;
		}
		switch (this.mode) {
			case MENU -> {
				this.bip();
				if (this.choixMenu == 0) {
					this.mode = Mode.ACHAT;
					this.bulle = null;
				} else if (this.choixMenu == 1) {
					this.mode = Mode.PARLER;
					this.dire(Component.translatable("gaster_shop.jn1.parler." + this.parole));
					this.parole = (this.parole + 1) % PAROLES;
				} else {
					this.sortir();
				}
			}
			case ACHAT -> {
				this.bip();
				if (this.choixAchat >= this.objets.size()) {
					this.revenirAuMenu();
				} else if ((this.vendus & (1 << this.choixAchat)) != 0) {
					this.bulle = une("deja", DÉJÀS);
				} else {
					this.mode = Mode.CONFIRMER;
					this.choixOuiNon = 0;
				}
			}
			case CONFIRMER -> {
				this.mode = Mode.ACHAT;
				if (this.choixOuiNon == 0) {
					ClientPlayNetworking.send(new GasterShopActionPayload(this.entityId,
							GasterShopActionPayload.ACHETER, this.choixAchat));
				} else {
					this.bip();
				}
			}
			case PARLER -> this.revenirAuMenu();
			default -> {
			}
		}
	}

	private void revenir() {
		switch (this.mode) {
			case MENU -> this.sortir();
			case ACHAT, PARLER -> this.revenirAuMenu();
			case CONFIRMER -> this.mode = Mode.ACHAT;
			default -> {
			}
		}
	}

	private void revenirAuMenu() {
		this.mode = Mode.MENU;
		this.dire(une("autre", AUTRES));
	}

	private void sortir() {
		this.mode = Mode.ADIEU;
		this.adieu = 0;
		this.dire(une("adieu", ADIEUX));
	}

	@Override
	public boolean keyPressed(KeyEvent événement) {
		switch (événement.key()) {
			case InputConstants.KEY_UP, InputConstants.KEY_W -> this.bouger(-1);
			case InputConstants.KEY_DOWN, InputConstants.KEY_S -> this.bouger(1);
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE,
					InputConstants.KEY_Z -> this.valider();
			case InputConstants.KEY_ESCAPE, InputConstants.KEY_X, InputConstants.KEY_BACKSPACE -> this.revenir();
			default -> {
				return super.keyPressed(événement);
			}
		}
		return true;
	}

	/** La ligne sous la souris, dans le mode courant ; -1 hors de toute ligne. */
	private int ligneSous(double mouseX, double mouseY) {
		double x = (mouseX - this.ox) / this.k;
		double y = (mouseY - this.oy) / this.k;
		switch (this.mode) {
			case MENU -> {
				if (x >= DROITE_X1 && x < DROITE_X2) {
					for (int i = 0; i < MENU_Y.length; i++) {
						if (y >= MENU_Y[i] - 4 && y < MENU_Y[i] + 12) {
							return i;
						}
					}
				}
			}
			case ACHAT -> {
				if (x >= GAUCHE_X1 && x < GAUCHE_X2) {
					int i = (int) Math.floor((y - PREMIER_ARTICLE + 6) / LIGNE_ARTICLE);
					if (i >= 0 && i <= this.objets.size()) {
						return i;
					}
				}
			}
			case CONFIRMER -> {
				if (x >= DROITE_X1 && x < DROITE_X2) {
					if (y >= 178 && y < 194) {
						return 0;
					}
					if (y >= 194 && y < 210) {
						return 1;
					}
				}
			}
			default -> {
			}
		}
		return -1;
	}

	private void choisir(int ligne) {
		switch (this.mode) {
			case MENU -> this.choixMenu = ligne;
			case ACHAT -> this.choixAchat = ligne;
			case CONFIRMER -> this.choixOuiNon = ligne;
			default -> {
			}
		}
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		int ligne = this.ligneSous(mouseX, mouseY);
		if (ligne >= 0) {
			this.choisir(ligne);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent événement, boolean doubleClic) {
		double mouseX = événement.x();
		double mouseY = événement.y();
		if (événement.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			this.revenir();
			return true;
		}
		int ligne = this.ligneSous(mouseX, mouseY);
		if (ligne >= 0) {
			this.choisir(ligne);
			this.valider();
		} else if (this.mode == Mode.PARLER || !this.répliqueFinie()) {
			this.valider();
		}
		return true;
	}
}
