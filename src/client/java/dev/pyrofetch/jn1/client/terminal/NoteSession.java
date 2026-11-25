package dev.pyrofetch.jn1.client.terminal;

import dev.pyrofetch.jn1.client.screen.NoteScreen;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * L'état de la lecture d'une note, côté client.
 *
 * <p>Même déroulé que devant un terminal : la vue glisse jusqu'à la feuille,
 * la page ne s'affiche qu'une fois arrivée.
 *
 * <p>Ce qui change, c'est que la feuille n'est pas toujours verticale. Le
 * terminal est mural par construction et sa caméra se contente d'un recul dans
 * son axe ; une note se pose aussi bien à terre ou au plafond, et il faut alors
 * la survoler ou la regarder d'en dessous. Les trois cas sont traités ici, sans
 * quoi une note au sol se lirait de profil, c'est-à-dire pas du tout.
 */
public final class NoteSession {
	private static final int APPROACH_TICKS = 9;
	private static final int SCREEN_DELAY = 6;
	/** Recul depuis la feuille : assez pour voir le bloc qui la porte autour. */
	private static final double RECUL = 1.15;

	@Nullable
	private static BlockPos note;
	private static BlockFace face = BlockFace.WALL;
	private static Direction facing = Direction.NORTH;
	private static int ticks;
	private static boolean leaving;
	private static boolean screenOpened;

	private NoteSession() {
	}

	public static void begin(BlockPos pos, BlockFace appui, Direction sens) {
		note = pos;
		face = appui;
		facing = sens;
		ticks = 0;
		leaving = false;
		screenOpened = false;
	}

	public static void tick() {
		if (note == null) {
			return;
		}
		if (leaving) {
			if (--ticks <= 0) {
				note = null;
			}
			return;
		}
		if (ticks < APPROACH_TICKS) {
			ticks++;
		}
		if (!screenOpened && ticks >= SCREEN_DELAY) {
			screenOpened = true;
			MinecraftClient.getInstance().setScreen(new NoteScreen(note));
		}
	}

	public static void leave() {
		if (note != null) {
			leaving = true;
			ticks = Math.min(ticks, APPROACH_TICKS);
		}
	}

	public static void clear() {
		note = null;
		leaving = false;
		screenOpened = false;
		ticks = 0;
	}

	public static boolean isActive() {
		return note != null;
	}

	public static float progress(float tickDelta) {
		float raw = MathHelper.clamp((ticks + (leaving ? -tickDelta : tickDelta))
				/ (float) APPROACH_TICKS, 0.0F, 1.0F);
		return raw * raw * (3.0F - 2.0F * raw);
	}

	/**
	 * Où se place l'œil : toujours du côté vers lequel la feuille regarde.
	 *
	 * <p>Au mur, {@code FACING} donne déjà cette direction. Au sol et au
	 * plafond, elle ne sert qu'au sens de lecture, et c'est la verticale qui
	 * porte le recul.
	 */
	public static Vec3d cameraPos() {
		BlockPos pos = note;
		if (pos == null) {
			return Vec3d.ZERO;
		}
		Vec3d centre = Vec3d.ofCenter(pos);
		return switch (face) {
			case FLOOR -> centre.add(0.0, RECUL, 0.0);
			case CEILING -> centre.add(0.0, -RECUL, 0.0);
			case WALL -> centre.add(facing.getOffsetX() * RECUL, 0.0,
					facing.getOffsetZ() * RECUL);
		};
	}

	/**
	 * Le regard.
	 *
	 * <p>Au mur, on fait face à la feuille, donc à l'opposé de son orientation.
	 * À plat, on la survole ou on la contemple d'en dessous, et le lacet garde
	 * le sens de lecture : sans lui, la page serait droite mais le décor
	 * tournerait autour, ce qui donne le tournis.
	 */
	public static float cameraYaw() {
		return face == BlockFace.WALL ? facing.getOpposite().asRotation() : facing.asRotation();
	}

	public static float cameraPitch() {
		return switch (face) {
			case FLOOR -> 88.0F;
			case CEILING -> -88.0F;
			case WALL -> 0.0F;
		};
	}
}
