package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.client.terminal.NoteSession;
import dev.pyrofetch.jn1.client.terminal.TerminalSession;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
 *
 * <p>26.3 : la pose se calcule dans {@code alignWithEntity}, appelée par
 * {@code update} juste avant le champ de vision et le volume de visibilité :
 * on se greffe à sa fin, pour que tout ce qui suit parte de notre pose.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void move(float x, float y, float z);

	@Shadow
	private float getMaxZoom(float desiredCameraDistance) {
		throw new AssertionError();
	}

	@Shadow
	private boolean detached;

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void jn1$terminalView(float tickDelta, CallbackInfo info) {
		Camera self = (Camera) (Object) this;
		boolean thirdPerson = this.detached;

		// Dans une scène, à la place de steve : l'œil est dans sa tête, le
		// regard reste celui du joueur. À la troisième personne, le recul du
		// jeu part de là plutôt que du personnage, resté aux pieds de la compagne.
		net.minecraft.world.phys.Vec3 œil = dev.pyrofetch.jn1.client.render.SceneView.eye();
		if (œil != null) {
			setPosition(œil);
			if (thirdPerson) {
				move(-jn1$horsDElle(self, œil, getMaxZoom(4.0F)), 0.0F, 0.0F);
			}
			return;
		}

		// L'appareil posé sur son trépied : la vue part de l'objectif, sans
		// glissement — on regarde par lui, pas vers lui.
		if (dev.pyrofetch.jn1.client.camera.CameraSession.vueDéportée()) {
			setPosition(dev.pyrofetch.jn1.client.camera.CameraSession.position());
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
	private static float jn1$horsDElle(Camera self, Vec3 œil, float recul) {
		dev.pyrofetch.jn1.entity.CompanionEntity elle = dev.pyrofetch.jn1.client.render.SceneView.participating();
		if (elle == null || recul <= 0.0F) {
			return recul;
		}
		org.joml.Vector3fc avant = self.forwardVector();
		Vec3 arrière = œil.subtract(avant.x() * recul, avant.y() * recul, avant.z() * recul);
		net.minecraft.world.phys.AABB boîte = elle.getBoundingBox();
		if (boîte.contains(œil)) {
			return recul;
		}
		return boîte.clip(œil, arrière)
				.map(entrée -> (float) Math.max(0.0, entrée.distanceTo(œil) - 0.2))
				.orElse(recul);
	}

	private void jn1$glisser(Camera self, float amount, Vec3 cible, float yaw, float pitch) {
		setPosition(self.position().lerp(cible, amount));
		setRotation(Mth.rotLerp(amount, self.yRot(), yaw),
				Mth.lerp(amount, self.xRot(), pitch));
	}
}
