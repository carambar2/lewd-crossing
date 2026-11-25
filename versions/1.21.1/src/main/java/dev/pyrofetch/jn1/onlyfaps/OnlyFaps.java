package dev.pyrofetch.jn1.onlyfaps;

import dev.pyrofetch.jn1.block.ComputerBlock;
import dev.pyrofetch.jn1.network.OnlyFapsEtatPayload;
import dev.pyrofetch.jn1.network.OnlyFapsPayload;
import dev.pyrofetch.jn1.photo.PhotoArchive;
import dev.pyrofetch.jn1.photo.PhotoItem;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.shop.ShopEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OnlyFaps : on y publie, depuis le terminal, les photos prises avec l'appareil,
 * contre des émeraudes — la monnaie de la boutique.
 *
 * <p>Le prix vient de ce qui est sur la photo, relevé par le serveur à la prise
 * ({@link Sujets}) : la tenue, la scène, les traces, les menottes, le cadrage,
 * et quelques filles de plus. Deux freins à la fabrique : une photo ne se
 * publie qu'une fois, et les abonnés se lassent d'un même contenu (fille et
 * scène, ou fille et tenue) — chaque publication retire 40 % à la suivante,
 * et la lassitude retombe d'un cran par jour. Les abonnés, eux, s'accumulent
 * et font monter tous les prix, jusqu'au double.
 */
public final class OnlyFaps {
	private static final double PORTÉE_CARRÉE = 36.0;
	private static final double LASSITUDE = 0.6;
	private static final int ABONNÉS_DOUBLE = 1000;
	/** Combien de commentaires par catégorie dans les langues. */
	private static final int COMMENTAIRES = 4;

	private OnlyFaps() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(OnlyFapsPayload.ID,
				(payload, context) -> context.player().server.execute(() -> recevoir(context.player(), payload)));
	}

	private static void recevoir(ServerPlayerEntity joueur, OnlyFapsPayload demande) {
		if (joueur.squaredDistanceTo(Vec3d.ofCenter(demande.terminal())) > PORTÉE_CARRÉE
				|| !(joueur.getServerWorld().getBlockState(demande.terminal()).getBlock() instanceof ComputerBlock)) {
			return;
		}
		if (demande.action() == OnlyFapsPayload.PUBLIER) {
			publier(joueur, demande.numéro());
		} else {
			envoyer(joueur, "", 0);
		}
	}

	// ------------------------------------------------------------------ prix

	/** La valeur d'une fille sur la photo, avant le cadrage. */
	static double valeur(Sujet sujet) {
		double v = 1 + Math.round(3 * sujet.nudité()) + sujet.traces() + (sujet.menottée() ? 2 : 0);
		String scène = sujet.scène();
		if (!scène.isEmpty()) {
			v += scène.endsWith("cu") ? 8 : scène.endsWith("_r") ? 6 : scène.equals("jn1_pole") ? 3 : 4;
		}
		float c = sujet.cadrage();
		return v * (c >= 0.55f ? 1.5 : c >= 0.3f ? 1.0 : c >= 0.12f ? 0.6 : 0.25);
	}

	/** Ce dont les abonnés se lassent : la fille principale, et sa scène ou sa tenue. */
	static String contenu(List<Sujet> sujets) {
		Sujet premier = sujets.get(0);
		return premier.fille() + ":" + (premier.enScène() ? premier.scène() : premier.tenue());
	}

	static int prix(List<Sujet> sujets, Compte compte) {
		if (sujets == null || sujets.isEmpty()) {
			return 0;
		}
		List<Double> valeurs = new ArrayList<>(sujets.stream().map(OnlyFaps::valeur).toList());
		valeurs.sort((a, b) -> Double.compare(b, a));
		double total = valeurs.stream().limit(3).mapToDouble(Double::doubleValue).sum()
				+ 2 * Math.min(2, sujets.size() - 1);
		total *= Math.pow(LASSITUDE, compte.lassitude(contenu(sujets)));
		total *= 1 + Math.min(1.0, compte.abonnés() / (double) ABONNÉS_DOUBLE);
		return Math.max(1, (int) Math.round(total));
	}

	// ------------------------------------------------------------ guichet

	private static Compte compte(ServerPlayerEntity joueur) {
		long jour = joueur.getServerWorld().getTimeOfDay() / 24000L;
		return joueur.getAttachedOrCreate(ModAttachments.ONLYFAPS).au(jour);
	}

	/** Les tirages de l'inventaire, un par numéro, dans l'ordre des cases. */
	private static List<Offre> offres(ServerPlayerEntity joueur, Compte compte) {
		PhotoArchive archive = PhotoArchive.of(joueur.getServerWorld());
		Set<Integer> numéros = new LinkedHashSet<>();
		var inventaire = joueur.getInventory();
		for (int case_ = 0; case_ < inventaire.size(); case_++) {
			ItemStack pile = inventaire.getStack(case_);
			if (pile.isOf(dev.pyrofetch.jn1.registry.ModItems.PHOTO) && PhotoItem.estDéveloppée(pile)) {
				numéros.add(PhotoItem.numéro(pile));
			}
		}
		List<Offre> offres = new ArrayList<>();
		for (int numéro : numéros) {
			var image = archive.get(numéro);
			if (image == null) {
				continue;
			}
			List<Sujet> sujets = archive.sujets(numéro);
			offres.add(new Offre(numéro, image.format(), sujets == null ? 0 : prix(sujets, compte),
					archive.publiée(numéro), sujets == null, sujets == null ? List.of() : sujets));
		}
		return offres;
	}

	private static void envoyer(ServerPlayerEntity joueur, String commentaire, int gain) {
		Compte compte = compte(joueur);
		ServerPlayNetworking.send(joueur, new OnlyFapsEtatPayload(compte.abonnés(), offres(joueur, compte), commentaire, gain));
	}

	private static void publier(ServerPlayerEntity joueur, int numéro) {
		PhotoArchive archive = PhotoArchive.of(joueur.getServerWorld());
		boolean tenue = offres(joueur, Compte.NEUF).stream().anyMatch(o -> o.numéro() == numéro);
		List<Sujet> sujets = archive.sujets(numéro);
		if (!tenue || sujets == null || archive.publiée(numéro)) {
			envoyer(joueur, "", 0);
			return;
		}
		if (sujets.isEmpty()) {
			// Personne dessus : on ne paie pas, et la photo reste à publier — rien n'est perdu.
			envoyer(joueur, "onlyfaps.jn1.comment.vide", 0);
			return;
		}
		Compte compte = compte(joueur);
		int prix = prix(sujets, compte);
		String commentaire = commentaire(sujets, compte, joueur);
		archive.publier(numéro);
		int nouveaux = prix * 4 + joueur.getRandom().nextInt(prix + 1);
		joueur.setAttached(ModAttachments.ONLYFAPS, compte.publié(contenu(sujets), nouveaux));
		ItemStack émeraudes = new ItemStack(ShopEntry.CURRENCY, prix);
		while (!émeraudes.isEmpty()) {
			ItemStack part = émeraudes.split(émeraudes.getMaxCount());
			if (!joueur.getInventory().insertStack(part)) {
				joueur.dropItem(part, false);
			}
		}
		joueur.getServerWorld().playSound(null, joueur.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
				SoundCategory.PLAYERS, 0.8f, 0.9f + joueur.getRandom().nextFloat() * 0.3f);
		envoyer(joueur, commentaire, prix);
	}

	/** Un abonné réagit, selon ce qu'il voit — ou qu'il en a assez vu. */
	private static String commentaire(List<Sujet> sujets, Compte compte, ServerPlayerEntity joueur) {
		Sujet s = sujets.get(0);
		String catégorie;
		if (compte.lassitude(contenu(sujets)) >= 2) {
			catégorie = "lasse";
		} else if (sujets.size() >= 2) {
			catégorie = "groupe";
		} else if (s.scène().endsWith("cu")) {
			catégorie = "fin";
		} else if (s.enScène()) {
			catégorie = "scene";
		} else if (s.menottée()) {
			catégorie = "menottes";
		} else if (s.traces() > 0) {
			catégorie = "traces";
		} else if (s.cadrage() < 0.12f) {
			catégorie = "loin";
		} else if (s.nudité() >= 0.66f) {
			catégorie = "nue";
		} else {
			catégorie = "simple";
		}
		return "onlyfaps.jn1.comment." + catégorie + "." + joueur.getRandom().nextInt(COMMENTAIRES);
	}
}
