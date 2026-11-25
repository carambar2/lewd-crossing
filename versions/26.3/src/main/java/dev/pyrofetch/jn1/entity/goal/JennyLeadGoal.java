package dev.pyrofetch.jn1.entity.goal;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Elle marche devant. Quand le joueur décroche, elle s'arrête, se retourne et
 * pointe la direction — puis elle attend. Elle a quatre mille ans de pratique.
 */
public class JennyLeadGoal extends Goal {
	/** Au-delà, elle considère que le joueur ne suit plus. */
	private static final double WAIT_DISTANCE_SQ = 8.0 * 8.0;
	/** Distance à laquelle la destination compte comme atteinte. */
	private static final double ARRIVAL_DISTANCE_SQ = 3.0 * 3.0;

	private final CompanionEntity jenny;
	private int repathCountdown;

	public JennyLeadGoal(CompanionEntity jenny) {
		this.jenny = jenny;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.jenny.getMode() == JennyMode.LEAD && this.jenny.getLeadTarget() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void stop() {
		this.jenny.setPointing(false);
		this.jenny.getNavigation().stop();
	}

	@Override
	public void tick() {
		BlockPos target = this.jenny.getLeadTarget();
		if (target == null) {
			return;
		}

		if (this.jenny.blockPosition().distSqr(target) <= ARRIVAL_DISTANCE_SQ) {
			this.jenny.setPointing(false);
			this.jenny.setMode(JennyMode.FOLLOW);
			return;
		}

		Player follower = this.jenny.getCompanion();
		if (follower == null) {
			follower = this.jenny.level().getNearestPlayer(this.jenny, 32.0);
		}

		if (follower != null && this.jenny.distanceToSqr(follower) > WAIT_DISTANCE_SQ) {
			// Il a décroché : on attend en montrant la direction.
			this.jenny.getNavigation().stop();
			this.jenny.getLookControl().setLookAt(follower, 30.0F, 30.0F);
			this.jenny.setPointing(true);
			return;
		}

		this.jenny.setPointing(false);
		if (--this.repathCountdown > 0) {
			return;
		}
		this.repathCountdown = this.adjustedTickDelay(20);
		this.jenny.getNavigation().moveTo(
				target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.9);
	}
}
