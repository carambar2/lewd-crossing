package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.entity.AuraEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * L'allure d'une projection : Aura, et qui a pris son apparence.
 *
 * <p>Translucide, pleinement lumineuse — elle se voit dans le noir, sans ombre
 * au sol —, avec un battement lent de la transparence et, de temps en temps, un
 * parasite : quelques images plus pâles, décalées d'un cheveu. Le rythme des
 * parasites se tire de l'identifiant de l'entité : deux projections ne
 * clignotent pas ensemble. En veille, elle n'est qu'un reflet.
 */
public final class Hologram {
	private static final float VEILLE = 0.22F;
	private static final float ÉVEILLÉE = 0.64F;
	/** Un parasite tous les ~ 90 ticks, qui dure 3 ticks. */
	private static final int PÉRIODE = 90;
	private static final int PARASITE = 3;

	private Hologram() {
	}

	/** L'opacité à cet instant. */
	public static float alpha(Entity entité, float partialTick) {
		float t = entité.age + partialTick;
		boolean veille = entité instanceof AuraEntity aura && aura.isDormant();
		float base = veille ? VEILLE : ÉVEILLÉE;
		float battement = MathHelper.sin(t * 0.12F) * (veille ? 0.06F : 0.05F);
		float alpha = base + battement;
		if (parasite(entité)) {
			alpha *= 0.45F;
		}
		return MathHelper.clamp(alpha, 0.05F, 1.0F);
	}

	/** Le décalage de côté pendant un parasite, en blocs ; nul le reste du temps. */
	public static float décalage(Entity entité) {
		if (!parasite(entité)) {
			return 0.0F;
		}
		return ((entité.age * 7 + entité.getId()) % 3 - 1) * 0.05F;
	}

	private static boolean parasite(Entity entité) {
		int phase = Math.floorMod(entité.age + entité.getId() * 37, PÉRIODE + (entité.getId() % 23));
		return phase < PARASITE;
	}
}
