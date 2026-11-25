package dev.pyrofetch.jn1.entity.goal;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Pendant une scène, ou menottée, elle reste où elle est et regarde droit
 * devant elle (une chaîne, elle, la tire encore : c'est la laisse du jeu).
 *
 * <p>L'objectif ne fait rien d'autre que tenir le déplacement et le regard :
 * c'est ce qui empêche de s'y mettre ceux qui la font suivre, errer ou tourner
 * la tête. Le saut reste libre, pour que la nage la garde à flot si la scène
 * se joue dans l'eau.
 */
public class SceneGoal extends Goal {
	private final CompanionEntity jenny;

	public SceneGoal(CompanionEntity jenny) {
		this.jenny = jenny;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		// Le câlin aussi : elle reste à côté du joueur, le bras sur ses épaules.
		return this.jenny.hasScene() || this.jenny.isCuffed()
				|| this.jenny.getGesture() == dev.pyrofetch.jn1.entity.CompanionGesture.HUG;
	}

	@Override
	public boolean shouldContinue() {
		return this.canStart();
	}

	@Override
	public void start() {
		this.jenny.getNavigation().stop();
	}
}
