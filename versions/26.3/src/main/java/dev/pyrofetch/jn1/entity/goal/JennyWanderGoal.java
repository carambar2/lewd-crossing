package dev.pyrofetch.jn1.entity.goal;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Elle traîne autour de son point d'ancrage. Quatre mille ans qu'elle fait des
 * allers-retours dans la même pièce ; ça ne l'occupe plus depuis longtemps.
 */
public class JennyWanderGoal extends Goal {
	private static final int MAX_ANCHOR_DISTANCE = 12;

	private final CompanionEntity jenny;
	private final double speed;

	private double targetX;
	private double targetY;
	private double targetZ;
	private int cooldown;

	public JennyWanderGoal(CompanionEntity jenny, double speed) {
		this.jenny = jenny;
		this.speed = speed;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.jenny.getMode() != JennyMode.WANDER) {
			return false;
		}
		if (this.cooldown-- > 0) {
			return false;
		}
		if (this.jenny.getRandom().nextInt(this.adjustedTickDelay(60)) != 0) {
			return false;
		}

		Vec3 destination = this.findDestination();
		if (destination == null) {
			return false;
		}
		this.targetX = destination.x;
		this.targetY = destination.y;
		this.targetZ = destination.z;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.jenny.getMode() == JennyMode.WANDER && !this.jenny.getNavigation().isDone();
	}

	@Override
	public void start() {
		this.jenny.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ, this.speed);
	}

	@Override
	public void stop() {
		this.cooldown = this.adjustedTickDelay(this.jenny.getRandom().nextIntBetweenInclusive(40, 200));
	}

	private Vec3 findDestination() {
		BlockPos anchor = this.jenny.getAnchorPos();
		Vec3 candidate = LandRandomPos.getPos(this.jenny, 8, 4);
		if (candidate == null) {
			return null;
		}
		if (anchor != null && anchor.distToLowCornerSqr(candidate.x, candidate.y, candidate.z)
				> MAX_ANCHOR_DISTANCE * MAX_ANCHOR_DISTANCE) {
			// Trop loin de l'ancre : on rentre plutôt vers elle.
			return LandRandomPos.getPosTowards(this.jenny, 8, 4, Vec3.atCenterOf(anchor));
		}
		return candidate;
	}
}
