package dev.pyrofetch.jn1.client.morph;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pyrofetch.jn1.client.render.CompanionModel;
import dev.pyrofetch.jn1.client.render.CompanionShading;
import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.morph.MorphState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Dessine un joueur transformé sous les traits de sa compagne.
 *
 * <p>Branché par {@code EntityRenderDispatcherMixin} au seul moment du rendu
 * dans le monde (et dans l'inventaire) : ailleurs — le bras à la première
 * personne notamment — le jeu attend le rendu du joueur et le convertit.
 *
 * <p>Même éclairage que les compagnes ({@link CompanionShading}) et mêmes faces
 * arrière éliminées, pour qu'elle paraisse la même, qu'on la croise ou qu'on
 * la soit.
 *
 * <p><strong>26.3.</strong> Le rendu passe par un état ({@link État}) relevé à
 * l'image, puis soumis. Le jeu choisit le rendu d'un état d'après son type :
 * un {@code État} est à confier à ce rendu-ci (le mixin du répartiteur,
 * {@code getRenderer(EntityRenderState)}). C'est aussi un état d'avatar,
 * rempli d'abord par le rendu des joueurs du jeu : 26.3 exige un état d'avatar
 * pour le joueur local (son bras à la première personne, les effets d'écran,
 * {@code LevelExtractor.extractPlayerState}), et le bras, lui, garde le rendu
 * des joueurs ({@code getRenderer(AvatarRenderState)}, que le mixin ne
 * détourne pas) — la planche du bras de la compagne y est posée par
 * {@code PlayerArmMixin}.
 */
public class MorphRenderer extends GeoReplacedEntityRenderer<MorphAnimatable, AbstractClientPlayer, MorphRenderer.État> {
	/** Ce qu'un joueur accroupi descend, comme au rendu du jeu. */
	private static final double SNEAK_DROP = -0.125;

	/**
	 * L'état de rendu d'un joueur transformé : celui d'un joueur (skin, bras,
	 * objets en main, rempli par le rendu des joueurs), plus ce que ses
	 * retouches d'os lisaient sur lui.
	 */
	public static class État extends AvatarRenderState {
		int poitrine;
		int croupe;
		boolean accroupi;
		/** Transformé en Aura, on est une projection. */
		boolean projection;
		@Nullable
		MorphArmPoses.Bras bras;
		/** Ce qu'il tient, main droite et main gauche ({@link MorphHeldItemLayer}). */
		final ItemStackRenderState objetDroit = new ItemStackRenderState();
		final ItemStackRenderState objetGauche = new ItemStackRenderState();
	}

	private final MorphModel modèle;
	private final CompanionShading shading = new CompanionShading();

	public MorphRenderer(EntityRendererProvider.Context context) {
		this(context, new MorphModel());
	}

	private MorphRenderer(EntityRendererProvider.Context context, MorphModel modèle) {
		super(context, modèle, MorphAnimatable.INSTANCE);
		this.modèle = modèle;
		this.shadowRadius = 0.4F;
		// Ce qu'il tient en main, dans les siennes. Pas d'armure.
		this.withRenderLayer(new MorphHeldItemLayer(this, context.getItemModelResolver()));
	}

	@Override
	public État createRenderState(MorphAnimatable animatable, AbstractClientPlayer joueur) {
		return new État();
	}

	/**
	 * Avant tout relevé, l'animatable unique prend la compagne de ce joueur :
	 * sa couleur, ses animations et ses contrôleurs en dépendent.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void extractRenderState(AbstractClientPlayer joueur, État état, float partialTick) {
		this.animatable.montrer(ClientMorphs.of(joueur));
		// D'abord ce qu'en relève le rendu des joueurs du jeu (celui de son type de skin).
		état.skin = joueur.getSkin();
		AvatarRenderer<?> avatar = this.entityRenderDispatcher.getRenderer((AvatarRenderState) état);
		((AvatarRenderer<AbstractClientPlayer>) avatar).extractRenderState(joueur, état, partialTick);
		super.extractRenderState(joueur, état, partialTick);
		// Transformé en Aura, on est une projection : lumineuse dans le noir.
		if (état.projection) {
			état.lightCoords = LightCoordsUtil.FULL_BRIGHT;
		}
	}

	@Override
	public void addRenderData(MorphAnimatable animatable, @Nullable AbstractClientPlayer joueur, État état,
	                          float partialTick) {
		if (joueur == null) {
			return;
		}
		MorphState forme = ClientMorphs.of(joueur);
		état.addGeckolibData(CompanionModel.APPARENCE, new CompanionModel.Apparence(animatable.kind(), animatable.outfit()));
		état.addGeckolibData(MorphAnimatable.JOUEUR, joueur);
		état.poitrine = forme.boobs();
		état.croupe = forme.booty();
		état.accroupi = joueur.isCrouching();
		état.projection = animatable.kind() == CompanionKind.AURA;
		état.bras = MorphArmPoses.Bras.relever(joueur);
	}

	@Override
	public Vec3 getRenderOffset(État état) {
		Vec3 base = super.getRenderOffset(état);
		return état.accroupi ? base.add(0, SNEAK_DROP, 0) : base;
	}

	@Override
	public @Nullable RenderType getRenderType(État état, Identifier texture) {
		RenderType défaut = super.getRenderType(état, texture);
		// Invisible ou translucide : on laisse faire GeckoLib ; sinon, faces
		// arrière éliminées, comme chez les compagnes — translucides pour Aura.
		if (défaut == null || état.isInvisible) {
			return défaut;
		}
		return état.projection ? RenderTypes.entityTranslucentCull(texture) : RenderTypes.entityCutoutCull(texture);
	}

	@Override
	public int getRenderColor(MorphAnimatable animatable, AbstractClientPlayer joueur, float partialTick) {
		if (animatable.kind() == CompanionKind.AURA && joueur != null) {
			return ARGB.colorFromFloat(dev.pyrofetch.jn1.client.render.Hologram.alpha(joueur, partialTick),
					1.0F, 1.0F, 1.0F);
		}
		return super.getRenderColor(animatable, joueur, partialTick);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<État> info, BoneSnapshots os) {
		this.modèle.poser(info.model(), os, info.renderState());
	}

	/** Le modèle, os par os, avec l'éclairage des compagnes ({@link CompanionShading#dessinerOs}). */
	@Override
	public void submitRenderTasks(RenderPassInfo<État> info, OrderedSubmitNodeCollector renderTasks,
	                              @Nullable RenderType renderType) {
		if (renderType == null || info.model().isMissingno()) {
			super.submitRenderTasks(info, renderTasks, renderType);
			return;
		}
		int lumière = info.packedLight();
		int recouvrement = info.packedOverlay();
		int couleur = info.renderColor();
		renderTasks.submitCustomGeometry(info.poseStack(), renderType, (pose, tampon) -> {
			PoseStack pile = info.poseStack();
			pile.pushPose();
			pile.last().set(pose);
			info.renderPosed(() -> {
				for (GeoBone os : info.model().topLevelBones()) {
					this.shading.dessinerOs(info, pile, os, tampon, lumière, recouvrement, couleur);
				}
			});
			pile.popPose();
		});
	}
}
