package dev.pyrofetch.jn1.client.gaster;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

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
 */
public class GasterModel extends GeoModel<GasterEntity> {
	private static final Identifier OS = JN1.id("geo/entity/gaster.geo.json");
	private static final Identifier PLANCHE = JN1.id("textures/entity/gaster/gaster.png");
	private static final Identifier ANIMATIONS = JN1.id("animations/entity/gaster.animation.json");

	@Override
	public Identifier getModelResource(GasterEntity gaster) {
		return OS;
	}

	@Override
	public Identifier getTextureResource(GasterEntity gaster) {
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
	private static void trembler(GasterEntity gaster, GeoBone tête) {
		float rad = MathHelper.RADIANS_PER_DEGREE;
		tête.setRotX(tête.getRotX() + (float) (Math.random() - 0.5) * 2 * FRISSON * rad);
		tête.setRotY(tête.getRotY() + (float) (Math.random() - 0.5) * 2 * FRISSON * rad);
		tête.setRotZ(tête.getRotZ() + (float) (Math.random() - 0.5) * 2 * FRISSON * rad);
		tête.setPosX(tête.getPosX() + (float) (Math.random() - 0.5) * 2 * FRISSON_PX);
		tête.setPosY(tête.getPosY() + (float) (Math.random() - 0.5) * 2 * FRISSON_PX);
		net.minecraft.util.math.random.Random saccade = net.minecraft.util.math.random.Random.create(
				(gaster.age / 3) * 9187L + gaster.getId() * 31L);
		if (saccade.nextFloat() < 0.6F) {
			tête.setRotZ(tête.getRotZ() + (saccade.nextFloat() - 0.5F) * 28.0F * rad);
			tête.setRotY(tête.getRotY() + (saccade.nextFloat() - 0.5F) * 5.0F * rad);
			tête.setRotX(tête.getRotX() + (saccade.nextFloat() - 0.5F) * 4.0F * rad);
		}
	}
	private static final float TANGAGE_MAX = 30.0F;

	@Override
	public void setCustomAnimations(GasterEntity gaster, long instanceId, AnimationState<GasterEntity> state) {
		super.setCustomAnimations(gaster, instanceId, state);
		EntityModelData regard = state.getData(DataTickets.ENTITY_MODEL_DATA);
		GeoBone tête = this.getAnimationProcessor().getBone("bip_head");
		if (tête != null && gaster.enAdieu()) {
			trembler(gaster, tête);
			tête.resetStateChanges();
			return;
		}
		if (!gaster.épie() || regard == null || tête == null) {
			return;
		}
		float lacet = MathHelper.clamp(regard.netHeadYaw(), -LACET_MAX, LACET_MAX);
		float tangage = MathHelper.clamp(regard.headPitch(), -TANGAGE_MAX, TANGAGE_MAX);
		tête.setRotX(tête.getRotX() + tangage * MathHelper.RADIANS_PER_DEGREE);
		tête.setRotY(tête.getRotY() + lacet * MathHelper.RADIANS_PER_DEGREE);
		// Sans quoi l'angle s'empilerait d'image en image (voir CompanionModel.turnHead).
		tête.resetStateChanges();
	}
}
