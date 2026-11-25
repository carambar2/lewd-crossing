package dev.pyrofetch.jn1.excitation;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionScenes;
import dev.pyrofetch.jn1.entity.JennyMode;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Le réveil : au matin, une fille qui l'aime assez le réveille en le branlant
 * (la scène {@link #SCÈNE}).
 *
 * <p>Quand un joueur se réveille de jour, une fille à 24 blocs — libre, sans
 * scène, la sienne ou une dont il a l'affection du câlin, la sienne d'abord —
 * monte sur le lit, à genoux sur ses jambes ; il reste couché (la scène le
 * couche). Il prend la place de
 * {@code steve} ; la barre d'excitation mène la scène à sa fin comme une
 * autre. Attendu quelques ticks après le réveil : le jeu ne l'a pas encore
 * relevé du lit au moment de l'événement.
 */
public final class Reveil {
	/** Au lit : il reste couché, elle le branle à genoux sur ses jambes ({@code tools/gen_lit.py}). */
	public static final String SCÈNE = "d_hj";
	private static final int ATTENTE = 10;
	/** Le joueur lui-même, pas son UUID : un joueur factice (épreuve) n'est pas dans la liste du serveur. */
	private static final Map<ServerPlayer, Integer> EN_ATTENTE = new HashMap<>();
	/** Le lit où il dormait : la tête du lit, donnée par l'événement. */
	private static final Map<ServerPlayer, BlockPos> LITS = new HashMap<>();

	private Reveil() {
	}

	public static void register() {
		// Le jour n'est pas vérifié ici : le jeu réveille les dormeurs juste après
		// avoir avancé l'heure, mais avant de recalculer la clarté — isDay()
		// répond encore « nuit » à cet instant, et le réveil ne venait jamais.
		EntitySleepEvents.STOP_SLEEPING.register((dormeur, lit) -> {
			if (dormeur instanceof ServerPlayer joueur) {
				dev.pyrofetch.jn1.JN1.LOGGER.info("Réveil : {} se réveille, une fille viendra dans {} ticks s'il fait jour",
						joueur.getName().getString(), ATTENTE);
				EN_ATTENTE.put(joueur, ATTENTE);
				LITS.put(joueur, lit);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(serveur -> {
			if (EN_ATTENTE.isEmpty()) {
				return;
			}
			for (var entrée : Map.copyOf(EN_ATTENTE).entrySet()) {
				int reste = entrée.getValue() - 1;
				if (reste > 0) {
					EN_ATTENTE.put(entrée.getKey(), reste);
					continue;
				}
				EN_ATTENTE.remove(entrée.getKey());
				ServerPlayer joueur = entrée.getKey();
				BlockPos lit = LITS.remove(joueur);
				if (joueur.isRemoved()) {
					continue;
				}
				if (!joueur.level().isBrightOutside()) {
					dev.pyrofetch.jn1.JN1.LOGGER.info("Réveil : il ne fait pas jour (heure {}), personne ne vient",
							joueur.level().getOverworldClockTime() % 24000);
					continue;
				}
				CompanionEntity venue = réveiller(joueur, false, lit);
				dev.pyrofetch.jn1.JN1.LOGGER.info("Réveil : {}", venue == null ? "personne n'est venu"
						: String.format(java.util.Locale.ROOT, "%s joue %s à %.3f %.3f %.3f, lacet %.0f (lit : %s)",
						venue.getName().getString(), venue.getScene(), venue.getX(), venue.getY(), venue.getZ(),
						venue.getYRot(), lit == null ? "aucun" : lit.toShortString()));
			}
		});
	}

	/**
	 * Fait venir une de ses compagnes ; rend celle qui vient, ou {@code null}.
	 * {@code toutes} : n'importe quelle fille, sans condition d'affection
	 * (commande d'épreuve).
	 */
	@org.jetbrains.annotations.Nullable
	public static CompanionEntity réveiller(ServerPlayer joueur, boolean toutes) {
		return réveiller(joueur, toutes, litProche(joueur));
	}

	/** La tête du lit le plus proche, à 6 blocs, ou {@code null}. */
	@org.jetbrains.annotations.Nullable
	public static BlockPos litProche(ServerPlayer joueur) {
		BlockPos centre = joueur.blockPosition();
		BlockPos meilleur = null;
		for (BlockPos pos : BlockPos.withinBoxByManhattanDistance(centre, 6, 3, 6)) {
			BlockState état = joueur.level().getBlockState(pos);
			if (état.getBlock() instanceof BedBlock && état.getValue(BedBlock.PART) == BedPart.HEAD) {
				meilleur = pos.immutable();
				break;
			}
		}
		return meilleur;
	}

	/**
	 * La fille sur le lit : au pied, sur le matelas, tournée vers l'oreiller.
	 * La scène couche steve — le joueur — la tête sur l'oreiller : elle est
	 * réglée pour que sa tête tombe au bout du lit quand la fille est à quatre
	 * pixels du milieu du pied, vers le bord. Rend vrai si c'est fait.
	 */
	private static boolean poserSurLeLit(CompanionEntity fille, ServerLevel monde, @org.jetbrains.annotations.Nullable BlockPos tête) {
		if (tête == null) {
			return false;
		}
		BlockState état = monde.getBlockState(tête);
		if (!(état.getBlock() instanceof BedBlock)) {
			return false;
		}
		Direction versLaTête = état.getValue(BedBlock.FACING);
		if (état.getValue(BedBlock.PART) == BedPart.FOOT) {
			tête = tête.relative(versLaTête);
		}
		BlockPos pied = tête.relative(versLaTête.getOpposite());
		Vec3 là = Vec3.atBottomCenterOf(pied)
				.add(Vec3.atLowerCornerOf(versLaTête.getOpposite().getUnitVec3i()).scale(4.0 / 16.0))
				.add(0, 9.0 / 16.0, 0);
		float lacet = versLaTête.toYRot();
		fille.snapTo(là.x, là.y, là.z, lacet, 0.0F);
		fille.setYBodyRot(lacet);
		fille.setYHeadRot(lacet);
		return true;
	}

	@org.jetbrains.annotations.Nullable
	public static CompanionEntity réveiller(ServerPlayer joueur, boolean toutes,
	                                        @org.jetbrains.annotations.Nullable BlockPos lit) {
		ServerLevel monde = joueur.level();
		PlayerProgress progrès = ModAttachments.get(joueur);
		CompanionEntity fille = monde.getEntitiesOfClass(CompanionEntity.class, new AABB(joueur.blockPosition()).inflate(24),
						f -> !f.hasScene() && !f.isCuffed() && !f.estEnVeille() && f.peutQuitterSaPlace() && f.joueLesScènes()
								&& CompanionScenes.knows(f.kind(), SCÈNE)
								&& (toutes || joueur.getUUID().equals(f.getCompanionUuid())
								|| progrès.trust(f.kind().id()) >= PlayerProgress.hugTrust(f.kind())))
				.stream()
				// La sienne d'abord, puis la plus proche.
				.min(Comparator.comparingDouble(f -> joueur.getUUID().equals(f.getCompanionUuid())
						? f.distanceToSqr(joueur) - 10000 : f.distanceToSqr(joueur)))
				.orElse(null);
		if (fille == null) {
			if (!toutes) {
				pourquoi(joueur);
			}
			return null;
		}
		fille.stopScene();
		fille.getNavigation().stop();
		fille.setMode(JennyMode.IDLE);
		if (poserSurLeLit(fille, monde, lit)) {
			fille.setAnchorPos(fille.blockPosition());
			fille.playScene(SCÈNE, joueur);
			fille.joinScene(joueur);
			dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "reveil");
			dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "wakeup");
			return fille;
		}
		// Pas de lit (la commande, loin du lit) : par terre, devant lui, steve
		// pixels devant elle dans le modèle. Huit directions, en partant de
		// celle où il regarde, à un bloc puis un peu plus près ou plus loin.
		// Rien de libre (une chambre étroite, le lit, un mur) : elle reste où
		// elle est, tournée vers lui. Il monte de toute façon à sa place —
		// c'est elle qui porte la scène —, l'endroit n'est qu'une question de
		// décor. Avant, faute de place, personne ne venait, sans un mot.
		Vec3 pieds = joueur.position();
		Vec3 choisie = null;
		float face = 0;
		for (double distance : new double[] {1.0, 0.85, 1.2}) {
			for (int huitième = 0; huitième < 8 && choisie == null; huitième++) {
				float lacet = joueur.getYRot() + huitième * 45.0F;
				double rad = Math.toRadians(lacet);
				Vec3 là = pieds.add(-Math.sin(rad) * distance * fille.getScale(), 0,
						Math.cos(rad) * distance * fille.getScale());
				if (monde.noCollision(fille, fille.getType().getDimensions().makeBoundingBox(là))) {
					choisie = là;
					face = lacet + 180.0F;
				}
			}
		}
		if (choisie == null) {
			choisie = fille.position();
			face = (float) Math.toDegrees(Math.atan2(pieds.z - choisie.z, pieds.x - choisie.x)) - 90.0F;
		}
		fille.stopScene();
		fille.getNavigation().stop();
		fille.setMode(JennyMode.IDLE);
		fille.snapTo(choisie.x, choisie.y, choisie.z, face, 0.0F);
		fille.setYBodyRot(face);
		fille.setYHeadRot(face);
		fille.setAnchorPos(fille.blockPosition());
		fille.playScene(SCÈNE, joueur);
		fille.joinScene(joueur);
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "reveil");
		dev.pyrofetch.jn1.interaction.Conversation.événement(joueur, fille, "wakeup");
		return fille;
	}

	/**
	 * Personne n'est venu : s'il y a une fille dans les parages, on lui dit
	 * pourquoi — sans cela, le joueur ne pouvait pas savoir ce qui manquait.
	 */
	private static void pourquoi(ServerPlayer joueur) {
		java.util.List<CompanionEntity> proches = joueur.level().getEntitiesOfClass(CompanionEntity.class,
				new AABB(joueur.blockPosition()).inflate(64), f -> true);
		if (proches.isEmpty()) {
			return;
		}
		CompanionEntity plus = proches.stream().min(Comparator.comparingDouble(f -> f.distanceToSqr(joueur))).get();
		String raison;
		if (plus.distanceToSqr(joueur) > 24 * 24) {
			raison = "loin";
		} else if (plus.hasScene()) {
			raison = "scene";
		} else if (plus.isCuffed()) {
			raison = "menottes";
		} else if (!CompanionScenes.knows(plus.kind(), SCÈNE)) {
			raison = "scene_inconnue";
		} else {
			raison = "affection";
		}
		joueur.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jn1.reveil." + raison, plus.getName()));
	}
}
