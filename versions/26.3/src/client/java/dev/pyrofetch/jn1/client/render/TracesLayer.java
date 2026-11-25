package dev.pyrofetch.jn1.client.render;

import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Les traces blanches d'une fille, redessinées par-dessus son corps.
 *
 * <p>Un calque par fille et par niveau ({@code textures/entity/traces/<fille>_<n>.png},
 * {@code tools/gen_cum.py}), posé sur les UV de son modèle : on redessine le
 * modèle entier avec ce calque, translucide, à la même profondeur. Le
 * mannequin {@code steve} et la pièce de scène sont sautés
 * ({@link CompanionRenderer#soumettreTraces}) : leurs UV visent une autre
 * planche, et le calque y poserait des taches au hasard.
 */
public class TracesLayer<T extends CompanionEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
	private final CompanionRenderer<T> rendu;

	public TracesLayer(CompanionRenderer<T> renderer) {
		super(renderer);
		this.rendu = renderer;
	}

	public static Identifier texture(CompanionEntity fille, int niveau) {
		return texture(fille.kind(), niveau);
	}

	public static Identifier texture(CompanionKind fille, int niveau) {
		return JN1.id("textures/entity/traces/" + fille.id() + "_" + niveau + ".png");
	}

	@Override
	public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector renderTasks) {
		CompanionRenderer.Scène scène = info.getGeckolibData(CompanionRenderer.SCÈNE);
		if (!info.willRender() || scène == null || scène.traces() <= 0 || scène.invisible()) {
			return;
		}
		// Après le corps, comme le calque de texture de GeckoLib.
		this.rendu.soumettreTraces(info, renderTasks.order(1),
				RenderTypes.entityTranslucent(texture(scène.kind(), scène.traces())));
	}
}
