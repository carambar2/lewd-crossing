package dev.pyrofetch.jn1.client.agegate;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
	private List<OrderedText> lignes = List.of();

	public AgeGateScreen(@Nullable Screen suite) {
		super(Text.translatable("screen.jn1.age_gate.title"));
		this.suite = suite;
	}

	@Override
	protected void init() {
		this.lignes = this.textRenderer.wrapLines(Text.translatable("screen.jn1.age_gate.message"),
				Math.min(LARGEUR_TEXTE, this.width - 40));
		int y = this.height / 2 + this.lignes.size() * 5 + 10;
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.age_gate.yes"), b -> {
			AgeGate.confirm();
			this.client.setScreen(this.suite);
		}).dimensions(this.width / 2 - LARGEUR_BOUTON - 5, y, LARGEUR_BOUTON, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.age_gate.no"), b -> AgeGate.refuse())
				.dimensions(this.width / 2 + 5, y, LARGEUR_BOUTON, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int y = this.height / 2 - this.lignes.size() * 5 - 24;
		context.drawCenteredTextWithShadow(this.textRenderer, this.title.copy().formatted(Formatting.BOLD),
				this.width / 2, y, 0xFFFF5555);
		y += 20;
		for (OrderedText ligne : this.lignes) {
			context.drawCenteredTextWithShadow(this.textRenderer, ligne, this.width / 2, y, 0xFFFFFFFF);
			y += 10;
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
