package fr.epsi.elections;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Vérifications de la section 10 : chaque test affiche OK ou ERREUR
public class Verifications {
    private static int nombreOk = 0;
    private static int nombreErreurs = 0;
    private static final Path DOSSIER_TESTS = Path.of(ConfigApp.getInstance().getDossierDonnees(), "tests");
    private static final Path FICHIER_REFERENCE = Path.of(ConfigApp.getInstance().getDossierDonnees(), "resultats.csv");

    public static void main(String[] args) throws Exception {
        verifierReference();
        verifierCentreEtDepartement();
        verifierVoixAZero();
        verifierImportsValides();
        verifierImportsRefuses();
        verifierExport();
        verifierBureaux();
        verifierCandidats();
        verifierConfiguration();
        verifierFormats();
        verifierHttp();

        System.out.println();
        System.out.println("Bilan : " + nombreOk + " OK, " + nombreErreurs + " ERREUR(S)");
        if (nombreErreurs > 0) {
            System.exit(1);
        }
    }

    private static void verifier(String description, boolean condition) {
        if (condition) {
            nombreOk++;
            System.out.println("[OK]     " + description);
        } else {
            nombreErreurs++;
            System.out.println("[ERREUR] " + description);
        }
    }

    private static void titre(String texte) {
        System.out.println();
        System.out.println("--- " + texte + " ---");
    }

    private static ServiceResultats serviceAvecReference() throws Exception {
        ServiceResultats service = JeuDemonstration.creerService();
        service.importer(FICHIER_REFERENCE);
        return service;
    }

    private static Synthese calculer(ServiceResultats service, String codeElection, String codeTerritoire) {
        Election election = service.getElectoral().trouverElection(codeElection);
        Territoire territoire = null;
        if (!"TOUS".equals(codeTerritoire)) {
            territoire = service.getGeographie().trouver(codeTerritoire);
        }
        return service.calculer(election, territoire);
    }

    // Résumé simple d'une synthèse, par exemple "CP-01=210 CP-02=190 total=400 tete=CP-01"
    private static String resume(Synthese synthese) {
        String texte = "";
        for (LigneResultat ligne : synthese.getResultats()) {
            texte += ligne.getCandidat().getCode() + "=" + ligne.getVoix() + " ";
        }
        texte += "total=" + synthese.getTotalVoix() + " tete=";
        List<Candidat> enTete = synthese.getEnTete();
        for (int i = 0; i < enTete.size(); i++) {
            if (i > 0) {
                texte += ",";
            }
            texte += enTete.get(i).getCode();
        }
        return texte;
    }

    private static double pourcentage(Synthese synthese, String codeCandidat) {
        for (LigneResultat ligne : synthese.getResultats()) {
            if (ligne.getCandidat().getCode().equals(codeCandidat)) {
                return ligne.getPourcentageArrondi();
            }
        }
        return -1;
    }

    // Liste triée des lignes "election;bureau;candidat;voix" : l'ordre ne compte pas pour comparer
    private static List<String> triplets(List<ResultatElection> resultats) {
        List<String> lignes = new ArrayList<>();
        for (ResultatElection resultat : resultats) {
            lignes.add(resultat.toString());
        }
        Collections.sort(lignes);
        return lignes;
    }

    private static void verifierReference() throws Exception {
        titre("Jeu de référence (section 9.4)");
        ServiceResultats service = serviceAvecReference();

        Synthese s = calculer(service, "PRES-01", "BV-01");
        verifier("PRES-01 / BV-01 : " + resume(s), resume(s).equals("CP-01=120 CP-02=80 total=200 tete=CP-01"));

        s = calculer(service, "PRES-01", "C-R");
        verifier("PRES-01 / C-R : " + resume(s) + " " + pourcentage(s, "CP-01") + " %",
                resume(s).equals("CP-01=210 CP-02=190 total=400 tete=CP-01") && pourcentage(s, "CP-01") == 52.5);

        s = calculer(service, "PRES-01", "R-N");
        verifier("PRES-01 / R-N : " + resume(s) + " " + pourcentage(s, "CP-01") + " %",
                resume(s).equals("CP-01=270 CP-02=230 total=500 tete=CP-01") && pourcentage(s, "CP-01") == 54.0);

        s = calculer(service, "PRES-01", "TOUS");
        verifier("PRES-01 / TOUS : " + resume(s) + " " + pourcentage(s, "CP-02") + " %",
                resume(s).equals("CP-01=340 CP-02=360 total=700 tete=CP-02") && pourcentage(s, "CP-02") == 51.43);

        s = calculer(service, "LEG-01", "BV-01");
        verifier("LEG-01 / BV-01 (égalité) : " + resume(s),
                resume(s).equals("CL-01=100 CL-02=100 total=200 tete=CL-01,CL-02")
                        && pourcentage(s, "CL-01") == 50.0 && pourcentage(s, "CL-02") == 50.0);

        s = calculer(service, "LEG-01", "TOUS");
        verifier("LEG-01 / TOUS : " + resume(s) + " " + pourcentage(s, "CL-02") + " %",
                resume(s).equals("CL-01=325 CL-02=375 total=700 tete=CL-02") && pourcentage(s, "CL-02") == 53.57);
    }

    private static void verifierCentreEtDepartement() throws Exception {
        titre("Centre et département : chaque bureau compté une fois");
        ServiceResultats service = serviceAvecReference();

        Synthese centre = calculer(service, "PRES-01", "CV-R");
        verifier("PRES-01 / CV-R : 2 bureaux, " + resume(centre),
                centre.getBureauxTotal() == 2 && resume(centre).equals("CP-01=210 CP-02=190 total=400 tete=CP-01"));

        Synthese departement = calculer(service, "PRES-01", "D-N");
        verifier("PRES-01 / D-N : 3 bureaux, " + resume(departement),
                departement.getBureauxTotal() == 3 && resume(departement).equals("CP-01=270 CP-02=230 total=500 tete=CP-01"));
    }

    private static void verifierVoixAZero() throws Exception {
        titre("Toutes les voix d'un bureau à zéro");
        ServiceResultats service = JeuDemonstration.creerService();
        service.importer(DOSSIER_TESTS.resolve("zero_pres_bv04.csv"));
        Synthese s = calculer(service, "PRES-01", "BV-04");
        verifier("PRES-01 / BV-04 : renseigné, total 0, pourcentages 0, personne en tête",
                s.getCouverture() == Couverture.COMPLET && s.getTotalVoix() == 0 && s.getResultats().size() == 2
                        && pourcentage(s, "CP-01") == 0.0 && pourcentage(s, "CP-02") == 0.0 && s.getEnTete().isEmpty());
    }

    private static void verifierImportsValides() throws Exception {
        titre("Imports valides particuliers");
        ServiceResultats service = JeuDemonstration.creerService();
        service.importer(DOSSIER_TESTS.resolve("sans_pres_bv02.csv"));
        Synthese s = calculer(service, "PRES-01", "C-R");
        verifier("Sans PRES-01 / BV-02 : C-R PARTIEL, 1 bureau sur 2, " + resume(s),
                s.getCouverture() == Couverture.PARTIEL && s.getBureauxRenseignes() == 1 && s.getBureauxTotal() == 2
                        && resume(s).equals("CP-01=120 CP-02=80 total=200 tete=CP-01"));
        Synthese leg = calculer(service, "LEG-01", "C-R");
        verifier("Sans PRES-01 / BV-02 : LEG-01 / C-R reste COMPLET", leg.getCouverture() == Couverture.COMPLET);

        service.importer(DOSSIER_TESTS.resolve("entete_seul.csv"));
        s = calculer(service, "PRES-01", "TOUS");
        verifier("En-tête seul : 0 résultat, AUCUNE_DONNEE, listes vides",
                service.getResultats().isEmpty() && s.getCouverture() == Couverture.AUCUNE_DONNEE
                        && s.getTotalVoix() == 0 && s.getResultats().isEmpty() && s.getEnTete().isEmpty());
    }

    private static void verifierImportsRefuses() throws Exception {
        titre("Imports refusés (les données actives ne changent pas)");
        verifierRefus("pres_bv02_une_ligne.csv", "Groupe incomplet pour l'élection PRES-01 et le bureau BV-02");
        verifierRefus("voix_negative.csv", "positif ou nul");
        verifierRefus("voix_non_numerique.csv", "nombre de voix invalide 'abc'");
        verifierRefus("voix_hors_int.csv", "nombre de voix invalide '3000000000'");
        verifierRefus("doublon.csv", "doublon");
        verifierRefus("candidat_non_inscrit.csv", "CL-02 n'est pas inscrit à l'élection PRES-01");
        verifierRefus("bureau_inconnu.csv", "bureau de vote inconnu BV-99");
        verifierRefus("fichier_absent.csv", "fichier_absent.csv");
        verifierRefus("vide.csv", "Fichier vide");
        verifierRefus("entete_incorrect.csv", "en-tête incorrect");
        verifierRefus("colonne_manquante.csv", "3 colonne(s) au lieu de 4");
        verifierRefus("colonne_supplementaire.csv", "5 colonne(s) au lieu de 4");
        verifierRefus("ligne_vide.csv", "ligne vide");
        verifierRefus("derniere_ligne_incorrecte.csv", "Ligne 17");
    }

    private static void verifierRefus(String nomFichier, String extraitMessage) throws Exception {
        ServiceResultats service = serviceAvecReference();
        List<String> avant = triplets(service.getResultats());
        String message;
        try {
            service.importer(DOSSIER_TESTS.resolve(nomFichier));
            message = "import accepté";
        } catch (IOException e) {
            message = "IOException : " + e;
        } catch (NombreInvalideException e) {
            message = "NombreInvalideException : " + e.getMessage();
        } catch (DonneeElectoraleInvalideException e) {
            message = "DonneeElectoraleInvalideException : " + e.getMessage();
        }
        boolean inchange = triplets(service.getResultats()).equals(avant);
        verifier(nomFichier + " -> " + message, message.contains(extraitMessage) && inchange);
    }

    private static void verifierExport() throws Exception {
        titre("Export puis réimport");
        ServiceResultats service = serviceAvecReference();
        Path export = Path.of(ConfigApp.getInstance().getDossierDonnees(), "export_resultats.csv");
        service.exporter(export);
        ServiceResultats relu = JeuDemonstration.creerService();
        relu.importer(export);
        verifier("Mêmes triplets et mêmes voix après réimport", triplets(relu.getResultats()).equals(triplets(service.getResultats())));

        ServiceResultats ordre = JeuDemonstration.creerService();
        ordre.importer(DOSSIER_TESTS.resolve("ordre_different.csv"));
        verifier("Lignes dans un autre ordre : mêmes triplets", triplets(ordre.getResultats()).equals(triplets(service.getResultats())));

        // Jeu vide : l'export doit pouvoir être réimporté
        ServiceResultats vide = JeuDemonstration.creerService();
        vide.importer(DOSSIER_TESTS.resolve("entete_seul.csv"));
        Path exportVide = Files.createTempFile("export_vide", ".csv");
        vide.exporter(exportVide);
        int relusVide = JeuDemonstration.creerService().importer(exportVide);
        Files.delete(exportVide);
        verifier("Export d'un jeu vide puis réimport : 0 résultat", relusVide == 0);
    }

    private static void verifierBureaux() throws Exception {
        titre("Identité des bureaux et référentiel géographique");
        ReferentielGeographique geographie = JeuDemonstration.creerGeographie();
        BureauVote existant = geographie.trouverBureau("BV-01");
        // Constructeur à deux paramètres : le nom par défaut est le code
        BureauVote copie = new BureauVote("BV-01", existant.getCentre());
        verifier("Deux objets BV-01 : == faux, equals vrai, même hashCode",
                existant != copie && existant.equals(copie) && existant.hashCode() == copie.hashCode());
        verifier("Nom par défaut = code (this(...)) : " + copie.getNom(), copie.getNom().equals("BV-01"));

        try {
            geographie.ajouter(copie);
            verifier("Inscription en double de BV-01 refusée", false);
        } catch (DonneeElectoraleInvalideException e) {
            verifier("Inscription en double de BV-01 refusée : " + e.getMessage(), existant.getCentre().getBureaux().size() == 2);
        }

        try {
            Region inconnue = new Region("R-X", "Région absente");
            geographie.ajouter(new Departement("D-X", "Département orphelin", inconnue));
            verifier("Parent absent du référentiel refusé", false);
        } catch (DonneeElectoraleInvalideException e) {
            verifier("Parent absent du référentiel refusé : " + e.getMessage(), true);
        }

        try {
            new Departement("D-Y", "Sans région", null);
            verifier("Département sans région refusé", false);
        } catch (IllegalArgumentException e) {
            verifier("Département sans région refusé : " + e.getMessage(), true);
        }
    }

    private static void verifierCandidats() throws Exception {
        titre("Candidats et type d'élection");
        Election presidentielle = new Election("PRES-T", "Test", TypeElection.PRESIDENTIELLE);
        try {
            presidentielle.ajouterCandidat(new CandidatListe("CL-T", "Liste test", 2));
            verifier("Liste refusée dans une présidentielle", false);
        } catch (DonneeElectoraleInvalideException e) {
            verifier("Liste refusée dans une présidentielle : " + e.getMessage(), true);
        }

        ReferentielElectoral electoral = JeuDemonstration.creerElections();
        try {
            electoral.inscrire("PRES-01", new CandidatPersonne("CP-03", "Nouveau", "Candidat"));
            verifier("Inscription refusée après fermeture des inscriptions", false);
        } catch (DonneeElectoraleInvalideException e) {
            verifier("Inscription refusée après fermeture des inscriptions : " + e.getMessage(), true);
        }

        // Le code CP-01 est déjà pris par un candidat de PRES-01
        Election autre = new Election("PRES-02", "Autre élection", TypeElection.PRESIDENTIELLE);
        autre.ajouterCandidat(new CandidatPersonne("CP-01", "Homonyme"));
        try {
            electoral.ajouterElection(autre);
            verifier("Code de candidat déjà utilisé dans une autre élection refusé", false);
        } catch (DonneeElectoraleInvalideException e) {
            verifier("Code de candidat déjà utilisé dans une autre élection refusé : " + e.getMessage(),
                    electoral.trouverElection("PRES-02") == null);
        }

        // Un code avec un point-virgule rendrait l'export impossible à relire
        try {
            new CandidatPersonne("CP;01", "Test");
            verifier("Code de candidat avec ; refusé", false);
        } catch (IllegalArgumentException e) {
            verifier("Code de candidat avec ; refusé : " + e.getMessage(), true);
        }
        try {
            new Election("PRES;01", "Test", TypeElection.PRESIDENTIELLE);
            verifier("Code d'élection avec ; refusé", false);
        } catch (IllegalArgumentException e) {
            verifier("Code d'élection avec ; refusé : " + e.getMessage(), true);
        }
        try {
            new Region("R;N", "Test");
            verifier("Code de territoire avec ; refusé", false);
        } catch (IllegalArgumentException e) {
            verifier("Code de territoire avec ; refusé : " + e.getMessage(), true);
        }
    }

    private static void verifierConfiguration() {
        titre("Singleton");
        ConfigApp premiere = ConfigApp.getInstance();
        ConfigApp deuxieme = ConfigApp.getInstance();
        verifier("Deux appels à getInstance() : même référence", premiere == deuxieme);
        verifier("Port 8080 et dossier data", premiere.getPort() == 8080 && premiere.getDossierDonnees().equals("data"));
    }

    private static void verifierFormats() throws Exception {
        titre("Strategy, fabrique et caractères spéciaux");
        ServiceResultats service = serviceAvecReference();
        Synthese s = calculer(service, "PRES-01", "C-R");
        String json = new ServiceAffichage(FabriqueConvertisseur.getConvertisseur("json")).afficher(s);
        String texte = new ServiceAffichage(FabriqueConvertisseur.getConvertisseur("texte")).afficher(s);
        verifier("Même synthèse en JSON et en texte", json.contains("\"totalVoix\": 400") && texte.contains("Total des voix connues : 400"));
        verifier("Format absent -> JSON par défaut", FabriqueConvertisseur.getConvertisseur(null) instanceof ResultatsJson);
        try {
            FabriqueConvertisseur.getConvertisseur("xml");
            verifier("Format inconnu refusé", false);
        } catch (IllegalArgumentException e) {
            verifier("Format inconnu refusé : " + e.getMessage(), true);
        }

        Candidat special = new CandidatListe("CL-S", "Liste \"Égalité\" \\ Sud", 2);
        String attendu = "\"Liste \\\"Égalité\\\" \\\\ Sud (2 membres)\"";
        verifier("Libellé avec accent, guillemets et \\ : " + ResultatsJson.chaine(special.getLibelle()),
                ResultatsJson.chaine(special.getLibelle()).equals(attendu));
    }

    private static void verifierHttp() throws Exception {
        titre("HTTP");
        ServiceResultats service = serviceAvecReference();
        int port = ConfigApp.getInstance().getPort();
        HttpServer serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        serveur.createContext("/", new ElectionHandler(service));
        serveur.start();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        String base = "http://127.0.0.1:" + port;

        try {
            HttpResponse<String> r = envoyer(client, "GET", base + "/territoires");
            verifier("GET /territoires -> 200 JSON, parent null pour R-N",
                    r.statusCode() == 200 && estJson(r) && r.body().contains("{\"code\": \"R-N\", \"nom\": \"Région du Nord\", \"type\": \"REGION\", \"parent\": null}"));

            r = envoyer(client, "GET", base + "/candidats?election=LEG-01");
            verifier("GET /candidats?election=LEG-01 -> 200, libellé accentué, type LISTE",
                    r.statusCode() == 200 && r.body().contains("Élan citoyen (3 membres)") && r.body().contains("\"type\": \"LISTE\""));

            r = envoyer(client, "GET", base + "/resultats?election=PRES-01&commune=C-R&format=json");
            String reference = r.body();
            verifier("GET /resultats C-R json -> 200, totalVoix 400", r.statusCode() == 200 && estJson(r) && reference.contains("\"totalVoix\": 400"));

            r = envoyer(client, "GET", base + "/resultats?format=json&commune=C-R&election=PRES-01");
            verifier("Paramètres dans un autre ordre -> même réponse", r.statusCode() == 200 && r.body().equals(reference));

            r = envoyer(client, "GET", base + "/resultats?election=PRES-01");
            verifier("Sans territoire ni format -> TOUS en JSON", r.statusCode() == 200 && estJson(r) && r.body().contains("\"territoire\": \"TOUS\""));

            r = envoyer(client, "GET", base + "/resultats?election=PRES-01&territoire=R-N&format=texte");
            verifier("format=texte -> text/plain, total 500", r.statusCode() == 200
                    && r.headers().firstValue("Content-Type").orElse("").equals("text/plain; charset=UTF-8")
                    && r.body().contains("Total des voix connues : 500"));

            r = envoyer(client, "GET", base + "/resultats?election=PRES%2D01&bv=BV%2D01");
            verifier("Paramètres encodés (%2D) décodés -> 200", r.statusCode() == 200 && r.body().contains("\"totalVoix\": 200"));

            verifierStatut(client, base + "/resultats", 400, "election absent");
            verifierStatut(client, base + "/resultats?election=", 400, "election vide");
            verifierStatut(client, base + "/resultats?election=PRES-01&election=LEG-01", 400, "election répété");
            verifierStatut(client, base + "/resultats?election=PRES-01&format=xml", 400, "format inconnu");
            verifierStatut(client, base + "/resultats?election=PRES-01&format=", 400, "format vide");
            verifierStatut(client, base + "/resultats?election=%FF", 400, "encodage UTF-8 invalide");
            verifierStatut(client, base + "/resultats?election=PRES-01&commune=C-R&region=R-N", 400, "deux territoires");
            verifierStatut(client, base + "/candidats", 400, "candidats sans election");
            verifierStatut(client, base + "/resultats?election=PRES-99", 404, "élection inconnue");
            verifierStatut(client, base + "/resultats?election=PRES-01&territoire=XX", 404, "territoire inconnu");
            verifierStatut(client, base + "/resultats?election=PRES-01&commune=R-N", 404, "R-N n'est pas une commune");
            verifierStatut(client, base + "/candidats?election=XX", 404, "candidats élection inconnue");
            verifierStatut(client, base + "/inconnu", 404, "chemin inconnu");
            verifierStatut(client, base + "/territoires/abc", 404, "chemin non exact");
            verifierStatut(client, base + "/territoires?x=1", 200, "paramètre inconnu ignoré");

            r = envoyer(client, "POST", base + "/territoires");
            verifier("POST /territoires -> 405 avec Allow: GET",
                    r.statusCode() == 405 && r.headers().firstValue("Allow").orElse("").equals("GET") && r.body().contains("\"erreur\""));

            r = envoyer(client, "HEAD", base + "/resultats?election=PRES-01");
            verifier("HEAD /resultats -> 405 sans corps", r.statusCode() == 405 && r.body().isEmpty()
                    && r.headers().firstValue("Allow").orElse("").equals("GET"));
        } finally {
            serveur.stop(0);
        }
    }

    private static void verifierStatut(HttpClient client, String url, int statutAttendu, String description) throws Exception {
        HttpResponse<String> r = envoyer(client, "GET", url);
        boolean ok = r.statusCode() == statutAttendu;
        if (statutAttendu != 200) {
            ok = ok && estJson(r) && r.body().contains("\"erreur\"");
        }
        verifier(description + " -> " + r.statusCode() + " " + r.body(), ok);
    }

    private static HttpResponse<String> envoyer(HttpClient client, String methode, String url) throws Exception {
        HttpRequest requete = HttpRequest.newBuilder(URI.create(url))
                .method(methode, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(requete, HttpResponse.BodyHandlers.ofString());
    }

    private static boolean estJson(HttpResponse<String> reponse) {
        return reponse.headers().firstValue("Content-Type").orElse("").equals("application/json; charset=UTF-8");
    }
}
