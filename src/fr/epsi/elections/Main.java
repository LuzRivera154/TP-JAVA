package fr.epsi.elections;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		System.out.println("Application de résultats électoraux");

		// Lecture de l'annotation @InfoTP (une classe annotée et une qui ne l'est pas)
		afficherInfoTP(ServiceResultats.class);
		afficherInfoTP(Main.class);

		ConfigApp config = ConfigApp.getInstance();
		Path fichierResultats = Path.of(config.getDossierDonnees(), "resultats.csv");

		ServiceResultats service;
		try {
			service = JeuDemonstration.creerService();
		} catch (DonneeElectoraleInvalideException e) {
			System.out.println("Jeu de démonstration invalide : " + e.getMessage());
			System.exit(1);
			return;
		}

		// Premier import : en cas d'échec, on ne démarre pas le serveur
		try {
			int nombre = service.importer(fichierResultats);
			System.out.println("Import réussi : " + nombre + " résultats chargés depuis " + fichierResultats);
		} catch (IOException e) {
			System.out.println("Impossible de lire le fichier " + fichierResultats + " : " + e);
			System.out.println("Le serveur n'est pas démarré.");
			System.exit(1);
			return;
		} catch (NombreInvalideException e) {
			System.out.println("Conversion impossible : " + e.getMessage());
			System.out.println("Le serveur n'est pas démarré.");
			System.exit(1);
			return;
		} catch (DonneeElectoraleInvalideException e) {
			System.out.println("Données refusées : " + e.getMessage());
			System.out.println("Le serveur n'est pas démarré.");
			System.exit(1);
			return;
		}

		demonstration(service);

		// "--sans-serveur" : uniquement la démonstration console
		if (args.length > 0 && "--sans-serveur".equals(args[0])) {
			return;
		}

		try {
			HttpServer serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", config.getPort()), 0);
			serveur.createContext("/", new ElectionHandler(service));
			serveur.start();
			System.out.println();
			System.out.println("Serveur démarré sur http://127.0.0.1:" + config.getPort() + " (Ctrl+C pour arrêter)");
			System.out.println("  http://127.0.0.1:" + config.getPort() + "/territoires");
			System.out.println("  http://127.0.0.1:" + config.getPort() + "/candidats?election=PRES-01");
			System.out.println("  http://127.0.0.1:" + config.getPort() + "/resultats?election=PRES-01&commune=C-R&format=json");
		} catch (IOException e) {
			System.out.println("Impossible de démarrer le serveur sur le port " + config.getPort() + " : " + e.getMessage());
			System.exit(1);
		}
	}

	private static void afficherInfoTP(Class<?> classe) {
		InfoTP info = classe.getAnnotation(InfoTP.class);
		if (info == null) {
			System.out.println("[InfoTP] " + classe.getSimpleName() + " : pas d'annotation @InfoTP");
			return;
		}
		String objectif = info.objectif();
		if (objectif.isEmpty()) {
			objectif = "non précisé";
		}
		System.out.println("[InfoTP] " + classe.getSimpleName() + " : auteur = " + info.auteur() + ", objectif = " + objectif);
	}

	private static void demonstration(ServiceResultats service) {
		titre("Territoires");
		for (Territoire territoire : service.getGeographie().getTerritoires()) {
			System.out.println("  " + territoire);
		}
		Territoire nord = service.getGeographie().trouver("R-N");
		System.out.println("  Bureaux de " + nord.getCode() + " : " + nord.getBureaux().size());
		for (BureauVote bureau : nord.getBureaux()) {
			System.out.println("    - " + bureau.getCode() + " (" + bureau.getNom() + ")");
		}

		titre("Candidats (appel polymorphe de getLibelle)");
		for (Election election : service.getElectoral().getElections()) {
			System.out.println("  " + election);
			List<Candidat> candidats = election.getCandidats();
			for (Candidat candidat : candidats) {
				System.out.println("    " + candidat.getCode() + " -> " + candidat.getLibelle() + " [" + candidat.getType() + "]");
			}
		}

		titre("Résultats de référence (format texte)");
		ServiceAffichage texte = new ServiceAffichage(FabriqueConvertisseur.getConvertisseur("texte"));
		String[][] cas = {
				{"PRES-01", "BV-01"}, {"PRES-01", "C-R"}, {"PRES-01", "R-N"},
				{"PRES-01", "TOUS"}, {"LEG-01", "BV-01"}, {"LEG-01", "TOUS"}
		};
		for (String[] unCas : cas) {
			System.out.println(texte.afficher(calculer(service, unCas[0], unCas[1])));
		}

		titre("Même synthèse au format JSON (on change seulement la stratégie)");
		ServiceAffichage json = new ServiceAffichage(FabriqueConvertisseur.getConvertisseur("json"));
		System.out.println(json.afficher(calculer(service, "PRES-01", "C-R")));

		titre("Import invalide pendant l'exécution");
		Path fichierInvalide = Path.of(ConfigApp.getInstance().getDossierDonnees(), "tests", "pres_bv02_une_ligne.csv");
		try {
			service.importer(fichierInvalide);
			System.out.println("  Import accepté (inattendu)");
		} catch (IOException | NombreInvalideException | DonneeElectoraleInvalideException e) {
			System.out.println("  Import refusé : " + e.getMessage());
		}
		System.out.println("  Données actives inchangées : " + service.getResultats().size()
				+ " résultats, total C-R = " + calculer(service, "PRES-01", "C-R").getTotalVoix());

		titre("Export puis réimport");
		Path fichierExport = Path.of(ConfigApp.getInstance().getDossierDonnees(), "export_resultats.csv");
		try {
			service.exporter(fichierExport);
			// La réussite n'est annoncée qu'après la fermeture du fichier (fin du try-with-resources)
			System.out.println("  Export terminé : " + fichierExport);
			int relus = service.importer(fichierExport);
			System.out.println("  Réimport : " + relus + " résultats relus");
		} catch (IOException | NombreInvalideException | DonneeElectoraleInvalideException e) {
			System.out.println("  Échec : " + e.getMessage());
		}

		titre("Singleton");
		System.out.println("  Même instance : " + (ConfigApp.getInstance() == ConfigApp.getInstance()));
	}

	private static Synthese calculer(ServiceResultats service, String codeElection, String codeTerritoire) {
		Election election = service.getElectoral().trouverElection(codeElection);
		Territoire territoire = null;
		if (!ReferentielGeographique.CODE_TOUS.equals(codeTerritoire)) {
			territoire = service.getGeographie().trouver(codeTerritoire);
		}
		return service.calculer(election, territoire);
	}

	private static void titre(String texte) {
		System.out.println();
		System.out.println("===== " + texte + " =====");
	}
}
