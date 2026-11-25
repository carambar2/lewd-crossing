package dev.pyrofetch.jn1.client.lab;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pyrofetch.jn1.lab.LidarItem;
import dev.pyrofetch.jn1.lab.VoidSwim;
import dev.pyrofetch.jn1.registry.ModItems;
import dev.pyrofetch.jn1.registry.ModSounds;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ce que sait le lidar du joueur, côté client : les échos reçus du serveur, et
 * le rythme des impulsions.
 *
 * <p>Comme un traqueur de mouvement, il bat : une impulsion toutes les deux
 * secondes, dont le front s'élargit du bas de l'écran vers le bord. Un écho
 * s'allume quand le front l'atteint, puis s'éteint lentement jusqu'au passage
 * suivant. L'impulsion sonne ; l'écho le plus proche répond, d'autant plus aigu
 * qu'il est près.
 *
 * <p>Seule la position des échos vient du serveur, et elle ne bouge pas : c'est
 * l'écran qui les replace à chaque image selon où l'on regarde.
 *
 * <p>Une touche ({@code key.jn1.lidar_3d}, H d'office) fait passer l'écran de
 * l'éventail à la vue 3D et retour. En vue 3D, chaque impulsion relance le
 * relevé du relief ({@link LidarCloud}).
 */
public final class LidarTracker {
	/** Entre deux impulsions, en ticks. */
	public static final int PÉRIODE = 40;
	/** Le temps que met le front pour aller du centre au bord, en ticks. */
	public static final float COURSE = 28.0F;
	/** Le temps que met un écho à s'éteindre après le passage du front, en ticks. */
	public static final float RÉMANENCE = 22.0F;

	private static List<BlockPos> échos = List.of();
	private static long départ;
	private static boolean échoSonné = true;
	/** La vue choisie : elle ne tient qu'au client, et survit au lidar rangé. */
	private static boolean vue3D;
	private static KeyMapping touche;

	private LidarTracker() {
	}

	public static void recevoir(List<BlockPos> nouveaux) {
		échos = nouveaux;
	}

	public static List<BlockPos> échos() {
		return échos;
	}

	/** Le rayon, de 0 à 1, où tombe un écho à cette distance : en racine, pour écarter les proches. */
	public static float rayon(double distance) {
		return (float) Math.sqrt(Math.min(1.0, distance / LidarItem.PORTÉE));
	}

	/**
	 * Où le front de l'impulsion atteint cet écho, de 0 (le joueur) à 1 (le
	 * bord) : selon le rayon de l'éventail, ou dans la vue 3D selon la distance
	 * vraie, rapportée au bord du relevé. L'écran allume l'écho à ce moment-là,
	 * et c'est aussi là que répond le son.
	 */
	public static float front(BlockPos écho, Vec3 moi) {
		Vec3 écart = Vec3.atCenterOf(écho).subtract(moi);
		if (vue3D) {
			return (float) Math.min(1.0, écart.length() / LidarCloud.RAYON);
		}
		return rayon(écart.horizontalDistance());
	}

	/** Vrai quand l'écran montre la vue 3D plutôt que l'éventail. */
	public static boolean vue3D() {
		return vue3D;
	}

	public static void register() {
		touche = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jn1.lidar_3d",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_H, dev.pyrofetch.jn1.client.CameraKeys.CATÉGORIE));
	}

	/** Depuis combien de ticks la dernière impulsion est partie, tickDelta compris. */
	public static float âgeImpulsion(float tickDelta) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return 0.0F;
		}
		return (client.level.getGameTime() - départ) + tickDelta;
	}

	public static boolean tenu(LocalPlayer joueur) {
		return joueur.getMainHandItem().is(ModItems.LIDAR) || joueur.getOffhandItem().is(ModItems.LIDAR);
	}

	/** Le battement, une fois par tick : l'impulsion, et la réponse du plus proche. */
	public static void tick(Minecraft client) {
		LocalPlayer joueur = client.player;
		if (joueur == null || client.level == null) {
			return;
		}
		// La touche ne vaut que lidar en main ; ailleurs, elle est avalée sans effet.
		while (touche != null && touche.consumeClick()) {
			if (tenu(joueur)) {
				vue3D = !vue3D;
				joueur.playSound(ModSounds.LIDAR_PING, 0.25F, 1.6F);
				if (vue3D && VoidSwim.dansLeVide(joueur)) {
					LidarCloud.relever(joueur.blockPosition());
				}
			}
		}
		if (!tenu(joueur) || !VoidSwim.dansLeVide(joueur)) {
			// Sorti du vide, il ne garde rien : les échos d'ailleurs seraient faux.
			if (!VoidSwim.dansLeVide(joueur)) {
				échos = List.of();
				LidarCloud.oublier();
			}
			return;
		}
		long maintenant = client.level.getGameTime();
		if (maintenant - départ >= PÉRIODE) {
			départ = maintenant;
			échoSonné = false;
			joueur.playSound(ModSounds.LIDAR_PING, 0.45F, 1.0F);
			if (vue3D) {
				LidarCloud.relever(joueur.blockPosition());
			}
		}
		if (vue3D) {
			LidarCloud.tick(client.level);
		}
		if (!échoSonné && !échos.isEmpty()) {
			BlockPos proche = échos.getFirst();
			float r = front(proche, joueur.position());
			if (maintenant - départ >= r * COURSE) {
				échoSonné = true;
				joueur.playSound(ModSounds.LIDAR_ECHO, 0.4F, 0.8F + 0.9F * (1.0F - r));
			}
		}
	}
}
