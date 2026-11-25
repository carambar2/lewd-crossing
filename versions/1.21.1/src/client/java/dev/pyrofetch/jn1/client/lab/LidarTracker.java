package dev.pyrofetch.jn1.client.lab;

import dev.pyrofetch.jn1.lab.LidarItem;
import dev.pyrofetch.jn1.lab.VoidSwim;
import dev.pyrofetch.jn1.registry.ModItems;
import dev.pyrofetch.jn1.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.glfw.GLFW;

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
	private static KeyBinding touche;

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
	public static float front(BlockPos écho, Vec3d moi) {
		Vec3d écart = Vec3d.ofCenter(écho).subtract(moi);
		if (vue3D) {
			return (float) Math.min(1.0, écart.length() / LidarCloud.RAYON);
		}
		return rayon(écart.horizontalLength());
	}

	/** Vrai quand l'écran montre la vue 3D plutôt que l'éventail. */
	public static boolean vue3D() {
		return vue3D;
	}

	public static void register() {
		touche = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.jn1.lidar_3d", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.jn1"));
	}

	/** Depuis combien de ticks la dernière impulsion est partie, tickDelta compris. */
	public static float âgeImpulsion(float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return 0.0F;
		}
		return (client.world.getTime() - départ) + tickDelta;
	}

	public static boolean tenu(ClientPlayerEntity joueur) {
		return joueur.getMainHandStack().isOf(ModItems.LIDAR) || joueur.getOffHandStack().isOf(ModItems.LIDAR);
	}

	/** Le battement, une fois par tick : l'impulsion, et la réponse du plus proche. */
	public static void tick(MinecraftClient client) {
		ClientPlayerEntity joueur = client.player;
		if (joueur == null || client.world == null) {
			return;
		}
		// La touche ne vaut que lidar en main ; ailleurs, elle est avalée sans effet.
		while (touche != null && touche.wasPressed()) {
			if (tenu(joueur)) {
				vue3D = !vue3D;
				joueur.playSound(ModSounds.LIDAR_PING, 0.25F, 1.6F);
				if (vue3D && VoidSwim.dansLeVide(joueur)) {
					LidarCloud.relever(joueur.getBlockPos());
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
		long maintenant = client.world.getTime();
		if (maintenant - départ >= PÉRIODE) {
			départ = maintenant;
			échoSonné = false;
			joueur.playSound(ModSounds.LIDAR_PING, 0.45F, 1.0F);
			if (vue3D) {
				LidarCloud.relever(joueur.getBlockPos());
			}
		}
		if (vue3D) {
			LidarCloud.tick(client.world);
		}
		if (!échoSonné && !échos.isEmpty()) {
			BlockPos proche = échos.getFirst();
			float r = front(proche, joueur.getPos());
			if (maintenant - départ >= r * COURSE) {
				échoSonné = true;
				joueur.playSound(ModSounds.LIDAR_ECHO, 0.4F, 0.8F + 0.9F * (1.0F - r));
			}
		}
	}
}
