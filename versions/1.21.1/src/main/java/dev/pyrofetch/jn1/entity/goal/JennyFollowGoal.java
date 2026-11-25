package dev.pyrofetch.jn1.entity.goal;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Elle suit son compagnon. Si elle se retrouve trop loin — un portail, une
 * chute, un joueur qui court — elle se replace près de lui : le mod ne peut pas
 * se permettre de perdre la seule autre personne du monde derrière un mur.
 */
public class JennyFollowGoal extends Goal {
	private final CompanionEntity jenny;
	private final EntityNavigation navigation;
	private final double speed;
	private final float stopDistance;
	private final float teleportDistance;

	private PlayerEntity target;
	private int updateCountdown;

	public JennyFollowGoal(CompanionEntity jenny, double speed, float stopDistance, float teleportDistance) {
		this.jenny = jenny;
		this.navigation = jenny.getNavigation();
		this.speed = speed;
		this.stopDistance = stopDistance;
		this.teleportDistance = teleportDistance;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.jenny.getMode() != JennyMode.FOLLOW) {
			return false;
		}
		PlayerEntity candidate = this.resolveTarget();
		if (candidate == null || candidate.isSpectator()) {
			return false;
		}
		if (this.jenny.squaredDistanceTo(candidate) < this.stopDistance * this.stopDistance) {
			return false;
		}
		this.target = candidate;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.jenny.getMode() == JennyMode.FOLLOW
				&& this.target != null
				&& !this.target.isSpectator()
				&& !this.navigation.isIdle()
				&& this.jenny.squaredDistanceTo(this.target) > this.stopDistance * this.stopDistance * 0.6;
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
		this.jenny.getLookControl().lookAt(this.target, 10.0F, this.jenny.getMaxLookPitchChange());

		if (--this.updateCountdown > 0) {
			return;
		}
		this.updateCountdown = this.getTickCount(10);

		double distanceSq = this.jenny.squaredDistanceTo(this.target);
		if (distanceSq > this.teleportDistance * this.teleportDistance) {
			this.tryTeleportNear(this.target);
			return;
		}
		this.navigation.startMovingTo(this.target, this.speed);
	}

	private PlayerEntity resolveTarget() {
		PlayerEntity companion = this.jenny.getCompanion();
		if (companion != null && companion.getWorld() == this.jenny.getWorld()) {
			return companion;
		}
		return this.jenny.getWorld().getClosestPlayer(this.jenny, 24.0);
	}

	private void tryTeleportNear(PlayerEntity player) {
		BlockPos base = player.getBlockPos();
		for (int attempt = 0; attempt < 10; attempt++) {
			int dx = this.jenny.getRandom().nextBetween(-3, 3);
			int dz = this.jenny.getRandom().nextBetween(-3, 3);
			int dy = this.jenny.getRandom().nextBetween(-1, 1);
			BlockPos candidate = base.add(dx, dy, dz);
			if (this.canStandAt(candidate)) {
				this.jenny.refreshPositionAndAngles(
						candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
						this.jenny.getYaw(), this.jenny.getPitch());
				this.navigation.stop();
				return;
			}
		}
	}

	private boolean canStandAt(BlockPos pos) {
		World world = this.jenny.getWorld();
		if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
			return false;
		}
		return world.isSpaceEmpty(this.jenny,
				this.jenny.getBoundingBox().offset(
						pos.getX() + 0.5 - this.jenny.getX(),
						pos.getY() - this.jenny.getY(),
						pos.getZ() + 0.5 - this.jenny.getZ()));
	}
}
