package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Les traces blanches d'une fille, redessinées par-dessus son corps.
 *
 * <p>Un calque par fille et par niveau ({@code textures/entity/traces/<fille>_<n>.png},
 * {@code tools/gen_cum.py}), posé sur les UV de son modèle : on redessine le
 * modèle entier avec ce calque, translucide, à la même profondeur. Le
 * mannequin {@code steve} et la pièce de scène sont sautés
 * ({@link CompanionRenderer#renderRecursively}) : leurs UV visent une autre
 * planche, et le calque y poserait des taches au hasard.
 */
public class TracesLayer<T extends CompanionEntity> extends GeoRenderLayer<T> {
	private final CompanionRenderer<T> rendu;

	public TracesLayer(CompanionRenderer<T> renderer) {
		super(renderer);
		this.rendu = renderer;
	}

	public static Identifier texture(CompanionEntity fille, int niveau) {
		return JN1.id("textures/entity/traces/" + fille.kind().id() + "_" + niveau + ".png");
	}

	@Override
	public void render(MatrixStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderLayer renderType,
	                   VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, float partialTick,
	                   int packedLight, int packedOverlay) {
		int niveau = animatable.getTraces();
		if (niveau <= 0 || animatable.isInvisible()) {
			return;
		}
		RenderLayer couche = RenderLayer.getEntityTranslucent(texture(animatable, niveau));
		int couleur = this.rendu.getRenderColor(animatable, partialTick, packedLight).argbInt();
		this.rendu.dessinerTraces(true);
		try {
			this.rendu.reRender(bakedModel, poseStack, bufferSource, animatable, couche,
					bufferSource.getBuffer(couche), partialTick, packedLight, packedOverlay, couleur);
		} finally {
			this.rendu.dessinerTraces(false);
		}
	}
}
