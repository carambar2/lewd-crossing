package dev.pyrofetch.jn1.client.gaster;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Les os de Gaster et son animation.
 *
 * <p>Le modèle n'a que des os : c'est {@link GasterRenderer} qui dessine le
 * maillage dessus.
 *
 * <p>Quand il épie derrière l'arbre, son corps reste tourné vers le chemin :
 * c'est la tête seule qui suit le joueur, ajoutée à la pose (bornée — au-delà,
 * il le regarde du coin de l'œil plutôt que de se tordre le cou). De même
 * pendant l'adieu, où il nous fixe à bout portant.
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Les fichiers se nomment sans dossier
 * ni suffixe ({@code jn1:entity/gaster}) ; les retouches d'os
 * ({@link #poser}) se font sur les instantanés de l'image, avec ce que le rendu
 * a relevé de lui ({@link #ÉTAT}).
 */
public class GasterModel extends GeoModel<GasterEntity> {
	private static final Identifier OS = JN1.id("entity/gaster");
	private static final Identifier PLANCHE = JN1.id("textures/entity/gaster/gaster.png");
	private static final Identifier ANIMATIONS = JN1.id("entity/gaster");

	/** Ce que ses retouches d'os lisaient sur lui. */
	public record État(boolean adieu, boolean épie, int âge, int id) {
	}

	public static final DataTicket<État> ÉTAT = DataTicket.create("jn1_gaster", État.class);

	@Override
	public Identifier getModelResource(GeoRenderState état) {
		return OS;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState état) {
		return PLANCHE;
	}

	@Override
	public Identifier getAnimationResource(GasterEntity gaster) {
		return ANIMATIONS;
	}

	private static final float LACET_MAX = 50.0F;
	/** Le tremblement de l'adieu, à chaque image : en degrés, et en pixels. */
	private static final float FRISSON = 0.7F;
	private static final float FRISSON_PX = 0.25F;

	/**
	 * L'adieu : à chaque image, un
	 * frisson d'un quart de pixel et de moins d'un degré ; et par saccades, un
	 * coup de tête tenu trois ticks, tiré de son âge pour que chaque image
	 * d'une même saccade le garde — surtout penchée (le roulis tourne le visage
	 * sur lui-même) : à vingt centimètres de l'œil, quelques degrés de lacet
	 * suffisent à le sortir de l'écran.
	 */
	private static void trembler(État gaster, BoneSnapshot tête) {
		float rad = Mth.DEG_TO_RAD;
		tête.setRotX(tête.getRotX() + (float) (Math.random() - 0.5) * 2 * FRISSON * rad);
		tête.setRotY(tête.getRotY() + (float) (Math.random() - 0.5) * 2 * FRISSON * rad);
		tête.setRotZ(tête.getRotZ() + (float) (Math.random() - 0.5) * 2 * FRISSON * rad);
		tête.setTranslateX(tête.getTranslateX() + (float) (Math.random() - 0.5) * 2 * FRISSON_PX);
		tête.setTranslateY(tête.getTranslateY() + (float) (Math.random() - 0.5) * 2 * FRISSON_PX);
		net.minecraft.util.RandomSource saccade = net.minecraft.util.RandomSource.create(
				(gaster.âge() / 3) * 9187L + gaster.id() * 31L);
		if (saccade.nextFloat() < 0.6F) {
			tête.setRotZ(tête.getRotZ() + (saccade.nextFloat() - 0.5F) * 28.0F * rad);
			tête.setRotY(tête.getRotY() + (saccade.nextFloat() - 0.5F) * 5.0F * rad);
			tête.setRotX(tête.getRotX() + (saccade.nextFloat() - 0.5F) * 4.0F * rad);
		}
	}
	private static final float TANGAGE_MAX = 30.0F;

	/**
	 * L'ancien {@code setCustomAnimations}.
	 *
	 * @param lacet   l'écart de la tête au corps, en degrés, au signe des os de GeckoLib
	 * @param tangage le tangage de la tête, au même signe
	 */
	void poser(BoneSnapshots os, État gaster, float lacet, float tangage) {
		BoneSnapshot tête = os.get("bip_head").orElse(null);
		if (tête == null) {
			return;
		}
		if (gaster.adieu()) {
			trembler(gaster, tête);
			return;
		}
		if (!gaster.épie()) {
			return;
		}
		tête.setRotX(tête.getRotX() + Mth.clamp(tangage, -TANGAGE_MAX, TANGAGE_MAX) * Mth.DEG_TO_RAD);
		tête.setRotY(tête.getRotY() + Mth.clamp(lacet, -LACET_MAX, LACET_MAX) * Mth.DEG_TO_RAD);
	}
}
