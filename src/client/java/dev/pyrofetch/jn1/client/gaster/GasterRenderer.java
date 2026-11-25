package dev.pyrofetch.jn1.client.gaster;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.mesh.DessinMaillage;
import dev.pyrofetch.jn1.client.mesh.Maillage;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.function.Predicate;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Gaster, son vrai modèle, lisse : GeckoLib pose les os sans rien dessiner
 * (ils n'ont pas de cubes), et l'on dessine ensuite le maillage exporté de
 * Blender, déformé par eux ({@link DessinMaillage}).
 *
 * <p>Pendant l'adieu, ses pupilles s'allument. Elles sont dans le fichier de Skyguy555, deux petites
 * sphères au fond des orbites ({@code pupils}, {@code pupils.001}), qui y
 * portaient un matériau émissif blanc, remplacé par la planche — d'où, en jeu,
 * deux points sombres qu'on ne voyait pas. Elles sont alors redessinées en
 * lumière, plus petites que la sphère du fichier, dans deux halos pâles.
 */
public class GasterRenderer extends GeoEntityRenderer<GasterEntity> {
	private static final Identifier MAILLAGE = JN1.id("mesh/gaster.jmesh");
	private static final Identifier LUEUR = JN1.id("textures/entity/gaster/lueur.png");
	private static final Predicate<Maillage.Partie> PUPILLES = p -> p.nom().startsWith("pupils");

	/**
	 * La pupille, puis ses halos, en proportion de la sphère du fichier. À bout
	 * portant, 1,5 / 2,6 / 4 faisaient des yeux énormes (0.56.3).
	 */
	private static final float CŒUR = 0.7F;
	private static final float HALO = 1.0F;
	private static final float HALO_LARGE = 1.4F;

	/**
	 * Pendant l'adieu, de l'œil de la caméra au milieu de ses yeux : le devant du
	 * masque est alors à un dixième de bloc, ses bords à un quart — il remplit
	 * la hauteur, et les côtés sont noyés de noir ({@code GasterAdieu}). Plus
	 * près, la caméra (qui ne voit rien à moins de 0,05) entrerait dans le masque.
	 */
	private static final float COLLÉ = 0.21F;
	/** Ce dont sa tête bouge encore à chaque image, en blocs, autour de ce point. */
	private static final float SECOUSSE = 0.006F;

	private final DessinMaillage dessin = new DessinMaillage();

	public GasterRenderer(EntityRendererFactory.Context context) {
		super(context, new GasterModel());
		this.shadowRadius = 0.6F;
	}

	/**
	 * Pendant l'adieu, il fait face à la caméra, où qu'elle se tourne : le
	 * corps prend son cap à chaque image (l'inclinaison, elle, est dans
	 * {@link #coller}).
	 */
	@Override
	public void render(GasterEntity gaster, float entityYaw, float partialTick, MatrixStack poseStack,
	                   VertexConsumerProvider bufferSource, int packedLight) {
		if (gaster.enAdieu()) {
			float cap = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw() + 180.0F;
			gaster.bodyYaw = gaster.prevBodyYaw = cap;
			gaster.headYaw = gaster.prevHeadYaw = cap;
		}
		super.render(gaster, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	/** Collé à la caméra, il ne sort jamais du champ : jamais écarté par le tri des entités visibles. */
	@Override
	public boolean shouldRender(GasterEntity gaster, Frustum frustum, double x, double y, double z) {
		return gaster.enAdieu() || super.shouldRender(gaster, frustum, x, y, z);
	}

	/**
	 * Le déplacement qui pose le milieu de ses yeux à {@link #COLLÉ} devant la
	 * caméra, le visage tourné vers elle. Recalculé à chaque image sur les os
	 * que GeckoLib vient de poser : son animation de repos balance la tête, et à
	 * cette distance quelques pixels de balancement sortaient le visage de
	 * l'écran. Le tremblement de la tête ({@code GasterModel}) tourne donc le
	 * visage autour de ses yeux, et une secousse d'un rien s'y ajoute.
	 */
	@Nullable
	private Matrix4f coller() {
		Vector3f gauche = this.dessin.pivot("bip_eye_L");
		Vector3f droit = this.dessin.pivot("bip_eye_R");
		if (gauche == null || droit == null) {
			return null;
		}
		Vector3f yeux = gauche.add(droit).mul(0.5F);
		Camera caméra = MinecraftClient.getInstance().gameRenderer.getCamera();
		Vec3d regard = Vec3d.fromPolar(caméra.getPitch(), caméra.getYaw());
		Vector3f devant = new Vector3f((float) regard.x, (float) regard.y, (float) regard.z);
		Vector3f but = new Vector3f(devant).mul(COLLÉ).add(
				(float) (Math.random() - 0.5) * 2 * SECOUSSE, (float) (Math.random() - 0.5) * 2 * SECOUSSE,
				(float) (Math.random() - 0.5) * 2 * SECOUSSE);
		// Le corps a déjà le cap de la caméra : reste à incliner le visage vers elle.
		Vector3f àPlat = new Vector3f(-devant.x, 0.0F, -devant.z);
		Quaternionf incline = new Quaternionf();
		if (àPlat.lengthSquared() > 1e-4F) {
			incline.rotationTo(àPlat.normalize(), new Vector3f(devant).negate());
		}
		return new Matrix4f().translation(but).rotate(incline).translate(yeux.negate());
	}

	@Override
	public void renderCubesOfBone(MatrixStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
	                              int packedOverlay, int colour) {
		this.dessin.retenir(bone, poseStack);
		super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
	}

	@Override
	public void actuallyRender(MatrixStack poseStack, GasterEntity gaster, BakedGeoModel model,
	                           @Nullable RenderLayer renderType, VertexConsumerProvider bufferSource,
	                           @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
	                           int packedLight, int packedOverlay, int colour) {
		this.dessin.oublier();
		super.actuallyRender(poseStack, gaster, model, renderType, bufferSource, buffer, isReRender, partialTick,
				packedLight, packedOverlay, colour);
		if (!isReRender) {
			Maillage maillage = Maillage.de(MAILLAGE);
			if (maillage == null) {
				return;
			}
			boolean lueur = gaster.enAdieu();
			Matrix4f collé = lueur ? this.coller() : null;
			this.dessin.dessiner(maillage, bufferSource, packedLight, packedOverlay, colour, lueur ? PUPILLES : null, collé);
			if (lueur) {
				this.dessin.dessinerLueur(maillage, bufferSource, PUPILLES, LUEUR, HALO_LARGE, 0xFF1C2630, collé);
				this.dessin.dessinerLueur(maillage, bufferSource, PUPILLES, LUEUR, HALO, 0xFF4A5A68, collé);
				this.dessin.dessinerLueur(maillage, bufferSource, PUPILLES, LUEUR, CŒUR, 0xFFFFFFFF, collé);
			}
		}
	}
}
