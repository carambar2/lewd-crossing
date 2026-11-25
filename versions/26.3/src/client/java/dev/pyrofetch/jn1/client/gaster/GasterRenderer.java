package dev.pyrofetch.jn1.client.gaster;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.mesh.DessinMaillage;
import dev.pyrofetch.jn1.client.mesh.Maillage;
import dev.pyrofetch.jn1.gaster.GasterEntity;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Au moment de soumettre son dessin
 * ({@link #submitRenderTasks}), on parcourt les os posés comme GeckoLib
 * l'aurait fait pour dessiner leurs cubes, en retenant leurs matrices ; le
 * maillage est déformé aussitôt, et soumis.
 */
public class GasterRenderer extends GeoEntityRenderer<GasterEntity, LivingEntityRenderState> {
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

	private final GasterModel modèle;
	private final DessinMaillage dessin = new DessinMaillage();

	public GasterRenderer(EntityRendererProvider.Context context) {
		this(context, new GasterModel());
	}

	private GasterRenderer(EntityRendererProvider.Context context, GasterModel modèle) {
		super(context, modèle);
		this.modèle = modèle;
		this.shadowRadius = 0.6F;
	}

	/**
	 * Pendant l'adieu, il fait face à la caméra, où qu'elle se tourne : le
	 * corps prend son cap à chaque image (l'inclinaison, elle, est dans
	 * {@link #coller}).
	 */
	@Override
	public void extractRenderState(GasterEntity gaster, LivingEntityRenderState état, float partialTick) {
		if (gaster.enAdieu()) {
			float cap = Minecraft.getInstance().gameRenderer.mainCamera().yRot() + 180.0F;
			gaster.yBodyRot = gaster.yBodyRotO = cap;
			gaster.yHeadRot = gaster.yHeadRotO = cap;
		}
		super.extractRenderState(gaster, état, partialTick);
	}

	@Override
	public void addRenderData(GasterEntity gaster, @Nullable Void relatedObject, LivingEntityRenderState état,
	                          float partialTick) {
		état.addGeckolibData(GasterModel.ÉTAT, new GasterModel.État(gaster.enAdieu(), gaster.épie(),
				gaster.tickCount, gaster.getId()));
	}

	/** Collé à la caméra, il ne sort jamais du champ : jamais écarté par le tri des entités visibles. */
	@Override
	public boolean shouldRender(GasterEntity gaster, Frustum frustum, double x, double y, double z, float partialTicks) {
		return gaster.enAdieu() || super.shouldRender(gaster, frustum, x, y, z, partialTicks);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots os) {
		GasterModel.État gaster = info.getGeckolibData(GasterModel.ÉTAT);
		if (gaster != null) {
			// Les signes des os de GeckoLib sont l'opposé de ceux du jeu.
			this.modèle.poser(os, gaster, -info.renderState().yRot, -info.renderState().xRot);
		}
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
	private Matrix4f coller(CameraRenderState caméra) {
		Vector3f gauche = this.dessin.pivot("bip_eye_L");
		Vector3f droit = this.dessin.pivot("bip_eye_R");
		if (gauche == null || droit == null) {
			return null;
		}
		Vector3f yeux = gauche.add(droit).mul(0.5F);
		Vec3 regard = Vec3.directionFromRotation(caméra.xRot, caméra.yRot);
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

	/** Retient la matrice de chaque os, telle que GeckoLib la prépare pour ses cubes. */
	private void retenir(RenderPassInfo<LivingEntityRenderState> info, PoseStack pile, GeoBone os) {
		pile.pushPose();
		RenderUtil.prepMatrixForBoneAndUpdateListeners(pile, os, info);
		this.dessin.retenir(os, pile);
		if (os.frameSnapshot == null || !os.frameSnapshot.areChildrenHidden()) {
			for (GeoBone fils : os.children()) {
				this.retenir(info, pile, fils);
			}
		}
		pile.popPose();
	}

	@Override
	public void submitRenderTasks(RenderPassInfo<LivingEntityRenderState> info, OrderedSubmitNodeCollector renderTasks,
	                              @Nullable RenderType renderType) {
		if (renderType == null || info.model().isMissingno()) {
			super.submitRenderTasks(info, renderTasks, renderType);
			return;
		}
		Maillage maillage = Maillage.de(MAILLAGE);
		if (maillage == null) {
			return;
		}
		PoseStack pile = info.poseStack();
		this.dessin.oublier();
		info.renderPosed(() -> {
			for (GeoBone os : info.model().topLevelBones()) {
				this.retenir(info, pile, os);
			}
		});
		GasterModel.État gaster = info.getGeckolibData(GasterModel.ÉTAT);
		boolean lueur = gaster != null && gaster.adieu();
		Matrix4f collé = lueur ? this.coller(info.cameraState()) : null;
		int lumière = info.packedLight();
		this.dessin.dessiner(maillage, renderTasks, pile, lumière, info.packedOverlay(), info.renderColor(),
				lueur ? PUPILLES : null, collé);
		if (lueur) {
			this.dessin.dessinerLueur(maillage, renderTasks, pile, PUPILLES, LUEUR, HALO_LARGE, 0xFF1C2630, collé);
			this.dessin.dessinerLueur(maillage, renderTasks, pile, PUPILLES, LUEUR, HALO, 0xFF4A5A68, collé);
			this.dessin.dessinerLueur(maillage, renderTasks, pile, PUPILLES, LUEUR, CŒUR, 0xFFFFFFFF, collé);
		}
	}
}
