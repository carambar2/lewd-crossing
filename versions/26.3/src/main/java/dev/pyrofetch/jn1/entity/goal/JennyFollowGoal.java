package dev.pyrofetch.jn1.entity.goal;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Elle suit son compagnon. Si elle se retrouve trop loin — un portail, une
 * chute, un joueur qui court — elle se replace près de lui : le mod ne peut pas
 * se permettre de perdre la seule autre personne du monde derrière un mur.
 */
public class JennyFollowGoal extends Goal {
	private final CompanionEntity jenny;
	private final PathNavigation navigation;
	private final double speed;
	private final float stopDistance;
	private final float teleportDistance;

	private Player target;
	private int updateCountdown;

	public JennyFollowGoal(CompanionEntity jenny, double speed, float stopDistance, float teleportDistance) {
		this.jenny = jenny;
		this.navigation = jenny.getNavigation();
		this.speed = speed;
		this.stopDistance = stopDistance;
		this.teleportDistance = teleportDistance;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.jenny.getMode() != JennyMode.FOLLOW) {
			return false;
		}
		Player candidate = this.resolveTarget();
		if (candidate == null || candidate.isSpectator()) {
			return false;
		}
		if (this.jenny.distanceToSqr(candidate) < this.stopDistance * this.stopDistance) {
			return false;
		}
		this.target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.jenny.getMode() == JennyMode.FOLLOW
				&& this.target != null
				&& !this.target.isSpectator()
				&& !this.navigation.isDone()
				&& this.jenny.distanceToSqr(this.target) > this.stopDistance * this.stopDistance * 0.6;
	}

	@Override
	public void start() {
		this.updateCountdown = 0;
	}

	@Override
	public void stop() {
		this.target = null;
		this.navigation.stop();
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.jenny.getLookControl().setLookAt(this.target, 10.0F, this.jenny.getMaxHeadXRot());

		if (--this.updateCountdown > 0) {
			return;
		}
		this.updateCountdown = this.adjustedTickDelay(10);

		double distanceSq = this.jenny.distanceToSqr(this.target);
		if (distanceSq > this.teleportDistance * this.teleportDistance) {
			this.tryTeleportNear(this.target);
			return;
		}
		this.navigation.moveTo(this.target, this.speed);
	}

	private Player resolveTarget() {
		Player companion = this.jenny.getCompanion();
		if (companion != null && companion.level() == this.jenny.level()) {
			return companion;
		}
		return this.jenny.level().getNearestPlayer(this.jenny, 24.0);
	}

	private void tryTeleportNear(Player player) {
		BlockPos base = player.blockPosition();
		for (int attempt = 0; attempt < 10; attempt++) {
			int dx = this.jenny.getRandom().nextIntBetweenInclusive(-3, 3);
			int dz = this.jenny.getRandom().nextIntBetweenInclusive(-3, 3);
			int dy = this.jenny.getRandom().nextIntBetweenInclusive(-1, 1);
			BlockPos candidate = base.offset(dx, dy, dz);
			if (this.canStandAt(candidate)) {
				this.jenny.snapTo(
						candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
						this.jenny.getYRot(), this.jenny.getXRot());
				this.navigation.stop();
				return;
			}
		}
	}

	private boolean canStandAt(BlockPos pos) {
		Level world = this.jenny.level();
		if (!world.getBlockState(pos.below()).isRedstoneConductor(world, pos.below())) {
			return false;
		}
		return world.noCollision(this.jenny,
				this.jenny.getBoundingBox().move(
						pos.getX() + 0.5 - this.jenny.getX(),
						pos.getY() - this.jenny.getY(),
						pos.getZ() + 0.5 - this.jenny.getZ()));
	}
}
