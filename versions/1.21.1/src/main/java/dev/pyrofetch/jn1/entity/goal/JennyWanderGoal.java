package dev.pyrofetch.jn1.entity.goal;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

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
		this.setControls(EnumSet.of(Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (this.jenny.getMode() != JennyMode.WANDER) {
			return false;
		}
		if (this.cooldown-- > 0) {
			return false;
		}
		if (this.jenny.getRandom().nextInt(this.getTickCount(60)) != 0) {
			return false;
		}

		Vec3d destination = this.findDestination();
		if (destination == null) {
			return false;
		}
		this.targetX = destination.x;
		this.targetY = destination.y;
		this.targetZ = destination.z;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.jenny.getMode() == JennyMode.WANDER && !this.jenny.getNavigation().isIdle();
	}

	@Override
	public void start() {
		this.jenny.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
	}

	@Override
	public void stop() {
		this.cooldown = this.getTickCount(this.jenny.getRandom().nextBetween(40, 200));
	}

	private Vec3d findDestination() {
		BlockPos anchor = this.jenny.getAnchorPos();
		Vec3d candidate = FuzzyTargeting.find(this.jenny, 8, 4);
		if (candidate == null) {
			return null;
		}
		if (anchor != null && anchor.getSquaredDistance(candidate.x, candidate.y, candidate.z)
				> MAX_ANCHOR_DISTANCE * MAX_ANCHOR_DISTANCE) {
			// Trop loin de l'ancre : on rentre plutôt vers elle.
			return FuzzyTargeting.findTo(this.jenny, 8, 4, Vec3d.ofCenter(anchor));
		}
		return candidate;
	}
}
