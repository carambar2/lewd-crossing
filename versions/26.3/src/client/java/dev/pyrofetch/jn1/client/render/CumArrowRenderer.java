package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.cum.CumArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** La flèche collante : le modèle de la flèche du jeu, sa planche nappée de blanc. */
public class CumArrowRenderer extends ArrowRenderer<CumArrowEntity, ArrowRenderState> {
	private static final Identifier PLANCHE = JN1.id("textures/entity/projectiles/cum_arrow.png");

	public CumArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState flèche) {
		return PLANCHE;
	}
}
