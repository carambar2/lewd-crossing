package dev.pyrofetch.jn1.client.lab;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

/**
 * L'ambiance du vide : ni astre, ni nuage, une brume noire.
 *
 * <p>Le ciel lui-même — le couloir entre les univers — est dessiné par
 * {@link VoidSky} ; ce qui suit ne règle que la brume et la lumière, qui restent
 * noires : le couloir est au loin, et le vide ne renvoie toujours rien.
 *
 * <p>Aucune des ambiances du jeu ne convenait. Celle du Nether teinte tout de
 * rouge, celle de l'End pose un ciel violet et des étoiles. Ici la brume est
 * noire et constante : ce qui est proche — laboratoires, épaves — ne se détache
 * que parce qu'il est éclairé, et se fond dans le noir en s'éloignant, devant
 * le couloir.
 */
public class VoidEffects extends DimensionEffects {
	public VoidEffects() {
		// Pas de nuages (Float.NaN), pas d'éclaircissement de la lumière,
		// et un ciel de type NONE : ni soleil, ni lune, ni étoiles.
		super(Float.NaN, false, DimensionEffects.SkyType.NONE, false, false);
	}

	@Override
	public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
		return Vec3d.ZERO;
	}

	@Override
	public boolean useThickFog(int camX, int camY) {
		return false;
	}

	/** Assombrit le rendu : le vide ne renvoie aucune lumière. */
	@Override
	public boolean isDarkened() {
		return true;
	}
}
