package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.morph.MorphState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Le bras à la première personne d'un joueur transformé.
 *
 * <p>Les planches viennent du mod d'origine : un bras de joueur déplié, pavé de
 * 4×12×4 au coin d'une grille de 32 — la manche de la tenue avec la main au
 * bout, ou le bras nu. Une par tenue, au nom de son modèle
 * ({@code textures/entity/hand/<modèle>.png}) :
 * une compagne sans planche garde le bras du joueur.
 *
 * <p>Le pavé reprend la position et l'angle que le jeu vient de donner au bras
 * du joueur ({@code copyTransform}) : seuls la forme et la planche changent.
 */
public final class MorphHands {
	private static final Map<String, Boolean> PRÉSENTES = new HashMap<>();
	@Nullable
	private static ModelPart droit;
	@Nullable
	private static ModelPart gauche;

	private MorphHands() {
	}

	/** La planche du bras de ce joueur, ou {@code null} s'il garde le sien. */
	@Nullable
	public static Identifier texture(AbstractClientPlayer joueur) {
		MorphState état = ClientMorphs.of(joueur);
		CompanionKind kind = état.companion();
		if (kind == null) {
			return null;
		}
		String modèle = état.currentOutfit();
		Identifier planche = JN1.id("textures/entity/hand/" + modèle + ".png");
		boolean là = PRÉSENTES.computeIfAbsent(modèle, clé ->
				Minecraft.getInstance().getResourceManager().getResource(planche).isPresent());
		return là ? planche : null;
	}

	/** Le bras droit ou gauche, taillé comme celui du joueur, sur une planche de 32. */
	public static ModelPart bras(boolean droite) {
		if (droit == null) {
			MeshDefinition données = new MeshDefinition();
			données.getRoot().addOrReplaceChild("droit", CubeListBuilder.create().texOffs(0, 0)
					.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE), PartPose.ZERO);
			données.getRoot().addOrReplaceChild("gauche", CubeListBuilder.create().texOffs(0, 0).mirror()
					.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE), PartPose.ZERO);
			ModelPart racine = LayerDefinition.create(données, 32, 32).bakeRoot();
			droit = racine.getChild("droit");
			gauche = racine.getChild("gauche");
		}
		return droite ? droit : gauche;
	}

	/** À la déconnexion : un pack de ressources a pu changer entre-temps. */
	public static void clear() {
		PRÉSENTES.clear();
	}
}
