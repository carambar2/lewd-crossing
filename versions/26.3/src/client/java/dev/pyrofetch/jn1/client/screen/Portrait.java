package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Une compagne dessinée dans un écran — son portrait —, à la place de
 * {@code InventoryScreen.drawEntity}.
 *
 * <p><strong>Attention (0.53.3) :</strong> {@code drawEntity} dessine
 * l'entité comme à la fin du tick (instant 1,0), alors que le monde, dans la
 * même image, la dessine en cours de tick. Pour GeckoLib, qui tient une
 * horloge d'animation par entité, le temps avançait puis reculait à chaque
 * image ; la remise au repos de la tête, calculée sur cet écart, prenait une
 * fraction <em>négative</em> et amplifiait l'angle au lieu de le ramener. Le
 * portrait tournait la tête jusqu'à la butée, d'un côté ou de l'autre selon
 * l'orientation de la fille dans le monde (Ellie : 0° et 90°
 * à droite, 180° et 270° à gauche ; Jenny avait le même défaut, en 0.53.1,
 * à moindre angle).
 *
 * <p>D'où une <strong>copie</strong> : une entité du même type, hors du monde,
 * qui porte la même tenue, les mêmes traces et les mêmes formes, et a sa propre
 * horloge. Elle vit au même âge que l'originale et est dessinée au même instant
 * du tick que le monde : ses animations coulent comme les siennes. La vraie
 * fille, elle, n'est plus touchée.
 */
public final class Portrait {
	private final CompanionEntity source;
	@Nullable
	private CompanionEntity copie;

	public Portrait(CompanionEntity source) {
		this.source = source;
	}

	/** Mêmes arguments que {@code InventoryScreen.drawEntity}, l'entité en moins. */
	public void dessiner(GuiGraphicsExtractor contexte, int x1, int y1, int x2, int y2, int taille, float décalage,
			float sourisX, float sourisY) {
		CompanionEntity copie = this.copie();
		dessiner(contexte, x1, y1, x2, y2, taille, décalage, sourisX, sourisY, copie != null ? copie : this.source);
	}

	@Nullable
	private CompanionEntity copie() {
		if (this.copie == null && this.source.level() != null
				&& this.source.getType().create(this.source.level(), EntitySpawnReason.LOAD) instanceof CompanionEntity neuve) {
			this.copie = HorsDuMonde.identifier(neuve);
		}
		if (this.copie != null) {
			this.source.copierApparence(this.copie);
			this.copie.tickCount = this.source.tickCount;
		}
		return this.copie;
	}

	/**
	 * Une entité de face, qui suit la souris, comme {@code drawEntity} — mais
	 * dessinée à l'instant du tick du monde, ses angles « précédents » posés
	 * comme les autres. Pour une entité du monde (le joueur, dans l'écran de
	 * transformation), les angles sont rendus ensuite.
	 */
	public static void dessiner(GuiGraphicsExtractor contexte, int x1, int y1, int x2, int y2, int taille, float décalage,
			float sourisX, float sourisY, LivingEntity entité) {
		float cx = (x1 + x2) / 2.0F;
		float cy = (y1 + y2) / 2.0F;
		float i = (float) Math.atan((cx - sourisX) / 40.0F);
		float j = (float) Math.atan((cy - sourisY) / 40.0F);
		Quaternionf vue = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf penché = new Quaternionf().rotateX(j * 20.0F * (float) (Math.PI / 180.0));
		vue.mul(penché);

		float corps = entité.yBodyRot;
		float corpsAvant = entité.yBodyRotO;
		float lacet = entité.getYRot();
		float lacetAvant = entité.yRotO;
		float tangage = entité.getXRot();
		float tangageAvant = entité.xRotO;
		float tête = entité.yHeadRot;
		float têteAvant = entité.yHeadRotO;

		entité.yBodyRot = entité.yBodyRotO = 180.0F + i * 20.0F;
		entité.setYRot(180.0F + i * 40.0F);
		entité.yRotO = entité.getYRot();
		entité.setXRot(-j * 20.0F);
		entité.xRotO = entité.getXRot();
		entité.yHeadRot = entité.yHeadRotO = entité.getYRot();

		try {
			rendre(contexte, x1, y1, x2, y2, taille, décalage, vue, penché, entité);
		} finally {
			entité.yBodyRot = corps;
			entité.yBodyRotO = corpsAvant;
			entité.setYRot(lacet);
			entité.yRotO = lacetAvant;
			entité.setXRot(tangage);
			entité.xRotO = tangageAvant;
			entité.yHeadRot = tête;
			entité.yHeadRotO = têteAvant;
		}
	}

	/**
	 * Le dessin de {@code extractEntityInInventoryFollowsMouse}, à l'instant du
	 * tick du monde au lieu de 1,0.
	 *
	 * <p>Depuis 1.21.6, une entité dans un écran est rendue à part, dans sa
	 * propre texture, découpée au cadre {@code x1..x2, y1..y2} : plus besoin de
	 * ciseaux. Le jeu n'en a qu'une par sorte de vue, et le terminal montre
	 * plusieurs filles à la fois : la vue passe donc par {@link Vignettes}, qui en
	 * a une par portrait de l'image. Son état de rendu est relevé ici, les angles
	 * posés plus haut, et l'échelle de la taille (potion) retirée comme le fait le
	 * jeu : la vue la montre entière.
	 */
	private static void rendre(GuiGraphicsExtractor contexte, int x1, int y1, int x2, int y2, int taille,
			float décalage, Quaternionf vue, Quaternionf penché, LivingEntity entité) {
		Minecraft client = Minecraft.getInstance();
		float instant = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		EntityRenderState état = dispatcher.getRenderer(entité).createRenderState(entité, instant);
		état.shadowPieces.clear();
		état.outlineColor = 0;
		état.lightCoords = 15728880;
		if (état instanceof LivingEntityRenderState vivant) {
			vivant.boundingBoxWidth = vivant.boundingBoxWidth / vivant.scale;
			vivant.boundingBoxHeight = vivant.boundingBoxHeight / vivant.scale;
			vivant.scale = 1.0F;
		}
		Vector3f centre = new Vector3f(0.0F, état.boundingBoxHeight / 2.0F + décalage, 0.0F);
		boolean posé = Vignettes.ajouter(contexte, x1, y1, x2, y2, taille, (pile, envoi) -> {
			// Ce que fait GuiEntityRenderer.renderToTexture.
			client.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
			pile.translate(centre.x(), centre.y(), centre.z());
			pile.rotate(vue);
			CameraRenderState caméra = new CameraRenderState();
			caméra.orientation = penché.conjugate(new Quaternionf()).rotateY((float) Math.PI);
			dispatcher.submit(état, caméra, 0.0, 0.0, 0.0, pile, envoi);
		});
		if (!posé) {
			// Plus de place dans cette image : la vue du jeu, partagée.
			contexte.entity(état, taille, centre, vue, penché, x1, y1, x2, y2);
		}
	}
}
