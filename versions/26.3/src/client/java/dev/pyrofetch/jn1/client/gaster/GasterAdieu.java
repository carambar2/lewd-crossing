package dev.pyrofetch.jn1.client.gaster;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * L'adieu de Gaster vu par le joueur (0.56.2).
 *
 * <p>Tant qu'un Gaster en adieu est sous le nez du joueur, l'interface du jeu
 * disparaît (barre d'objets, viseur, main : {@code HudMixin},
 * {@code GameRendererMixin}) et l'image est abîmée : l'image rendue est
 * recopiée ({@link #copie}), puis redessinée par-dessus elle-même — couleurs
 * séparées (le rouge d'un côté, le vert et le bleu de l'autre), tranches
 * horizontales décalées, blocs de parasites, bords noyés de noir, lignes de balayage, et de loin en
 * loin une image inversée ou noire. Le dessin change à chaque demi-tick, de
 * plus en plus fort jusqu'au départ. La malédiction du gardien ancien sonne à son
 * arrivée.
 *
 * <p><strong>26.3.</strong> Plus d'appels directs à OpenGL : l'interface est
 * relevée avant que le monde soit dessiné, puis dessinée après lui. L'image du
 * monde est donc recopiée à la fin de son rendu ({@code LevelRenderEvents.AFTER_RENDER_PASS},
 * {@link #copier}), avant l'interface de la même image ; l'interface, elle, ne
 * fait que poser des éléments qui lisent cette copie, avec les mélanges
 * d'origine : opaque, additif ({@link #ADDITIF}), la vignette du jeu
 * (multiplication), l'inversion du jeu.
 */
public final class GasterAdieu {
	/** Ce qui compte comme « sous le nez ». */
	private static final double PORTÉE = 4.0;
	/** La durée de l'adieu au serveur, pour faire monter l'effet. */
	private static final float DURÉE = 18.0F;

	private static final net.minecraft.resources.Identifier VIGNETTE =
			net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/vignette.png");

	/** La copie redessinée sans mélange : elle remplace ce qui est dessous. */
	private static final RenderPipeline OPAQUE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
			.withLocation(JN1.id("pipeline/gaster_adieu_opaque"))
			.withColorTargetState(ColorTargetState.DEFAULT)
			.build());
	/** La copie ajoutée à ce qui est dessous (un, un) : le vert et le bleu décalés. */
	private static final RenderPipeline ADDITIF = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
			.withLocation(JN1.id("pipeline/gaster_adieu_additive"))
			.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
			.build());

	private static int début = -1;
	@Nullable
	private static RenderTarget copie;
	/** L'ancienne copie, après un changement de taille : l'interface relevée a pu la viser, on la libère une image plus tard. */
	@Nullable
	private static RenderTarget ancienne;
	private static boolean branché;

	private GasterAdieu() {
	}

	public static boolean actif() {
		return début >= 0;
	}

	/** À chaque tick client : un Gaster en adieu tout près ? */
	public static void tick(Minecraft client) {
		if (!branché) {
			branché = true;
			LevelRenderEvents.AFTER_RENDER_PASS.register(contexte -> copier());
		}
		boolean là = false;
		if (client.level != null && client.player != null) {
			for (Entity e : client.level.entitiesForRendering()) {
				if (e instanceof GasterEntity g && g.enAdieu() && g.distanceToSqr(client.player) < PORTÉE * PORTÉE) {
					là = true;
					break;
				}
			}
		}
		if (là && début < 0) {
			début = client.player.tickCount;
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ELDER_GUARDIAN_CURSE, 0.55F, 1.0F));
		} else if (!là) {
			début = -1;
		}
	}

	/** À la fin du rendu du monde, sur le fil du rendu : l'image recopiée, de la taille de l'écran. */
	private static void copier() {
		if (ancienne != null) {
			ancienne.destroyBuffers();
			ancienne = null;
		}
		if (début < 0) {
			return;
		}
		RenderTarget principal = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		if (principal.getColorTexture() == null) {
			return;
		}
		int lp = principal.width;
		int hp = principal.height;
		if (copie == null || copie.width != lp || copie.height != hp) {
			ancienne = copie;
			copie = new TextureTarget("jn1 gaster adieu", lp, hp, GpuFormat.RGBA8_UNORM, null);
		}
		copie.copyColorFrom(principal);
	}

	/** À la place de l'interface du jeu. */
	public static void render(GuiGraphicsExtractor contexte, float tickDelta) {
		Minecraft client = Minecraft.getInstance();
		if (début < 0 || client.player == null) {
			return;
		}
		float t = client.player.tickCount - début + tickDelta;
		float force = Math.min(1.0F, 0.55F + 0.45F * t / DURÉE);
		// Le dessin change à chaque demi-tick ; seule la séparation des couleurs frémit à chaque image.
		RandomSource hasard = RandomSource.create((long) Math.floor(t * 2.0F) * 341873128712L + 132897987541L);
		float l = contexte.guiWidth();
		float h = contexte.guiHeight();

		// La copie de l'image (elle manque à la toute première image de l'adieu).
		RenderTarget image = copie;
		GpuTextureView vue = image == null ? null : image.getColorTextureView();

		// Les couleurs séparées : le rouge seul, opaque, décalé d'un côté ; puis
		// le vert et le bleu ajoutés, décalés de l'autre.
		float d = (2.0F + (float) Math.random() * 5.0F) * force;
		float dy = hasard.nextFloat() < 0.3F ? (hasard.nextFloat() - 0.5F) * 6.0F * force : 0.0F;
		bande(contexte, vue, OPAQUE, 0, h, d, dy, l, h, 1, 0, 0);
		bande(contexte, vue, ADDITIF, 0, h, -d, -dy, l, h, 0, 1, 1);

		// Des tranches arrachées et décalées, parfois teintées.
		int tranches = 3 + hasard.nextInt(1 + Math.round(7 * force));
		for (int i = 0; i < tranches; i++) {
			float y = hasard.nextFloat() * h;
			float épaisseur = 1.0F + hasard.nextFloat() * h * (hasard.nextFloat() < 0.2F ? 0.25F : 0.06F);
			float dx = (hasard.nextFloat() - 0.5F) * l * 0.3F * force;
			float r = 1, v = 1, b = 1;
			float teinte = hasard.nextFloat();
			if (teinte < 0.15F) {
				v = b = 0.2F;
			} else if (teinte < 0.3F) {
				r = 0.2F;
			}
			bande(contexte, vue, OPAQUE, y, Math.min(h, y + épaisseur), dx, 0, l, h, r, v, b);
		}

		// Des blocs de parasites.
		int blocs = hasard.nextInt(2 + Math.round(10 * force));
		for (int i = 0; i < blocs; i++) {
			int x = Math.round(hasard.nextFloat() * l), y = Math.round(hasard.nextFloat() * h);
			int bl = 4 + hasard.nextInt(Math.max(1, Math.round(l / 6))), bh = 2 + hasard.nextInt(Math.max(1, Math.round(h / 14)));
			int gris = hasard.nextInt(3) == 0 ? 0xFFFFFF : hasard.nextInt(2) == 0 ? 0x000000 : 0x7F7F7F;
			contexte.fill(x, y, x + bl, y + bh, ((150 + hasard.nextInt(106)) << 24) | gris);
		}
		// Rien d'autre que lui : les bords noyés de noir, la vignette du jeu
		// passée cinq fois (à trois, le ciel se voyait encore dans un coin).
		// Son mélange est celui d'origine (zéro, un moins la source).
		for (int i = 0; i < 5; i++) {
			contexte.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0.0F, 0.0F, Math.round(l), Math.round(h),
					Math.round(l), Math.round(h));
		}
		// Les lignes de balayage.
		for (int y = 0; y < h; y += 3) {
			contexte.fill(0, y, Math.round(l), y + 1, 0x38000000);
		}

		// De loin en loin : l'image inversée, ou noire.
		float coup = hasard.nextFloat();
		if (coup < 0.07F * force) {
			contexte.fill(RenderPipelines.GUI_INVERT, 0, 0, Math.round(l), Math.round(h), 0xFFFFFFFF);
		} else if (coup < 0.11F * force) {
			contexte.fill(0, 0, Math.round(l), Math.round(h), 0xE0000000);
		}
	}

	/**
	 * Redessine la bande d'écran {@code y1..y2} de l'image recopiée, décalée de
	 * (dx, dy), teintée (r, v, b). L'image est à l'envers sur sa planche (le
	 * bas en premier).
	 */
	private static void bande(GuiGraphicsExtractor contexte, @Nullable GpuTextureView vue, RenderPipeline mélange,
	                          float y1, float y2, float dx, float dy, float l, float h, float r, float v, float b) {
		if (vue == null) {
			return;
		}
		float v1 = 1.0F - y1 / h, v2 = 1.0F - y2 / h;
		int x0 = Math.round(dx);
		int x1 = Math.round(l + dx);
		int haut = Math.round(y1 + dy);
		int bas = Math.round(y2 + dy);
		if (x1 <= x0 || bas <= haut) {
			return;
		}
		contexte.guiRenderState.addGuiElement(new BlitRenderState(mélange,
				TextureSetup.singleTexture(vue, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
				new Matrix3x2f(contexte.pose()), x0, haut, x1, bas, 0.0F, 1.0F, v1, v2,
				ARGB.colorFromFloat(1.0F, r, v, b), null));
	}
}
