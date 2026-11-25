package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.morph.MorphState;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.render.CompanionModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** Les fichiers de la compagne dont le joueur a pris l'apparence, masquage compris. */
public class MorphModel extends GeoModel<MorphAnimatable> {
	/** L'inclinaison du buste accroupi, en radians : celle du joueur du jeu. */
	private static final float ACCROUPI = 0.5F;

	@Override
	public Identifier getModelResource(MorphAnimatable animatable) {
		return JN1.id(animatable.kind().modelPath(animatable.outfit()));
	}

	@Override
	public Identifier getTextureResource(MorphAnimatable animatable) {
		return JN1.id(animatable.kind().texturePath());
	}

	@Override
	public Identifier getAnimationResource(MorphAnimatable animatable) {
		return JN1.id(animatable.kind().animationPath());
	}

	@Override
	public void setCustomAnimations(MorphAnimatable animatable, long instanceId, AnimationState<MorphAnimatable> state) {
		super.setCustomAnimations(animatable, instanceId, state);
		// Le mannequin, les emplacements d'armure et les objets modélisés,
		// comme chez les compagnes.
		CompanionModel.hideUnusedParts(this.getAnimationProcessor());
		if (state.getData(DataTickets.ENTITY) instanceof PlayerEntity joueur) {
			MorphState forme = ClientMorphs.of(joueur);
			CompanionModel.applyShape(this.getAnimationProcessor(), forme.boobs(), forme.booty());
		}
		this.suivreLeJoueur(state);
	}

	/**
	 * La tête suit le regard du joueur ({@link CompanionModel#turnHead}, commun
	 * aux compagnes), le buste s'incline quand il s'accroupit — même prudence :
	 * {@code resetStateChanges} après la retouche.
	 */
	private void suivreLeJoueur(AnimationState<MorphAnimatable> state) {
		CompanionModel.turnHead(this.getAnimationProcessor(), state.getData(DataTickets.ENTITY_MODEL_DATA));
		if (state.getData(DataTickets.ENTITY) instanceof PlayerEntity joueur) {
			MorphArmPoses.apply(this.getAnimationProcessor(), joueur, state.getData(DataTickets.ENTITY_MODEL_DATA));
		}
		GeoBone buste = this.getAnimationProcessor().getBone("upperBody");
		if (buste != null && state.getData(DataTickets.ENTITY) instanceof PlayerEntity joueur
				&& joueur.isInSneakingPose()) {
			// Même sens que la tête qui regarde vers le bas : vers l'avant.
			buste.setRotX(buste.getRotX() - ACCROUPI);
			buste.resetStateChanges();
		}
	}
}
