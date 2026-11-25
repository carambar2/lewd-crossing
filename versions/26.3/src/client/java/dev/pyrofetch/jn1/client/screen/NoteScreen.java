package dev.pyrofetch.jn1.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import dev.pyrofetch.jn1.block.PaperNoteBlockEntity;
import dev.pyrofetch.jn1.block.TextSupport;
import dev.pyrofetch.jn1.client.terminal.NoteSession;
import dev.pyrofetch.jn1.network.SaveNotePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * La page qu'on lit — et qu'on écrit, tant qu'elle n'est pas verrouillée.
 *
 * <p>Elle n'occupe pas tout l'écran et n'assombrit pas ce qu'il y a derrière :
 * on doit continuer de voir la table ou le mur qui porte la note, sans quoi
 * l'approche de la caméra n'aurait servi à rien — autant ouvrir un livre.
 *
 * <p>La saisie est écrite à la main plutôt que posée sur des champs de texte.
 * Quatorze champs empilés auraient chacun leur cadre, leur curseur et leur
 * tabulation : ce serait un formulaire, pas une feuille de papier. Ici une
 * seule barre clignote, là où l'on écrit.
 *
 * <p>Le même éditeur sert aux écrans muraux : seules les couleurs changent —
 * du texte clair sur un fond d'écran éteint au lieu de l'encre sur le papier.
 */
public class NoteScreen extends Screen {
	/** La page d'une feuille ; celle d'un écran mural se mesure sur lui ({@link #mesurer}). */
	private static final int LARGEUR_FEUILLE = 176;
	private static final int MARGE = 14;
	private static final int INTERLIGNE = 11;

	private static final int PLIURE = 0xFFD2C8AE;
	private static final int OMBRE = 0x55000000;

	/** Ce qu'on édite : une feuille, ou un écran mural. */
	public enum Support {
		PAPIER(0xFFE8E0CC, 0xFFF4EEDE, 0xFF2C2620, true),
		ÉCRAN(0xFF0C1214, 0xFF1C2A2E, 0xFF9AF0C8, false);

		private final int fond;
		private final int fondHaut;
		private final int encre;
		private final boolean pliure;

		Support(int fond, int fondHaut, int encre, boolean pliure) {
			this.fond = fond;
			this.fondHaut = fondHaut;
			this.encre = encre;
			this.pliure = pliure;
		}
	}

	private final BlockPos pos;
	private final Support support;
	private final List<String> lignes = new ArrayList<>();
	private boolean verrouillée;
	private int ligne;
	private int colonne;
	private int clignotement;
	/** Pour ne pas envoyer deux fois : par le bouton, puis par la fermeture. */
	private boolean envoyée;
	private int largeur = LARGEUR_FEUILLE;
	private int lignesMax = PaperNoteBlockEntity.LIGNES_MAX;
	/** La taille de l'écran mural, en blocs, pour l'afficher ; nulle pour une feuille. */
	private int[] taille;

	public NoteScreen(BlockPos pos) {
		this(pos, Support.PAPIER);
	}

	public NoteScreen(BlockPos pos, Support support) {
		super(Component.translatable("screen.jn1.note.title"));
		this.pos = pos;
		this.support = support;
	}

	@Override
	protected void init() {
		this.lignes.clear();
		if (this.minecraft != null && this.minecraft.level != null) {
			BlockEntity entité = this.minecraft.level.getBlockEntity(this.pos);
			if (entité instanceof TextSupport texte) {
				this.lignes.addAll(texte.lignes());
				this.verrouillée = texte.verrouillée();
			}
		}
		if (this.lignes.isEmpty()) {
			this.lignes.add("");
		}
		this.mesurer();
		this.ligne = this.lignes.size() - 1;
		this.colonne = this.lignes.get(this.ligne).length();
		this.placerBoutons();
	}

	/**
	 * Un écran mural tient autant que sa taille le permet (0.83.1) : sa
	 * capacité ({@code WallScreenBlock.capacité}) fait la largeur de la page et
	 * le nombre de lignes, dans la limite de ce que l'interface peut montrer.
	 */
	private void mesurer() {
		if (this.support != Support.ÉCRAN || this.minecraft == null || this.minecraft.level == null) {
			return;
		}
		var état = this.minecraft.level.getBlockState(this.pos);
		if (!(état.getBlock() instanceof dev.pyrofetch.jn1.block.WallScreenBlock)) {
			return;
		}
		this.taille = dev.pyrofetch.jn1.block.WallScreenBlock.taille(this.minecraft.level, this.pos,
				état.getValue(dev.pyrofetch.jn1.block.WallScreenBlock.FACING));
		int[] capacité = dev.pyrofetch.jn1.block.WallScreenBlock.capacité(this.taille);
		this.largeur = Math.max(LARGEUR_FEUILLE / 2, Math.min(capacité[0] + 2 * MARGE, this.width - 24));
		this.lignesMax = Math.max(1, Math.min(capacité[1], (this.height - 2 * MARGE - 60) / INTERLIGNE));
		// Un texte écrit avant, sur un écran plus grand, garde ce qui tient.
		while (this.lignes.size() > this.lignesMax) {
			this.lignes.removeLast();
		}
	}

	private int largeurTexte() {
		return this.largeur - MARGE * 2;
	}

	/**
	 * Les boutons se tiennent sous la page, qui grandit avec le nombre de
	 * lignes : ils sont donc reposés à chaque fois qu'elle change de taille.
	 */
	private void placerBoutons() {
		int hauteur = this.hauteurPage();
		int left = (this.width - this.largeur) / 2;
		int bas = (this.height - hauteur) / 2 + hauteur + 6;

		if (this.verrouillée) {
			// Rien à enregistrer : la note est figée, on ne fait que la reposer.
			this.addRenderableWidget(Button.builder(
							Component.translatable("screen.jn1.note.close"), b -> this.onClose())
					.bounds(left + this.largeur / 2 - 40, bas, 80, 20).build());
		} else {
			this.addRenderableWidget(Button.builder(
							Component.translatable("screen.jn1.note.save"), b -> this.enregistrer(false))
					.bounds(left, bas, this.largeur / 2 - 2, 20).build());
			this.addRenderableWidget(Button.builder(
							Component.translatable("screen.jn1.note.lock"), b -> this.enregistrer(true))
					.bounds(left + this.largeur / 2 + 2, bas, this.largeur / 2 - 2, 20).build());
		}
	}

	private int hauteurPage() {
		return MARGE * 2 + Math.max(1, this.lignes.size()) * INTERLIGNE;
	}

	private void enregistrer(boolean verrou) {
		ClientPlayNetworking.send(new SaveNotePayload(this.pos,
				List.copyOf(this.lignes), verrou));
		this.envoyée = true;
		this.onClose();
	}

	/** Rien à assombrir : le décor doit rester lisible tout autour de la page. */
	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int hauteur = this.hauteurPage();
		int left = (this.width - this.largeur) / 2;
		int top = (this.height - hauteur) / 2;

		context.fill(left + 3, top + 3, left + this.largeur + 3, top + hauteur + 3, OMBRE);
		context.fill(left, top, left + this.largeur, top + hauteur, this.support.fond);
		context.fill(left, top, left + this.largeur, top + 2, this.support.fondHaut);
		// Une pliure verticale : sans elle, la page n'est qu'un rectangle beige.
		if (this.support.pliure) {
			context.fill(left + this.largeur / 2, top + 4, left + this.largeur / 2 + 1,
					top + hauteur - 4, PLIURE);
		}

		int y = top + MARGE;
		for (int index = 0; index < this.lignes.size(); index++) {
			String texte = this.lignes.get(index);
			context.text(this.font, texte, left + MARGE, y, this.support.encre, false);
			if (!this.verrouillée && index == this.ligne && (this.clignotement / 6) % 2 == 0) {
				int x = left + MARGE + this.font.width(
						texte.substring(0, Math.min(this.colonne, texte.length())));
				context.fill(x, y - 1, x + 1, y + 9, this.support.encre);
			}
			y += INTERLIGNE;
		}
		if (this.taille != null) {
			// Sous la page : la taille de l'écran, et les lignes qu'il reste.
			String repère = this.taille[0] + "×" + this.taille[1] + "  ·  " + this.lignes.size() + "/" + this.lignesMax;
			context.text(this.font, repère, left + this.largeur - this.font.width(repère),
					top - 10, 0xFFB0C8C0, true);
		}

		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public void tick() {
		this.clignotement++;
	}

	@Override
	public boolean charTyped(CharacterEvent événement) {
		char c = (char) événement.codepoint();
		if (this.verrouillée || c < ' ') {
			return false;
		}
		String texte = this.lignes.get(this.ligne);
		// La largeur de la page décide, pas un compte de caractères : un
		// « m » et un « i » ne prennent pas la même place.
		if (this.font.width(texte + c) > this.largeurTexte()) {
			return this.àLaLigne(texte, c);
		}
		this.lignes.set(this.ligne, texte.substring(0, this.colonne) + c
				+ texte.substring(this.colonne));
		this.colonne++;
		return true;
	}

	/**
	 * La ligne est pleine : en tapant au bout, le dernier mot passe à la ligne
	 * suivante, comme dans un traitement de texte, tant qu'il en reste une.
	 */
	private boolean àLaLigne(String texte, char c) {
		if (this.colonne != texte.length() || this.lignes.size() >= this.lignesMax) {
			return false;
		}
		String suite;
		if (c == ' ') {
			suite = "";
		} else {
			String complète = texte + c;
			int espace = complète.lastIndexOf(' ');
			if (espace > 0 && espace < complète.length() - 1) {
				this.lignes.set(this.ligne, complète.substring(0, espace));
				suite = complète.substring(espace + 1);
			} else {
				suite = String.valueOf(c);
			}
		}
		this.lignes.add(this.ligne + 1, suite);
		this.ligne++;
		this.colonne = suite.length();
		this.recalerBoutons();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent événement) {
		int key = événement.key();
		if (this.verrouillée) {
			return super.keyPressed(événement);
		}
		String texte = this.lignes.get(this.ligne);
		switch (key) {
			case InputConstants.KEY_BACKSPACE -> {
				if (this.colonne > 0) {
					this.lignes.set(this.ligne, texte.substring(0, this.colonne - 1)
							+ texte.substring(this.colonne));
					this.colonne--;
				} else if (this.ligne > 0) {
					// On recolle la ligne à la précédente, curseur au raccord.
					String dessus = this.lignes.get(this.ligne - 1);
					this.colonne = dessus.length();
					this.lignes.set(this.ligne - 1, dessus + texte);
					this.lignes.remove(this.ligne);
					this.ligne--;
					this.recalerBoutons();
				}
				return true;
			}
			case InputConstants.KEY_DELETE -> {
				if (this.colonne < texte.length()) {
					this.lignes.set(this.ligne, texte.substring(0, this.colonne)
							+ texte.substring(this.colonne + 1));
				}
				return true;
			}
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				if (this.lignes.size() < this.lignesMax) {
					this.lignes.set(this.ligne, texte.substring(0, this.colonne));
					this.lignes.add(this.ligne + 1, texte.substring(this.colonne));
					this.ligne++;
					this.colonne = 0;
					this.recalerBoutons();
				}
				return true;
			}
			case InputConstants.KEY_LEFT -> {
				if (this.colonne > 0) {
					this.colonne--;
				} else if (this.ligne > 0) {
					this.ligne--;
					this.colonne = this.lignes.get(this.ligne).length();
				}
				return true;
			}
			case InputConstants.KEY_RIGHT -> {
				if (this.colonne < texte.length()) {
					this.colonne++;
				} else if (this.ligne < this.lignes.size() - 1) {
					this.ligne++;
					this.colonne = 0;
				}
				return true;
			}
			case InputConstants.KEY_UP -> {
				if (this.ligne > 0) {
					this.ligne--;
					this.colonne = Math.min(this.colonne, this.lignes.get(this.ligne).length());
				}
				return true;
			}
			case InputConstants.KEY_DOWN -> {
				if (this.ligne < this.lignes.size() - 1) {
					this.ligne++;
					this.colonne = Math.min(this.colonne, this.lignes.get(this.ligne).length());
				}
				return true;
			}
			case InputConstants.KEY_HOME -> {
				this.colonne = 0;
				return true;
			}
			case InputConstants.KEY_END -> {
				this.colonne = texte.length();
				return true;
			}
			default -> {
				return super.keyPressed(événement);
			}
		}
	}

	/**
	 * Replace les boutons sans toucher au texte.
	 *
	 * <p>Repasser par init() les aurait bien reposés, mais aurait aussi relu les
	 * lignes depuis le bloc : tout ce qui venait d'être tapé aurait disparu à la
	 * première touche Entrée.
	 */
	private void recalerBoutons() {
		this.clearWidgets();
		this.placerBoutons();
	}

	/**
	 * Sortir sans passer par un bouton ne perd rien : Échap enregistre comme le
	 * ferait « Enregistrer et fermer », sans verrouiller. Une note qu'on rouvre
	 * pour corriger un mot et qu'on referme d'un réflexe ne doit pas se vider.
	 */
	@Override
	public void onClose() {
		if (!this.verrouillée && !this.envoyée) {
			this.envoyée = true;
			ClientPlayNetworking.send(new SaveNotePayload(this.pos,
					List.copyOf(this.lignes), false));
		}
		NoteSession.leave();
		super.onClose();
	}

	/** Remplacée sans être fermée (la mort, un changement de dimension) : la caméra revient quand même. */
	@Override
	public void removed() {
		NoteSession.leave();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
