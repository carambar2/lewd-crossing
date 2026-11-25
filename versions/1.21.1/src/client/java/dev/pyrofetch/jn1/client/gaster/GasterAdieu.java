package dev.pyrofetch.jn1.client.gaster;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

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
 */
public final class GasterAdieu {
	/** Ce qui compte comme « sous le nez ». */
	private static final double PORTÉE = 4.0;
	/** La durée de l'adieu au serveur, pour faire monter l'effet. */
	private static final float DURÉE = 18.0F;

	private static final net.minecraft.util.Identifier VIGNETTE =
			net.minecraft.util.Identifier.ofVanilla("textures/misc/vignette.png");

	private static int début = -1;
	private static Framebuffer copie;

	private GasterAdieu() {
	}

	public static boolean actif() {
		return début >= 0;
	}

	/** À chaque tick client : un Gaster en adieu tout près ? */
	public static void tick(MinecraftClient client) {
		boolean là = false;
		if (client.world != null && client.player != null) {
			for (Entity e : client.world.getEntities()) {
				if (e instanceof GasterEntity g && g.enAdieu() && g.squaredDistanceTo(client.player) < PORTÉE * PORTÉE) {
					là = true;
					break;
				}
			}
		}
		if (là && début < 0) {
			début = client.player.age;
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.55F, 1.0F));
		} else if (!là) {
			début = -1;
		}
	}

	/** À la place de l'interface du jeu. */
	public static void render(DrawContext contexte, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (début < 0 || client.player == null) {
			return;
		}
		float t = client.player.age - début + tickDelta;
		float force = Math.min(1.0F, 0.55F + 0.45F * t / DURÉE);
		// Le dessin change à chaque demi-tick ; seule la séparation des couleurs frémit à chaque image.
		Random hasard = Random.create((long) Math.floor(t * 2.0F) * 341873128712L + 132897987541L);
		float l = contexte.getScaledWindowWidth();
		float h = contexte.getScaledWindowHeight();
		contexte.draw();

		Framebuffer principal = client.getFramebuffer();
		int lp = principal.textureWidth, hp = principal.textureHeight;
		if (copie == null) {
			copie = new SimpleFramebuffer(lp, hp, false, MinecraftClient.IS_SYSTEM_MAC);
		} else if (copie.textureWidth != lp || copie.textureHeight != hp) {
			copie.resize(lp, hp, MinecraftClient.IS_SYSTEM_MAC);
		}
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, principal.fbo);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copie.fbo);
		GlStateManager._glBlitFrameBuffer(0, 0, lp, hp, 0, 0, lp, hp, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
		principal.beginWrite(false);

		RenderSystem.disableDepthTest();
		RenderSystem.setShaderTexture(0, copie.getColorAttachment());
		RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
		Matrix4f m = contexte.getMatrices().peek().getPositionMatrix();

		// Les couleurs séparées : le rouge seul, opaque, décalé d'un côté ; puis
		// le vert et le bleu ajoutés, décalés de l'autre.
		float d = (2.0F + (float) Math.random() * 5.0F) * force;
		float dy = hasard.nextFloat() < 0.3F ? (hasard.nextFloat() - 0.5F) * 6.0F * force : 0.0F;
		RenderSystem.disableBlend();
		bande(m, 0, h, d, dy, l, h, 1, 0, 0);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
		bande(m, 0, h, -d, -dy, l, h, 0, 1, 1);

		// Des tranches arrachées et décalées, parfois teintées.
		RenderSystem.disableBlend();
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
			bande(m, y, Math.min(h, y + épaisseur), dx, 0, l, h, r, v, b);
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

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
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR);
		for (int i = 0; i < 5; i++) {
			contexte.drawTexture(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, Math.round(l), Math.round(h), Math.round(l), Math.round(h));
		}
		contexte.draw();
		RenderSystem.defaultBlendFunc();
		// Les lignes de balayage.
		for (int y = 0; y < h; y += 3) {
			contexte.fill(0, y, Math.round(l), y + 1, 0x38000000);
		}
		contexte.draw();

		// De loin en loin : l'image inversée, ou noire.
		float coup = hasard.nextFloat();
		if (coup < 0.07F * force) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ZERO);
			contexte.fill(0, 0, Math.round(l), Math.round(h), 0xFFFFFFFF);
			contexte.draw();
			RenderSystem.defaultBlendFunc();
		} else if (coup < 0.11F * force) {
			contexte.fill(0, 0, Math.round(l), Math.round(h), 0xE0000000);
			contexte.draw();
		}
		RenderSystem.enableDepthTest();
	}

	/**
	 * Redessine la bande d'écran {@code y1..y2} de l'image recopiée, décalée de
	 * (dx, dy), teintée (r, v, b). L'image est à l'envers sur sa planche (le
	 * bas en premier).
	 */
	private static void bande(Matrix4f m, float y1, float y2, float dx, float dy, float l, float h,
	                          float r, float v, float b) {
		BufferBuilder tampon = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		float v1 = 1.0F - y1 / h, v2 = 1.0F - y2 / h;
		tampon.vertex(m, dx, y1 + dy, 0).texture(0, v1).color(r, v, b, 1.0F);
		tampon.vertex(m, dx, y2 + dy, 0).texture(0, v2).color(r, v, b, 1.0F);
		tampon.vertex(m, l + dx, y2 + dy, 0).texture(1, v2).color(r, v, b, 1.0F);
		tampon.vertex(m, l + dx, y1 + dy, 0).texture(1, v1).color(r, v, b, 1.0F);
		BufferRenderer.drawWithGlobalProgram(tampon.end());
	}
}
