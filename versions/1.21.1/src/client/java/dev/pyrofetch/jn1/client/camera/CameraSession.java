package dev.pyrofetch.jn1.client.camera;

import dev.pyrofetch.jn1.block.CameraStandBlockEntity;
import dev.pyrofetch.jn1.network.CameraAimPayload;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * La séance de prise de vue, côté client : viseur ouvert ou non, à main levée
 * ou depuis un trépied, cadrage, retardateur, compte à rebours et éclair.
 * Tant qu'elle dure, le clic déclenche au lieu de frapper.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class CameraSession {
	private static final int POSE = 14;
	private static final int ÉCLAIR = 4;
	private static boolean active;
	private static boolean surPied;
	@Nullable
	private static BlockPos pied;
	private static PhotoFormat format = PhotoFormat.PAYSAGE;
	/** Le format de l'appareil en main : un trépied a le sien, qui ne doit pas l'écraser. */
	private static PhotoFormat formatEnMain = PhotoFormat.PAYSAGE;
	/** Le format du trépied qui va déclencher au retardateur. */
	private static PhotoFormat poseFormat = PhotoFormat.PAYSAGE;
	/**
	 * Le clic droit a été relâché depuis l'ouverture. Tenu, le jeu rappelle
	 * l'usage de l'objet tous les quatre ticks : le viseur s'ouvrait puis se
	 * refermait aussitôt, et clignotait tant qu'on tenait.
	 */
	private static boolean relâché = true;
	private static int retard;
	private static float lacet;
	private static float tangage;
	private static int compteÀRebours = -1;
	@Nullable
	private static BlockPos armé;
	private static boolean verrouClic;
	private static int pose;
	private static int éclair;
	@Nullable
	private static BlockPos poseDepuis;
	private static float poseLacet;
	private static float poseTangage;
	@Nullable
	private static Perspective perspectiveAvant;

	private CameraSession() {
	}

	public static void begin(boolean posé, BlockPos position) {
		if (active && surPied == posé) {
			leave();
		} else {
			active = true;
			surPied = posé;
			pied = posé ? position : null;
			if (posé) {
				MinecraftClient client = MinecraftClient.getInstance();
				if (client.world != null && client.world.getBlockEntity(position) instanceof CameraStandBlockEntity bloc) {
					lacet = bloc.lacet();
					tangage = bloc.tangage();
					retard = bloc.retard();
					format = bloc.format();
				} else {
					// Le trépied n'est pas (encore) chez le client : rien à viser.
					active = false;
					surPied = false;
					pied = null;
					return;
				}
			} else {
				format = formatEnMain;
			}

			relâché = false;
			son(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.4F);
		}
	}

	public static void leave() {
		if (active && surPied && pied != null) {
			ClientPlayNetworking.send(new CameraAimPayload(pied, lacet, tangage, retard, format));
		}

		active = false;
		surPied = false;
		pied = null;
	}

	public static void clear() {
		relâché = true;
		active = false;
		surPied = false;
		pied = null;
		compteÀRebours = -1;
		armé = null;
		verrouClic = false;
		pose = 0;
		éclair = 0;
		poseDepuis = null;
		rendrePerspective();
	}

	public static boolean isActive() {
		return active;
	}

	/** L'usage de l'objet est-il bloqué ? Au trépied toujours, en main tant que le clic qui a ouvert est tenu. */
	public static boolean bloqueLUsage() {
		return active && (surPied || !relâché);
	}

	public static boolean verrouilleLeClic() {
		return verrouClic;
	}

	public static boolean surPied() {
		return active && surPied;
	}

	public static boolean bloqueLeDéplacement() {
		return active && surPied;
	}

	public static PhotoFormat format() {
		return format;
	}

	public static int retard() {
		return retard;
	}

	public static int compteÀRebours() {
		return compteÀRebours;
	}

	public static float éclair() {
		return éclair / 4.0F;
	}

	public static void formatSuivant() {
		format = format.suivant();
		if (!surPied) {
			formatEnMain = format;
		}
		son(SoundEvents.UI_BUTTON_CLICK.value(), 1.8F);
	}

	public static void retardSuivant() {
		if (surPied) {
			retard = CameraStandBlockEntity.retardSuivant(retard);
			son(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F);
		}
	}

	public static void déclencher() {
		if (active) {
			verrouClic = true;
			if (surPied && retard != 0) {
				armé = pied;
				compteÀRebours = retard * 20;
				leave();
				son(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1.0F);
			} else {
				PhotoCapture.demander(format, null, 0.0F, 0.0F);
				éclair = 4;
				son(SoundEvents.BLOCK_DISPENSER_FAIL, 1.6F);
			}
		}
	}

	public static void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (verrouClic && !client.options.attackKey.isPressed()) {
			verrouClic = false;
		}
		if (!client.options.useKey.isPressed()) {
			relâché = true;
		}
		if (active && !encoreValide(client)) {
			leave();
		}

		if (éclair > 0) {
			éclair--;
		}

		if (pose > 0 && --pose == 0) {
			poseDepuis = null;
			rendrePerspective();
		}

		if (compteÀRebours >= 0) {
			if (compteÀRebours % 20 == 0 && compteÀRebours > 0) {
				son(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), compteÀRebours <= 20 ? 1.8F : 1.0F);
			}

			if (--compteÀRebours <= 0) {
				compteÀRebours = -1;
				déclencherDepuisLePied();
				armé = null;
			}
		}
	}

	/**
	 * Le viseur suit l'appareil : il se ferme si le joueur meurt, lâche ou
	 * range l'appareil, ou si le trépied est cassé. Il restait sinon ouvert,
	 * l'interface masquée et le clic gauche détourné, jusqu'à la déconnexion.
	 */
	private static boolean encoreValide(MinecraftClient client) {
		if (client.player == null || client.world == null || !client.player.isAlive()) {
			return false;
		}
		if (surPied) {
			return pied != null && client.world.getBlockEntity(pied) instanceof CameraStandBlockEntity;
		}
		return client.player.getMainHandStack().isOf(dev.pyrofetch.jn1.registry.ModItems.CAMERA)
				|| client.player.getOffHandStack().isOf(dev.pyrofetch.jn1.registry.ModItems.CAMERA);
	}

	private static void déclencherDepuisLePied() {
		BlockPos appareil = poseDepuisLeBloc();
		if (appareil != null) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (perspectiveAvant == null) {
				perspectiveAvant = client.options.getPerspective();
				client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
			}

			poseDepuis = appareil;
			pose = 14;
			éclair = 4;
			PhotoCapture.demander(poseFormat, appareil, poseLacet, poseTangage);
			son(SoundEvents.BLOCK_DISPENSER_FAIL, 1.6F);
		}
	}

	@Nullable
	private static BlockPos poseDepuisLeBloc() {
		MinecraftClient client = MinecraftClient.getInstance();
		BlockPos position = armé;
		if (position != null && client.world != null) {
			if (client.world.getBlockEntity(position) instanceof CameraStandBlockEntity bloc) {
				poseLacet = bloc.lacet();
				poseTangage = bloc.tangage();
				poseFormat = bloc.format();
				return position;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static void rendrePerspective() {
		if (perspectiveAvant != null) {
			MinecraftClient.getInstance().options.setPerspective(perspectiveAvant);
			perspectiveAvant = null;
		}
	}

	public static boolean vueDéportée() {
		return active && surPied && pied != null || poseDepuis != null;
	}

	/** Le trépied par lequel on regarde, ou rien. */
	@Nullable
	public static BlockPos pied() {
		return poseDepuis != null ? poseDepuis : active && surPied ? pied : null;
	}

	public static Vec3d position() {
		BlockPos position = poseDepuis != null ? poseDepuis : pied;
		return position == null ? Vec3d.ZERO : new Vec3d(position.getX() + 0.5, position.getY() + 0.62, position.getZ() + 0.5);
	}

	public static float lacetVue() {
		return poseDepuis != null ? poseLacet : lacet;
	}

	public static float tangageVue() {
		return poseDepuis != null ? poseTangage : tangage;
	}

	public static void tourner(double dx, double dy) {
		lacet = MathHelper.wrapDegrees(lacet + (float)dx * 0.15F);
		tangage = MathHelper.clamp(tangage + (float)dy * 0.15F, -89.0F, 89.0F);
	}

	private static void son(SoundEvent événement, float hauteur) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(événement, hauteur, 0.5F));
		}
	}
}
