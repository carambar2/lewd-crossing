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
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

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
	private static final Identifier POLICE = JN1.id("deltarune");
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
	private Text bulle;
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
		super(Text.translatable("gaster_shop.jn1.title"));
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
				this.client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.GASTER_SHOP_BUY, 1.0F, 1.0F));
			}
			case GasterShopPayload.PAUVRE -> {
				this.bulle = une("pauvre", PAUVRES);
				this.client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.VENDING_DENY, 1.0F, 0.6F));
			}
			case GasterShopPayload.DÉJÀ -> this.bulle = une("deja", DÉJÀS);
			default -> {
			}
		}
	}

	@Override
	protected void init() {
		Window fenêtre = this.client.getWindow();
		int v = Math.max(1, (int) Math.floor(Math.min(fenêtre.getFramebufferWidth() / (double) L,
				fenêtre.getFramebufferHeight() / (double) H)));
		this.k = (float) (v / fenêtre.getScaleFactor());
		this.ox = (this.width - L * this.k) / 2.0F;
		this.oy = (this.height - H * this.k) / 2.0F;
		if (this.copie == null && this.client.world != null
				&& ModEntities.GASTER.create(this.client.world) instanceof GasterEntity neuve) {
			this.copie = neuve;
			neuve.enBoutique();
			if (this.client.world.getEntityById(this.entityId) instanceof GasterEntity vrai) {
				vrai.copierApparence(neuve);
			}
		}
		if (this.réplique.isEmpty()) {
			this.dire(une("accueil", ACCUEILS));
		}
	}

	/** Une réplique de cette sorte, au hasard. */
	private static Text une(String sorte, int combien) {
		return Text.translatable("gaster_shop.jn1." + sorte + "." + (int) (Math.random() * combien));
	}

	private void dire(Text texte) {
		List<String> lignes = new ArrayList<>();
		for (String morceau : texte.getString().split("\n")) {
			this.textRenderer.getTextHandler().wrapLines(morceau, GAUCHE_X2 - GAUCHE_X1 - 16,
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
			this.copie.age++;
		}
		Entity vrai = this.client.world == null ? null : this.client.world.getEntityById(this.entityId);
		if (vrai == null || vrai.isRemoved()) {
			// Parti sans nous (un autre joueur est sorti) : rien à lui dire.
			this.parti = true;
			this.close();
			return;
		}
		if (this.client.player != null && this.client.player.squaredDistanceTo(vrai) > 64.0) {
			this.close();
			return;
		}
		if (this.mode == Mode.ADIEU && ++this.adieu >= DURÉE_ADIEU) {
			this.close();
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
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	// ------------------------------------------------------------ dessin

	@Override
	public void renderBackground(DrawContext contexte, int mouseX, int mouseY, float delta) {
		contexte.fill(0, 0, this.width, this.height, 0xFF000000);
	}

	@Override
	public void render(DrawContext contexte, int mouseX, int mouseY, float delta) {
		this.renderBackground(contexte, mouseX, mouseY, delta);
		var matrices = contexte.getMatrices();

		matrices.push();
		matrices.translate(this.ox, this.oy, 0.0F);
		matrices.scale(this.k, this.k, 1.0F);
		contexte.drawTexture(FOND, 0, 0, 0.0F, 0.0F, L, 120, L, 120);
		matrices.pop();

		// Gaster, à la place du marchand : le buste, le bas caché par les cadres.
		// Si grand, il passerait en partie derrière le fond (le portrait n'est
		// qu'à 50 en profondeur) : on oublie la profondeur du fond, puis la sienne
		// avant les cadres. Droit — incliné vers la souris, sa tête passait
		// derrière ; il ne fait que tourner un peu vers elle.
		if (this.copie != null) {
			int x1 = Math.round(this.ox + 70 * this.k);
			int x2 = Math.round(this.ox + 250 * this.k);
			int y1 = Math.round(this.oy);
			int y2 = Math.round(this.oy + H * this.k);
			float cx = (x1 + x2) / 2.0F;
			float cy = (y1 + y2) / 2.0F;
			contexte.draw();
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
			Portrait.dessiner(contexte, x1, y1, x2, y2, Math.round(86 * this.k), 0.0F,
					cx + (mouseX - cx) * 0.15F, cy, this.copie);
			contexte.draw();
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		}

		matrices.push();
		matrices.translate(this.ox, this.oy, 200.0F);
		matrices.scale(this.k, this.k, 1.0F);
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
		matrices.pop();
	}

	private static Text p(Text texte) {
		return texte.copy().styled(s -> s.withFont(POLICE));
	}

	private static Text p(String texte) {
		return Text.literal(texte).setStyle(STYLE);
	}

	private static void cadre(DrawContext contexte, int x1, int y1, int x2, int y2) {
		contexte.fill(x1, y1, x2, y1 + 1, BLANC);
		contexte.fill(x1, y2 - 1, x2, y2, BLANC);
		contexte.fill(x1, y1, x1 + 1, y2, BLANC);
		contexte.fill(x2 - 1, y1, x2, y2, BLANC);
	}

	private void cœur(DrawContext contexte, int x, int y) {
		contexte.drawTexture(CŒUR, x, y, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
	}

	private void dessinerRéplique(DrawContext contexte) {
		int reste = (int) this.lettres;
		int y = BAS_Y1 + 8;
		for (String ligne : this.réplique) {
			if (reste <= 0) {
				break;
			}
			String vu = ligne.substring(0, Math.min(ligne.length(), reste));
			contexte.drawText(this.textRenderer, p(vu), GAUCHE_X1 + 8, y, BLANC, false);
			reste -= ligne.length();
			y += 12;
		}
	}

	private void dessinerMenu(DrawContext contexte, boolean actif) {
		String[] clés = {"acheter", "parler", "sortir"};
		for (int i = 0; i < clés.length; i++) {
			contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1." + clés[i])),
					DROITE_X1 + 18, MENU_Y[i], actif ? BLANC : GRIS, false);
		}
		if (actif) {
			this.cœur(contexte, DROITE_X1 + 6, MENU_Y[this.choixMenu]);
		}
	}

	private void dessinerÉtal(DrawContext contexte) {
		for (int i = 0; i <= this.objets.size(); i++) {
			int y = PREMIER_ARTICLE + i * LIGNE_ARTICLE;
			if (i == this.objets.size()) {
				contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1.retour")),
						GAUCHE_X1 + 22, y, BLANC, false);
				continue;
			}
			ItemStack objet = this.objets.get(i);
			GasterShop.Rang rang = GasterShop.Rang.de(this.rangs.get(i));
			boolean vendu = (this.vendus & (1 << i)) != 0;
			if (vendu) {
				contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1.vendu")),
						GAUCHE_X1 + 22, y, GRIS, false);
				continue;
			}
			contexte.drawItem(objet, GAUCHE_X1 + 18, y - 5);
			// Coupé avec des points de suspension ; écrit en entier dans le cadre de droite.
			String nom = objet.getName().getString();
			if (this.textRenderer.getWidth(p(nom)) > 128) {
				nom = this.textRenderer.getTextHandler().trimToWidth(nom, 128 - this.textRenderer.getWidth(p("…")), STYLE)
						.stripTrailing() + "…";
			}
			contexte.drawText(this.textRenderer, p(nom), GAUCHE_X1 + 38, y, rang.couleur.getColorValue() | 0xFF000000, false);
			String prix = String.valueOf(rang.prix);
			int fin = GAUCHE_X2 - 14;
			contexte.drawText(this.textRenderer, p(prix), fin - this.textRenderer.getWidth(p(prix)) - 1, y, BLANC, false);
			var matrices = contexte.getMatrices();
			matrices.push();
			matrices.translate(fin, y, 0.0F);
			matrices.scale(0.5F, 0.5F, 1.0F);
			contexte.drawItem(new ItemStack(ModItems.CUM_CRYSTAL), 0, -1);
			matrices.pop();
		}
		this.cœur(contexte, GAUCHE_X1 + 6, PREMIER_ARTICLE + this.choixAchat * LIGNE_ARTICLE);
	}

	private void dessinerDétail(DrawContext contexte) {
		int x = DROITE_X1 + 7;
		int largeur = DROITE_X2 - DROITE_X1 - 12;
		if (this.mode == Mode.CONFIRMER) {
			GasterShop.Rang rang = GasterShop.Rang.de(this.rangs.get(this.choixAchat));
			int y = BAS_Y1 + 8;
			Text question = rang.prix == 1 ? Text.translatable("gaster_shop.jn1.confirmer.un")
					: Text.translatable("gaster_shop.jn1.confirmer", rang.prix);
			for (var ligne : this.textRenderer.wrapLines(p(question), largeur)) {
				contexte.drawText(this.textRenderer, ligne, x, y, BLANC, false);
				y += 11;
			}
			contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1.oui")), x + 14, 182, BLANC, false);
			contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1.non")), x + 14, 198, BLANC, false);
			this.cœur(contexte, x + 2, this.choixOuiNon == 0 ? 182 : 198);
		} else {
			int y = BAS_Y1 + 8;
			if (this.choixAchat < this.objets.size() && (this.vendus & (1 << this.choixAchat)) == 0) {
				GasterShop.Rang rang = GasterShop.Rang.de(this.rangs.get(this.choixAchat));
				int couleur = rang.couleur.getColorValue() | 0xFF000000;
				var lignes = this.textRenderer.wrapLines(p(this.objets.get(this.choixAchat).getName()), largeur);
				for (var ligne : lignes.subList(0, Math.min(3, lignes.size()))) {
					contexte.drawText(this.textRenderer, ligne, x, y, couleur, false);
					y += 10;
				}
				y += 4;
				contexte.drawText(this.textRenderer, p(Text.translatable(rang.clé())), x, y, GRIS, false);
				contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1.prix", rang.prix)), x, y + 11,
						BLANC, false);
				y += 26;
			}
			if (this.bulle != null) {
				int yb = Math.max(y, BAS_Y1 + 40);
				for (var ligne : this.textRenderer.wrapLines(p(this.bulle), largeur)) {
					contexte.drawText(this.textRenderer, ligne, x, yb, BLANC, false);
					yb += 11;
				}
			}
		}
		int cristaux = this.client.player == null ? 0 : this.client.player.getInventory().count(ModItems.CUM_CRYSTAL);
		contexte.drawText(this.textRenderer, p(Text.translatable("gaster_shop.jn1.cristaux", cristaux)), x,
				BAS_Y2 - 14, GRIS, false);
	}

	// ------------------------------------------------------------ commandes

	private void bip() {
		this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 2.0F, 0.15F));
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
					this.dire(Text.translatable("gaster_shop.jn1.parler." + this.parole));
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
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> this.bouger(-1);
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> this.bouger(1);
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_Z -> this.valider();
			case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_BACKSPACE -> this.revenir();
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
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
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 1) {
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
