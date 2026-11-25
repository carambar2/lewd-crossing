package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * La scène vue de l'intérieur : le joueur local tient la place de {@code steve}.
 *
 * <p>Il est monté sur la compagne ({@code CompanionEntity.joinScene}) ; son
 * personnage n'est plus dessiné, c'est le mannequin {@code steve}, habillé de
 * son skin, qui le représente — pour lui comme pour les autres. La caméra se
 * place au centre de la tête du mannequin, là où le jeu met l'œil d'un joueur,
 * et le regard reste libre.
 *
 * <p>Le point des yeux est relevé au rendu de la compagne
 * ({@link CompanionRenderer}), à partir de la matrice que GeckoLib tient pour
 * l'os {@code Head2}. La caméra de l'image suivante s'en sert : une image de
 * retard, sans importance pour un corps qui bouge au rythme d'une animation.
 */
public final class SceneView {
	private static int compagne = -1;
	@Nullable
	private static Vec3d œil;

	private SceneView() {
	}

	/** La compagne dont le joueur local tient la place de {@code steve}, ou {@code null}. */
	@Nullable
	public static CompanionEntity participating() {
		PlayerEntity joueur = MinecraftClient.getInstance().player;
		if (joueur != null && joueur.getVehicle() instanceof CompanionEntity jenny
				&& jenny.showsScenePartner() && jenny.isScenePartner(joueur)) {
			return jenny;
		}
		return null;
	}

	/** Vrai s'il y participe et regarde à la première personne. */
	public static boolean firstPerson() {
		return participating() != null && MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
	}

	/** Le point des yeux de {@code steve}, s'il y participe et qu'il a déjà été relevé. */
	@Nullable
	public static Vec3d eye() {
		CompanionEntity jenny = participating();
		return jenny != null && jenny.getId() == compagne ? œil : null;
	}

	static void remember(CompanionEntity jenny, Vec3d point) {
		compagne = jenny.getId();
		œil = point;
	}

	/** Un joueur que le mannequin représente : son propre personnage ne se dessine pas. */
	public static boolean hidesPlayer(Entity entity) {
		return entity instanceof PlayerEntity joueur && joueur.getVehicle() instanceof CompanionEntity jenny
				&& jenny.showsScenePartner() && jenny.isScenePartner(joueur);
	}

	/** Le skin du partenaire de cette compagne ; celui par défaut s'il n'est pas chargé. */
	public static SkinTextures skin(CompanionEntity jenny) {
		MinecraftClient client = MinecraftClient.getInstance();
		java.util.UUID uuid = jenny.getScenePartner().orElse(null);
		if (uuid != null && client.world != null
				&& client.world.getPlayerByUuid(uuid) instanceof AbstractClientPlayerEntity joueur) {
			return joueur.getSkinTextures();
		}
		return DefaultSkinHelper.getSkinTextures(uuid == null ? java.util.UUID.randomUUID() : uuid);
	}
}
