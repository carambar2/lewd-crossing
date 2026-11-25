package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.terminal.NoteSession;
import dev.pyrofetch.jn1.client.terminal.TerminalSession;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Amène la caméra devant ce qu'on consulte : l'écran d'un terminal, ou la
 * feuille d'une note — ou la place dans l'objectif d'un appareil posé sur son
 * trépied.
 *
 * <p>Aucune API ne permet de déplacer la caméra sans passer par une entité, et
 * en attacher une pour ça poserait plus de problèmes qu'elle n'en résout —
 * synchronisation, collisions, monture. On se greffe donc à la fin du calcul de
 * la caméra pour en écraser la pose, sans rien changer au joueur lui-même.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPos(Vec3d pos);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void moveBy(float x, float y, float z);

	@Shadow
	private float clipToSpace(float desiredCameraDistance) {
		throw new AssertionError();
	}

	@Inject(method = "update", at = @At("TAIL"))
	private void jn1$terminalView(BlockView area, Entity focusedEntity, boolean thirdPerson,
	                              boolean inverseView, float tickDelta, CallbackInfo info) {
		Camera self = (Camera) (Object) this;

		// Dans une scène, à la place de steve : l'œil est dans sa tête, le
		// regard reste celui du joueur. À la troisième personne, le recul du
		// jeu part de là plutôt que du personnage, resté aux pieds de la compagne.
		net.minecraft.util.math.Vec3d œil = dev.pyrofetch.jn1.client.render.SceneView.eye();
		if (œil != null) {
			setPos(œil);
			if (thirdPerson) {
				moveBy(-jn1$horsDElle(self, œil, clipToSpace(4.0F)), 0.0F, 0.0F);
			}
			return;
		}

		// L'appareil posé sur son trépied : la vue part de l'objectif, sans
		// glissement — on regarde par lui, pas vers lui.
		if (dev.pyrofetch.jn1.client.camera.CameraSession.vueDéportée()) {
			setPos(dev.pyrofetch.jn1.client.camera.CameraSession.position());
			setRotation(dev.pyrofetch.jn1.client.camera.CameraSession.lacetVue(),
					dev.pyrofetch.jn1.client.camera.CameraSession.tangageVue());
			return;
		}

		// Les deux sessions ne se recouvrent pas : on ne lit pas une note en
		// étant assis devant un terminal. Le même glissement les sert donc,
		// seule la pose visée diffère.
		if (TerminalSession.isActive()) {
			jn1$glisser(self, TerminalSession.progress(tickDelta), TerminalSession.cameraPos(),
					TerminalSession.cameraYaw(), TerminalSession.cameraPitch());
		} else if (NoteSession.isActive()) {
			jn1$glisser(self, NoteSession.progress(tickDelta), NoteSession.cameraPos(),
					NoteSession.cameraYaw(), NoteSession.cameraPitch());
		}
	}

	/**
	 * Le recul de la troisième personne s'arrête aussi devant elle : vue de face,
	 * la caméra partait de la tête du partenaire droit dans son corps. Sa boîte
	 * suit la pose de la scène ; si l'œil y est déjà, rien à couper.
	 */
	private static float jn1$horsDElle(Camera self, Vec3d œil, float recul) {
		dev.pyrofetch.jn1.entity.CompanionEntity elle = dev.pyrofetch.jn1.client.render.SceneView.participating();
		if (elle == null || recul <= 0.0F) {
			return recul;
		}
		org.joml.Vector3f avant = self.getHorizontalPlane();
		Vec3d arrière = œil.subtract(avant.x() * recul, avant.y() * recul, avant.z() * recul);
		net.minecraft.util.math.Box boîte = elle.getBoundingBox();
		if (boîte.contains(œil)) {
			return recul;
		}
		return boîte.raycast(œil, arrière)
				.map(entrée -> (float) Math.max(0.0, entrée.distanceTo(œil) - 0.2))
				.orElse(recul);
	}

	private void jn1$glisser(Camera self, float amount, Vec3d cible, float yaw, float pitch) {
		setPos(self.getPos().lerp(cible, amount));
		setRotation(MathHelper.lerpAngleDegrees(amount, self.getYaw(), yaw),
				MathHelper.lerp(amount, self.getPitch(), pitch));
	}
}
