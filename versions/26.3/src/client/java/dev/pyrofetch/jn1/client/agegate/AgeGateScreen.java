package dev.pyrofetch.jn1.client.agegate;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * « Avez-vous 18 ans ou plus ? » Oui : on continue ; non : le jeu se ferme.
 * Échap ne le ferme pas : il faut répondre.
 */
public class AgeGateScreen extends Screen {
	private static final int LARGEUR_TEXTE = 300;
	private static final int LARGEUR_BOUTON = 150;

	/** L'écran à rendre après un oui ; {@code null} : revenir au jeu. */
	@Nullable
	private final Screen suite;
	private List<FormattedCharSequence> lignes = List.of();

	public AgeGateScreen(@Nullable Screen suite) {
		super(Component.translatable("screen.jn1.age_gate.title"));
		this.suite = suite;
	}

	@Override
	protected void init() {
		this.lignes = this.font.split(Component.translatable("screen.jn1.age_gate.message"),
				Math.min(LARGEUR_TEXTE, this.width - 40));
		int y = this.height / 2 + this.lignes.size() * 5 + 10;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.age_gate.yes"), b -> {
			AgeGate.confirm();
			this.minecraft.gui.setScreen(this.suite);
		}).bounds(this.width / 2 - LARGEUR_BOUTON - 5, y, LARGEUR_BOUTON, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.age_gate.no"), b -> AgeGate.refuse())
				.bounds(this.width / 2 + 5, y, LARGEUR_BOUTON, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		int y = this.height / 2 - this.lignes.size() * 5 - 24;
		context.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD),
				this.width / 2, y, 0xFFFF5555);
		y += 20;
		for (FormattedCharSequence ligne : this.lignes) {
			context.centeredText(this.font, ligne, this.width / 2, y, 0xFFFFFFFF);
			y += 10;
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
