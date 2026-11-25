package dev.pyrofetch.jn1.client.terminal;

import dev.pyrofetch.jn1.client.screen.TerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * L'état de la session devant un terminal, côté client uniquement.
 *
 * <p>Le déroulé : la caméra glisse jusqu'à hauteur d'écran, et l'interface
 * n'apparaît qu'une fois arrivée. C'est ce décalage qui fait qu'on a le temps de
 * voir les mains sur le clavier avant que l'écran ne prenne toute la vue.
 */
public final class TerminalSession {
	/** Durée du glissement de la caméra. */
	private static final int APPROACH_TICKS = 9;
	/** Délai avant que l'écran ne s'affiche : un peu après l'arrivée. */
	private static final int SCREEN_DELAY = 6;
	/** Distance à laquelle la caméra s'arrête devant le panneau. */
	private static final double VIEW_DISTANCE = 0.12;
	private static final double VIEW_HEIGHT = 0.72;

	@Nullable
	private static BlockPos terminal;
	private static Direction facing = Direction.NORTH;
	private static int ticks;
	private static boolean leaving;
	private static boolean screenOpened;

	private TerminalSession() {
	}

	public static void begin(BlockPos pos, Direction blockFacing) {
		terminal = pos;
		facing = blockFacing;
		ticks = 0;
		leaving = false;
		screenOpened = false;
	}

	public static void tick() {
		if (terminal == null) {
			return;
		}

		if (leaving) {
			if (--ticks <= 0) {
				terminal = null;
			}
			return;
		}

		if (ticks < APPROACH_TICKS) {
			ticks++;
		}
		if (!screenOpened && ticks >= SCREEN_DELAY) {
			screenOpened = true;
			Minecraft.getInstance().gui.setScreen(new TerminalScreen(terminal));
		}
	}

	/** Referme la session : la caméra revient d'où elle vient. */
	public static void leave() {
		if (terminal != null) {
			leaving = true;
			ticks = Math.min(ticks, APPROACH_TICKS);
		}
	}

	public static void clear() {
		terminal = null;
		leaving = false;
		screenOpened = false;
		ticks = 0;
	}

	public static boolean isActive() {
		return terminal != null;
	}

	@Nullable
	public static BlockPos terminal() {
		return terminal;
	}

	/** Avancement du glissement, adouci aux deux bouts. */
	public static float progress(float tickDelta) {
		float raw = Mth.clamp((ticks + (leaving ? -tickDelta : tickDelta))
				/ (float) APPROACH_TICKS, 0.0F, 1.0F);
		// Lissage : démarrage et arrivée sans à-coup.
		return raw * raw * (3.0F - 2.0F * raw);
	}

	public static Vec3 cameraPos() {
		BlockPos pos = terminal;
		if (pos == null) {
			return Vec3.ZERO;
		}
		return Vec3.atCenterOf(pos)
				.add(facing.getStepX() * VIEW_DISTANCE,
						VIEW_HEIGHT - 0.5,
						facing.getStepZ() * VIEW_DISTANCE);
	}

	/** Le regard fait face au panneau, donc à l'opposé de son orientation. */
	public static float cameraYaw() {
		return facing.getOpposite().toYRot();
	}

	public static float cameraPitch() {
		return 8.0F;
	}
}
