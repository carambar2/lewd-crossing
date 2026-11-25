package dev.pyrofetch.jn1.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Donne accès aux réglages de position et d'orientation de la caméra.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
@Mixin(Camera.class)
public interface CameraAccess {
	@Invoker("setPosition")
	void jn1$poser(Vec3 var1);

	@Invoker("setRotation")
	void jn1$orienter(float var1, float var2);
}
