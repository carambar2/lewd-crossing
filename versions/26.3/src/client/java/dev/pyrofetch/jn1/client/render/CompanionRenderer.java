package dev.pyrofetch.jn1.client.render;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pyrofetch.jn1.entity.AuraEntity;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.shape.Shapes;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Le rendu d'une compagne (GeckoLib).
 *
 * <p><strong>GeckoLib 5 et 26.3.</strong> Le rendu se fait en deux temps :
 * à l'image, on relève l'état de la compagne ({@link #addRenderData} : sa
 * tenue, sa scène, ses formes, rangées sous {@link #SCÈNE} et
 * {@link CompanionModel#APPARENCE}) ; puis on « soumet » des dessins que le jeu
 * exécute plus tard, sans l'entité. Les os cuits se dessinent eux-mêmes chez
 * GeckoLib 5, sans crochet par os ni par cube : on soumet donc nos propres
 * dessins, qui parcourent les os ({@link #dessinerOs}) comme le faisait
 * {@code renderRecursively} et dessinent chaque cube avec nos normales
 * ({@link CompanionShading}). Un dessin par planche : le corps, le skin du
 * partenaire sur {@code steve}, la pièce de scène, et le calque de traces
 * ({@link TracesLayer}) — là où GeckoLib 4 changeait de tampon au milieu du
 * parcours.
 */
public class CompanionRenderer<T extends CompanionEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {

	/**
	 * Ce que le rendu relève d'une compagne à chaque image : tout ce que ses
	 * dessins et ses retouches d'os lisaient sur l'entité.
	 *
	 * @param id                numéro d'entité (le point des yeux, {@link SceneView})
	 * @param partenaire        un joueur tient la place de {@code steve}
	 * @param pièce             la pièce de scène est à dessiner, avec sa planche
	 * @param pièceDuPartenaire la pièce paraît sur le mannequin : réglée, partenaire homme
	 * @param peau              la planche du skin du partenaire, s'il y en a un
	 * @param skinFin           son skin a les bras fins
	 * @param participe         le joueur local tient cette place-là
	 * @param premièrePersonne  et regarde à la première personne
	 * @param enScène           une scène joue : l'animation tient la tête
	 * @param décalage          le décalage de côté d'un parasite d'hologramme, en blocs
	 */
	public record Scène(CompanionKind kind, int id, boolean partenaire, boolean pièce, boolean pièceDuPartenaire,
	                    @Nullable Identifier peau, boolean skinFin, boolean participe, boolean premièrePersonne,
	                    int poitrine, int croupe, boolean enScène, int traces, boolean invisible,
	                    boolean hologramme, float décalage) {
	}

	public static final DataTicket<Scène> SCÈNE = DataTicket.create("jn1_scene", Scène.class);

	/**
	 * Les os du mannequin qui ne paraissent jamais, quel que soit le réglage :
	 * ceux qui accompagnent la pièce sous le torse. La 1.21.1 les masquait dans
	 * tous les cas (mannequin caché, pièce seule, ou partenaire) ; on les saute
	 * ici avec leur descendance, sans dépendre des instantanés de l'image.
	 */
	private static final java.util.Set<String> JAMAIS = java.util.Set.of("ballL", "ballR");

	/** La planche d'un dessin : celle de la fille, le skin du partenaire, celle de la pièce. */
	private enum Couche { CORPS, PEAU, PIÈCE }

	private final CompanionModel<T> modèle;

	/** Lissage des normales et vraie normale des cubes plats : voir {@link CompanionShading}. */
	private final CompanionShading shading = new CompanionShading();

	public CompanionRenderer(EntityRendererProvider.Context context) {
		this(context, new CompanionModel<>());
	}

	protected CompanionRenderer(EntityRendererProvider.Context context, CompanionModel<T> model) {
		super(context, model);
		this.modèle = model;
		// Le cadeau qu'elle vient de recevoir, tenu à la main.
		this.withRenderLayer(new GiftLayer<>(this, context.getItemModelResolver()));
		// Ses traces de cum, par-dessus sa planche.
		this.withRenderLayer(new TracesLayer<>(this));
		this.shadowRadius = 0.4F;
	}

	@Override
	public void addRenderData(T animatable, @Nullable Void relatedObject, LivingEntityRenderState état, float partialTick) {
		CompanionKind kind = animatable.kind();
		état.addGeckolibData(CompanionModel.APPARENCE, new CompanionModel.Apparence(kind, animatable.getOutfit()));
		boolean partenaire = animatable.showsScenePartner();
		PlayerSkin peau = partenaire ? SceneView.skin(animatable) : null;
		boolean hologramme = animatable instanceof AuraEntity aura && aura.isHologram();
		état.addGeckolibData(SCÈNE, new Scène(kind, animatable.getId(), partenaire, animatable.showsSceneProp(),
				animatable.isScenePropShown() && !animatable.isScenePartnerFemale(),
				peau == null ? null : peau.body().texturePath(), peau != null && peau.model() == PlayerModelType.SLIM,
				SceneView.participating() == animatable, SceneView.firstPerson(),
				animatable.getShape(Shapes.Part.BOOBS), animatable.getShape(Shapes.Part.BOOTY), animatable.hasScene(),
				animatable.getTraces(), animatable.isInvisible(),
				hologramme, hologramme ? Hologram.décalage(animatable) : 0.0F));
	}

	@Override
	public void extractRenderState(T entity, LivingEntityRenderState état, float partialTick) {
		super.extractRenderState(entity, état, partialTick);
		// Une projection est lumineuse dans le noir.
		Scène scène = état.getGeckolibData(SCÈNE);
		if (scène != null && scène.hologramme()) {
			état.lightCoords = LightCoordsUtil.FULL_BRIGHT;
		}
		// Sa bulle est dessinée dans l'interface, au-dessus d'elle (BubbleOverlay).
		dev.pyrofetch.jn1.client.hud.BubbleOverlay.ancrer(entity, partialTick);
	}

	/**
	 * Le rendu élimine les faces arrière, là où GeckoLib les garde par défaut.
	 *
	 * <p>La poitrine, la croupe et la robe de Gardevoir sont des cubes dont une
	 * dimension vaut zéro. Leurs faces avant et arrière sont donc **dans le même
	 * plan**, et tant que les deux sont dessinées elles se disputent chaque
	 * pixel : la face arrière, qui échantillonne une autre zone de la planche,
	 * l'emporte par endroits et y pose une tache sombre. Le dessin change avec
	 * l'angle de vue, ce qui donne une ombre qui se déplace avec le joueur.
	 *
	 * <p>Aucune géométrie n'est perdue : un cube plat n'est pas une facette
	 * simple mais une boîte dégénérée dont les deux faces regardent en sens
	 * opposés. De n'importe quel point de vue, l'une des deux est tournée vers
	 * l'œil et reste tracée.
	 *
	 * <p>Cela suppose que toutes les boîtes soient enroulées dans le même sens :
	 * une taille négative les retourne, et c'est alors la mauvaise face qui
	 * tombe. `validate_assets.py` refuse désormais les tailles négatives.
	 *
	 * <p>En 26.3, l'ancien {@code entityCutout} (faces arrière éliminées) se
	 * nomme {@code entityCutoutCull} ; {@code entityCutout} est l'ancien
	 * {@code entityCutoutNoCull}.
	 */
	@Override
	public @Nullable RenderType getRenderType(LivingEntityRenderState état, Identifier texture) {
		// Aura est une projection : translucide, faces arrière toujours éliminées.
		Scène scène = état.getGeckolibData(SCÈNE);
		if (scène != null && scène.hologramme()) {
			return RenderTypes.entityTranslucentCull(texture);
		}
		return RenderTypes.entityCutoutCull(texture);
	}

	@Override
	public int getRenderColor(T animatable, @Nullable Void relatedObject, float partialTick) {
		if (animatable instanceof AuraEntity aura && aura.isHologram()) {
			return ARGB.colorFromFloat(Hologram.alpha(animatable, partialTick), 1.0F, 1.0F, 1.0F);
		}
		return super.getRenderColor(animatable, relatedObject, partialTick);
	}

	/** Une projection ne porte pas d'ombre. */
	@Override
	protected float getShadowRadius(LivingEntityRenderState état) {
		Scène scène = état.getGeckolibData(SCÈNE);
		return scène != null && scène.hologramme() ? 0.0F : super.getShadowRadius(état);
	}

	/** Une projection est décalée d'un cheveu pendant un parasite — elle et tout ce qui l'accompagne. */
	@Override
	public void submit(LivingEntityRenderState état, PoseStack poseStack, SubmitNodeCollector renderTasks,
	                   CameraRenderState caméra) {
		Scène scène = état.getGeckolibData(SCÈNE);
		if (scène != null && scène.décalage() != 0.0F) {
			poseStack.pushPose();
			poseStack.translate(scène.décalage(), 0.0F, 0.0F);
			super.submit(état, poseStack, renderTasks, caméra);
			poseStack.popPose();
		} else {
			super.submit(état, poseStack, renderTasks, caméra);
		}
	}

	/** Les retouches d'os de la compagne, une fois l'animation posée ({@link CompanionModel#poser}). */
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots os) {
		Scène scène = info.getGeckolibData(SCÈNE);
		if (scène == null) {
			return;
		}
		LivingEntityRenderState état = info.renderState();
		// Les signes des os de GeckoLib sont l'opposé de ceux du jeu.
		this.modèle.poser(info.model(), os, scène, -état.yRot, -état.xRot);
	}

	@Override
	public void submitRenderTasks(RenderPassInfo<LivingEntityRenderState> info, OrderedSubmitNodeCollector renderTasks,
	                              @Nullable RenderType renderType) {
		Scène scène = info.getGeckolibData(SCÈNE);
		if (renderType == null || scène == null || info.model().isMissingno()) {
			super.submitRenderTasks(info, renderTasks, renderType);
			return;
		}
		this.soumettre(info, renderTasks, renderType, Couche.CORPS, false, scène);

		// Le mannequin d'un partenaire porte son skin : ses UV sont celles d'un
		// skin de joueur, 64 × 64. Translucide comme le joueur du jeu, pour la
		// seconde couche (chapeau, veste).
		if (scène.partenaire() && scène.peau() != null) {
			this.soumettre(info, renderTasks, RenderTypes.entityTranslucent(scène.peau()), Couche.PEAU, false, scène);
		}

		// La pièce de scène a sa propre planche : ses UV visent un coin que celle
		// de la compagne laisse vide.
		if (scène.pièce()) {
			this.soumettre(info, renderTasks,
					RenderTypes.entityCutout(dev.pyrofetch.jn1.JN1.id(scène.kind().scenePropTexturePath())),
					Couche.PIÈCE, false, scène);
		}
	}

	/** Le calque de traces ({@link TracesLayer}) : le corps redessiné, sans le mannequin ni la pièce. */
	void soumettreTraces(RenderPassInfo<LivingEntityRenderState> info, OrderedSubmitNodeCollector renderTasks,
	                     RenderType couche) {
		Scène scène = info.getGeckolibData(SCÈNE);
		if (scène != null && !info.model().isMissingno()) {
			this.soumettre(info, renderTasks, couche, Couche.CORPS, true, scène);
		}
	}

	/**
	 * Un dessin de la compagne sur une planche, comme GeckoLib soumet le sien
	 * ({@code GeoRenderer.submitRenderTasks}) : exécuté plus tard par le jeu,
	 * qui rend la pose de ce moment-ci ; les os sont alors posés
	 * ({@code renderPosed}) le temps du parcours.
	 */
	private void soumettre(RenderPassInfo<LivingEntityRenderState> info, OrderedSubmitNodeCollector renderTasks,
	                       RenderType type, Couche couche, boolean traces, Scène scène) {
		int lumière = info.packedLight();
		int recouvrement = info.packedOverlay();
		int couleur = info.renderColor();
		renderTasks.submitCustomGeometry(info.poseStack(), type, (pose, tampon) -> {
			PoseStack pile = info.poseStack();
			pile.pushPose();
			pile.last().set(pose);
			info.renderPosed(() -> {
				for (GeoBone os : info.model().topLevelBones()) {
					this.dessinerOs(info, pile, os, Couche.CORPS, couche, traces, scène, tampon,
							lumière, recouvrement, couleur);
				}
			});
			pile.popPose();
		});
	}

	/**
	 * Un os et sa descendance, comme {@code renderRecursively} de GeckoLib 4 :
	 * matrice de l'os, ses cubes s'ils sont de la planche voulue et visibles,
	 * puis ses enfants s'il ne les cache pas.
	 *
	 * @param héritée la planche de l'os parent : un sous-arbre la garde
	 * @param voulue  la planche de ce dessin-ci
	 */
	private void dessinerOs(RenderPassInfo<LivingEntityRenderState> info, PoseStack pile, GeoBone os,
	                        Couche héritée, Couche voulue, boolean traces, Scène scène, VertexConsumer tampon,
	                        int lumière, int recouvrement, int couleur) {
		String nom = os.name();
		if (JAMAIS.contains(nom)) {
			return;
		}
		// Le calque de traces suit les UV de la fille : le mannequin et la pièce
		// de scène, qui lisent d'autres planches, n'en reçoivent pas.
		if (traces && (nom.equals("steve") || nom.equals(scène.kind().sceneProp()))) {
			return;
		}
		Couche couche = héritée;
		if (scène.partenaire() && nom.equals("steve")) {
			couche = Couche.PEAU;
		}
		if (scène.pièce() && nom.equals(scène.kind().sceneProp())) {
			couche = Couche.PIÈCE;
		}
		boolean principal = voulue == Couche.CORPS && !traces;
		boolean racine = this.shading.enterBone(pile, os);
		pile.pushPose();
		RenderUtil.prepMatrixForBoneAndUpdateListeners(pile, os, principal ? info : null);

		BoneSnapshot instantané = os.frameSnapshot;
		if (couche == voulue && (instantané == null || !instantané.isHidden())) {
			for (GeoCube cube : CompanionShading.cubes(os)) {
				this.shading.dessinerCube(pile, cube, tampon, lumière, recouvrement, couleur);
			}
		}

		// Le point des yeux du joueur qui tient la place de steve : le centre
		// de la tête du mannequin, là où le jeu met l'œil d'un joueur — relevé
		// même quand la tête est cachée à la première personne.
		if (principal && scène.participe() && nom.equals("Head2")) {
			relèverŒil(info.cameraState(), pile, os, scène.id());
		}

		if (instantané == null || !instantané.areChildrenHidden()) {
			for (GeoBone fils : os.children()) {
				this.dessinerOs(info, pile, fils, couche, voulue, traces, scène, tampon, lumière, recouvrement, couleur);
			}
		}
		pile.popPose();
		this.shading.leaveBone(racine);
	}

	/**
	 * Le centre des cubes de l'os, dans le monde. La matrice de la pile mène du
	 * modèle à la caméra (les rendus d'entités sont relatifs à elle) : on y
	 * ajoute sa position.
	 */
	private static void relèverŒil(CameraRenderState caméra, PoseStack pile, GeoBone os, int id) {
		Vector3f centre = new Vector3f();
		int n = 0;
		for (GeoCube cube : CompanionShading.cubes(os)) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) {
					continue;
				}
				for (GeoVertex sommet : quad.vertices()) {
					centre.add(sommet.posX(), sommet.posY(), sommet.posZ());
					n++;
				}
			}
		}
		if (n > 0) {
			Vector3f vue = pile.last().pose().transformPosition(centre.div(n));
			Vec3 œil = caméra.pos;
			SceneView.remember(id, new Vec3(œil.x + vue.x(), œil.y + vue.y(), œil.z + vue.z()));
		}
	}
}
