package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.lift.SelectorBlockEntity;
import dev.pyrofetch.jn1.network.OpenSelectorPayload;
import dev.pyrofetch.jn1.network.SelectorActionPayload;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Le menu d'un sélecteur d'étage : une plaque d'inox brossé, un afficheur à
 * diodes, et une colonne de boutons ronds, l'étage le plus haut en haut.
 * Celui où se tient la plateforme est cerclé d'ambre ; celui de ce sélecteur
 * porte une flèche. En bas, « Réglages » ouvre le sous-menu : où la
 * plateforme s'arrête par rapport à ce sélecteur, et le nom de l'étage.
 *
 * <p>Tout est dessiné au pixel d'interface, sans mise à l'échelle
 * fractionnaire : c'est ce qui garde les chiffres nets. Le fond n'est flouté
 * qu'une fois, avant la plaque.
 */
public class SelectorScreen extends Screen {
	private static final int INOX = 0xFFB9BEC6;
	private static final int INOX_CLAIR = 0xFFE2E6EC;
	private static final int INOX_SOMBRE = 0xFF5A5F68;
	private static final int FILET_CLAIR = 0x16FFFFFF;
	private static final int FILET_SOMBRE = 0x0C000000;
	private static final int GRAVURE = 0xFF30343A;
	private static final int BAGUE = 0xFF7E848E;
	private static final int FACE = 0xFF2E3238;
	private static final int FACE_SURVOL = 0xFF454A52;
	private static final int AMBRE = 0xFFFFA630;
	private static final int AMBRE_TERNE = 0xFF5A3208;
	private static final int ÉCRAN = 0xFF0E0906;
	private static final Pattern NUMÉRO = Pattern.compile("-?\\d+");

	/** La largeur minimale ; un nom d'étage plus long élargit la plaque. */
	private static final int LARGEUR = 170;
	private static final int PAS = 24;
	private static final int RAYON = 9;
	private static final int PIED = 22;

	private OpenSelectorPayload données;
	private boolean réglages;
	private int survol = -1;
	private TextFieldWidget champNom;

	public SelectorScreen(OpenSelectorPayload données) {
		super(Text.translatable("screen.jn1.lift_selector.title"));
		this.données = données;
	}

	public BlockPos pos() {
		return this.données.pos();
	}

	/** Le serveur a renvoyé le menu (un réglage a changé) : on le reprend sans quitter le sous-menu. */
	public void mettreÀJour(OpenSelectorPayload données) {
		this.données = données;
		this.init(this.client, this.width, this.height);
	}

	/**
	 * Ce que montrent l'afficheur et le bouton : le premier nombre du nom de
	 * l'étage, sinon sa première lettre ; sans nom, son rang depuis le bas.
	 */
	public static String étiquette(Optional<Text> nom, int rang) {
		if (nom.isPresent()) {
			String texte = nom.get().getString().strip();
			Matcher nombre = NUMÉRO.matcher(texte);
			if (nombre.find()) {
				return nombre.group();
			}
			if (!texte.isEmpty()) {
				return texte.substring(0, texte.offsetByCodePoints(0, 1)).toUpperCase();
			}
		}
		return String.valueOf(rang);
	}

	private List<OpenSelectorPayload.Floor> étages() {
		return this.données.étages();
	}

	private Text nom(int rang) {
		return this.étages().get(rang).nom().orElse(Text.translatable("screen.jn1.lift_selector.floor", rang));
	}

	private int largeur() {
		int nom = 0;
		for (int rang = 0; rang < this.étages().size(); rang++) {
			nom = Math.max(nom, this.textRenderer.getWidth(this.nom(rang)));
		}
		return Math.max(LARGEUR, 26 + RAYON + 8 + nom + 24);
	}

	private int hauteur() {
		if (this.réglages) {
			return 64 + 112;
		}
		return 64 + Math.max(1, this.étages().size()) * PAS + PIED;
	}

	private int gauche() {
		return (this.width - this.largeur()) / 2;
	}

	private int haut() {
		return (this.height - this.hauteur()) / 2;
	}

	@Override
	protected void init() {
		this.clearChildren();
		if (!this.réglages) {
			return;
		}
		int g = this.gauche();
		int l = this.largeur();
		int y = this.haut() + 64;
		this.addDrawableChild(ButtonWidget.builder(Text.literal("−"),
				b -> this.décaler(-1)).dimensions(g + 12, y + 14, 20, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("+"),
				b -> this.décaler(1)).dimensions(g + l - 32, y + 14, 20, 20).build());
		this.champNom = new TextFieldWidget(this.textRenderer, g + 12, y + 56, l - 24, 18,
				Text.translatable("screen.jn1.lift_selector.name"));
		this.champNom.setMaxLength(SelectorBlockEntity.NOM_MAX);
		this.champNom.setText(this.données.nom());
		this.champNom.setPlaceholder(Text.translatable("screen.jn1.lift_selector.name.hint"));
		this.addDrawableChild(this.champNom);
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.lift_selector.done"), b -> {
			if (!this.champNom.getText().strip().equals(this.données.nom())) {
				ClientPlayNetworking.send(new SelectorActionPayload(this.pos(), SelectorActionPayload.NOMMER, 0,
						this.champNom.getText()));
			}
			this.réglages = false;
			this.init(this.client, this.width, this.height);
		}).dimensions(g + 12, y + 84, l - 24, 20).build());
	}

	private void décaler(int pas) {
		int décalage = Math.clamp(this.données.décalage() + pas, -SelectorBlockEntity.DÉCALAGE_MAX,
				SelectorBlockEntity.DÉCALAGE_MAX);
		ClientPlayNetworking.send(new SelectorActionPayload(this.pos(), SelectorActionPayload.DÉCALER, décalage, ""));
	}

	/** Le rang de l'étage dont la ligne est sous ce point ; -1 s'il n'y en a pas. */
	private int rangSous(double mouseX, double mouseY) {
		if (this.réglages) {
			return -1;
		}
		int n = this.étages().size();
		int g = this.gauche();
		int h = this.haut();
		for (int i = 0; i < n; i++) {
			int y = h + 64 + i * PAS;
			if (mouseX >= g + 10 && mouseX < g + this.largeur() - 10 && mouseY >= y + 1 && mouseY < y + PAS - 1) {
				return n - 1 - i;
			}
		}
		return -1;
	}

	private boolean surRéglages(double mouseX, double mouseY) {
		if (this.réglages) {
			return false;
		}
		int y = this.haut() + this.hauteur() - PIED + 4;
		return mouseX >= this.gauche() + 10 && mouseX < this.gauche() + this.largeur() - 10
				&& mouseY >= y && mouseY < y + 12;
	}

	/** Le flou n'est posé qu'une fois, avant la plaque : {@code Screen.render} le reposait par-dessus. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int g = this.gauche();
		int h = this.haut();
		int l = this.largeur();
		this.plaque(context, g, h, l, this.hauteur());
		this.afficheur(context, g + 16, h + 12, l - 32, 30);
		Text titre = this.données.nom().isEmpty() ? this.title : Text.literal(this.données.nom());
		context.drawText(this.textRenderer, titre, g + (l - this.textRenderer.getWidth(titre)) / 2, h + 48, GRAVURE, false);

		if (this.réglages) {
			this.dessinerRéglages(context, g, h + 64, l);
			super.render(context, mouseX, mouseY, delta);
			return;
		}

		this.survol = this.rangSous(mouseX, mouseY);
		int n = this.étages().size();
		if (this.données.plateforme() == 0) {
			context.drawTextWrapped(this.textRenderer, Text.translatable("screen.jn1.lift_selector.no_platform"),
					g + 14, h + 68, l - 28, GRAVURE);
		} else if (n == 0) {
			context.drawText(this.textRenderer, Text.translatable("screen.jn1.lift_selector.empty"), g + 16, h + 68,
					GRAVURE, false);
		}
		for (int i = 0; i < n; i++) {
			int rang = n - 1 - i;
			OpenSelectorPayload.Floor étage = this.étages().get(rang);
			int y = h + 64 + i * PAS;
			int cx = g + 26;
			int cy = y + PAS / 2;
			boolean ici = !this.données.enRoute() && étage.niveau() == this.données.actuel();
			boolean dessus = rang == this.survol;
			this.bouton(context, cx, cy, ici, dessus, étiquette(étage.nom(), rang));
			int couleurNom = ici ? 0xFF8A5010 : dessus ? 0xFF14171B : GRAVURE;
			context.drawText(this.textRenderer, this.nom(rang), cx + RAYON + 8, cy - 4, couleurNom, false);
			if (étage.niveau() == this.données.niveau()) {
				// L'étage de ce sélecteur : « vous êtes ici ».
				context.drawText(this.textRenderer, "◀", g + l - 18, cy - 4, 0xFF8A5010, false);
			}
		}
		boolean surR = this.surRéglages(mouseX, mouseY);
		Text réglages = Text.translatable("screen.jn1.lift_selector.settings");
		context.drawText(this.textRenderer, réglages, g + (l - this.textRenderer.getWidth(réglages)) / 2,
				h + this.hauteur() - PIED + 6, surR ? 0xFF14171B : 0xFF6A6F78, false);
	}

	private void dessinerRéglages(DrawContext context, int g, int y, int l) {
		int relatif = this.données.décalage() + 1;
		Text ligne = relatif == 0 ? Text.translatable("screen.jn1.lift_selector.offset.level")
				: Text.translatable(relatif < 0 ? "screen.jn1.lift_selector.offset.below" : "screen.jn1.lift_selector.offset.above",
						Math.abs(relatif));
		Text intitulé = Text.translatable("screen.jn1.lift_selector.offset");
		context.drawText(this.textRenderer, intitulé, g + (l - this.textRenderer.getWidth(intitulé)) / 2, y + 2, GRAVURE, false);
		context.drawText(this.textRenderer, ligne, g + (l - this.textRenderer.getWidth(ligne)) / 2, y + 20, 0xFF8A5010, false);
		Text nom = Text.translatable("screen.jn1.lift_selector.name");
		context.drawText(this.textRenderer, nom, g + 12, y + 44, GRAVURE, false);
	}

	/** L'inox brossé : des filets horizontaux, un chanfrein clair en haut, sombre en bas, quatre vis. */
	private void plaque(DrawContext context, int x, int y, int l, int h) {
		context.fill(x - 1, y - 1, x + l + 1, y + h + 1, 0xFF1E2126);
		context.fill(x, y, x + l, y + h, INOX);
		for (int ligne = y + 1; ligne < y + h - 1; ligne++) {
			int bruit = (ligne * 7919) % 5;
			if (bruit == 0) {
				context.fill(x + 1, ligne, x + l - 1, ligne + 1, FILET_CLAIR);
			} else if (bruit == 3) {
				context.fill(x + 1, ligne, x + l - 1, ligne + 1, FILET_SOMBRE);
			}
		}
		context.fill(x, y, x + l, y + 1, INOX_CLAIR);
		context.fill(x, y, x + 1, y + h, INOX_CLAIR);
		context.fill(x, y + h - 1, x + l, y + h, INOX_SOMBRE);
		context.fill(x + l - 1, y, x + l, y + h, INOX_SOMBRE);
		for (int[] vis : new int[][] {{5, 5}, {l - 8, 5}, {5, h - 8}, {l - 8, h - 8}}) {
			this.disque(context, x + vis[0] + 1, y + vis[1] + 1, 2, INOX_SOMBRE);
			context.fill(x + vis[0], y + vis[1] + 1, x + vis[0] + 3, y + vis[1] + 2, INOX_CLAIR);
		}
	}

	/** L'afficheur : l'étage où est la plateforme (ou celui qu'elle vise, flèche en tête), en grandes diodes. */
	private void afficheur(DrawContext context, int x, int y, int l, int h) {
		context.fill(x - 1, y - 1, x + l + 1, y + h + 1, INOX_SOMBRE);
		context.fill(x, y, x + l, y + h, ÉCRAN);
		int rang = -1;
		for (int i = 0; i < this.étages().size(); i++) {
			if (this.étages().get(i).niveau() == this.données.actuel()) {
				rang = i;
			}
		}
		String texte = this.données.plateforme() == 0 || rang < 0 ? "--" : étiquette(this.étages().get(rang).nom(), rang);
		if (this.données.enRoute()) {
			texte = "»" + texte;
		}
		int largeur = this.textRenderer.getWidth(texte) * 2;
		context.getMatrices().push();
		context.getMatrices().translate(x + (l - largeur) / 2, y + (h - 14) / 2, 0);
		context.getMatrices().scale(2.0F, 2.0F, 1.0F);
		context.drawText(this.textRenderer, texte, 1, 1, AMBRE_TERNE, false);
		context.drawText(this.textRenderer, texte, 0, 0, AMBRE, false);
		context.getMatrices().pop();
		context.fill(x, y, x + l, y + 1, 0x30FFFFFF);
		context.fill(x + 2, y + 2, x + 6, y + 3, 0x20FFFFFF);
	}

	private void bouton(DrawContext context, int cx, int cy, boolean allumé, boolean dessus, String étiquette) {
		this.disque(context, cx, cy + 1, RAYON, 0x60000000);
		this.disque(context, cx, cy, RAYON, allumé ? AMBRE : BAGUE);
		this.disque(context, cx, cy, RAYON - 2, allumé ? 0xFF3A2204 : dessus ? FACE_SURVOL : FACE);
		context.fill(cx - 5, cy - RAYON + 1, cx - 1, cy - RAYON + 2, allumé ? 0xFFFFD890 : INOX_CLAIR);
		int couleur = allumé ? AMBRE : 0xFFE6E8EC;
		context.drawText(this.textRenderer, étiquette, cx - this.textRenderer.getWidth(étiquette) / 2 + 1, cy - 3,
				couleur, false);
	}

	private void disque(DrawContext context, int cx, int cy, int r, int couleur) {
		for (int dy = -r; dy <= r; dy++) {
			int demi = (int) Math.floor(Math.sqrt(r * r - dy * dy + r * 0.8));
			demi = Math.min(demi, r);
			context.fill(cx - demi, cy + dy, cx + demi + 1, cy + dy + 1, couleur);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.surRéglages(mouseX, mouseY)) {
			this.réglages = true;
			this.init(this.client, this.width, this.height);
			return true;
		}
		// L'étage sous le clic, pas celui survolé à la dernière image.
		int rang = button == 0 ? this.rangSous(mouseX, mouseY) : -1;
		if (rang >= 0) {
			ClientPlayNetworking.send(new SelectorActionPayload(this.pos(), SelectorActionPayload.ALLER,
					this.étages().get(rang).niveau(), ""));
			if (this.client != null) {
				this.client.getSoundManager().play(
						PositionedSoundInstance.master(SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 1.2F, 0.5F));
			}
			this.close();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Échap dans le sous-menu ramène aux étages.
		if (this.réglages && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			this.réglages = false;
			this.init(this.client, this.width, this.height);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
