package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
 * ({@link CompanionRenderer}), à partir de la matrice de l'os {@code Head2}
 * au moment où ses cubes se dessinent. La caméra de l'image suivante s'en
 * sert : une image de retard, sans importance pour un corps qui bouge au
 * rythme d'une animation.
 */
public final class SceneView {
	private static int compagne = -1;
	@Nullable
	private static Vec3 œil;

	private SceneView() {
	}

	/** La compagne dont le joueur local tient la place de {@code steve}, ou {@code null}. */
	@Nullable
	public static CompanionEntity participating() {
		Player joueur = Minecraft.getInstance().player;
		if (joueur != null && joueur.getVehicle() instanceof CompanionEntity jenny
				&& jenny.showsScenePartner() && jenny.isScenePartner(joueur)) {
			return jenny;
		}
		return null;
	}

	/** Vrai s'il y participe et regarde à la première personne. */
	public static boolean firstPerson() {
		return participating() != null && Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	/** Le point des yeux de {@code steve}, s'il y participe et qu'il a déjà été relevé. */
	@Nullable
	public static Vec3 eye() {
		CompanionEntity jenny = participating();
		return jenny != null && jenny.getId() == compagne ? œil : null;
	}

	/** La compagne est désignée par son numéro d'entité : le rendu de 26.3 ne voit plus que son état. */
	static void remember(int jenny, Vec3 point) {
		compagne = jenny;
		œil = point;
	}

	/** Un joueur que le mannequin représente : son propre personnage ne se dessine pas. */
	public static boolean hidesPlayer(Entity entity) {
		return entity instanceof Player joueur && joueur.getVehicle() instanceof CompanionEntity jenny
				&& jenny.showsScenePartner() && jenny.isScenePartner(joueur);
	}

	/** Le skin du partenaire de cette compagne ; celui par défaut s'il n'est pas chargé. */
	public static PlayerSkin skin(CompanionEntity jenny) {
		Minecraft client = Minecraft.getInstance();
		java.util.UUID uuid = jenny.getScenePartner().orElse(null);
		if (uuid != null && client.level != null
				&& client.level.getPlayerByUUID(uuid) instanceof AbstractClientPlayer joueur) {
			return joueur.getSkin();
		}
		return DefaultPlayerSkin.get(uuid == null ? java.util.UUID.randomUUID() : uuid);
	}
}
