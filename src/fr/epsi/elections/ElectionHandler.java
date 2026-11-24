package fr.epsi.elections;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

// Handler du TP adapté : il traite les trois chemins /territoires, /candidats et /resultats
public class ElectionHandler implements HttpHandler {
    // Noms de paramètres acceptés pour le territoire, et type de territoire attendu pour chacun
    private static final String[] PARAMETRES_TERRITOIRE = {"territoire", "region", "departement", "commune", "cv", "bv"};
    private static final String[] TYPES_TERRITOIRE = {null, "REGION", "DEPARTEMENT", "COMMUNE", "CENTRE_VOTE", "BUREAU_VOTE"};

    private final ServiceResultats service;

    public ElectionHandler(ServiceResultats service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String chemin = exchange.getRequestURI().getPath();
            if (!"/territoires".equals(chemin) && !"/candidats".equals(chemin) && !"/resultats".equals(chemin)) {
                repondre(exchange, 404, ResultatsJson.erreur("Ressource introuvable : " + chemin), "application/json");
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                repondre(exchange, 405, ResultatsJson.erreur("Méthode non autorisée"), "application/json");
                return;
            }

            if ("/territoires".equals(chemin)) {
                String json = ResultatsJson.convertirTerritoires(service.getGeographie().getTerritoires());
                repondre(exchange, 200, json, "application/json");
            } else if ("/candidats".equals(chemin)) {
                repondreCandidats(exchange);
            } else {
                repondreResultats(exchange);
            }
        } catch (RequeteInvalideException e) {
            repondre(exchange, 400, ResultatsJson.erreur(e.getMessage()), "application/json");
        } catch (RuntimeException e) {
            // Erreur imprévue : détail technique dans la console, message simple pour le client
            System.out.println("Erreur technique pendant la requête " + exchange.getRequestURI() + " : " + e);
            e.printStackTrace();
            repondre(exchange, 500, ResultatsJson.erreur("Erreur interne du serveur"), "application/json");
        } finally {
            exchange.close();
        }
    }

    private void repondreCandidats(HttpExchange exchange) throws IOException, RequeteInvalideException {
        String codeElection = recupererParametre(exchange, "election");
        if (codeElection == null) {
            throw new RequeteInvalideException("Le paramètre election est obligatoire");
        }
        Election election = service.getElectoral().trouverElection(codeElection);
        if (election == null) {
            repondre(exchange, 404, ResultatsJson.erreur("Élection inconnue : " + codeElection), "application/json");
            return;
        }
        repondre(exchange, 200, ResultatsJson.convertirCandidats(election.getCandidats()), "application/json");
    }

    private void repondreResultats(HttpExchange exchange) throws IOException, RequeteInvalideException {
        // 1) On lit et contrôle tous les paramètres (erreurs 400)
        String codeElection = recupererParametre(exchange, "election");
        if (codeElection == null) {
            throw new RequeteInvalideException("Le paramètre election est obligatoire");
        }
        String format = recupererParametre(exchange, "format");

        String codeTerritoire = null;
        String typeAttendu = null;
        for (int i = 0; i < PARAMETRES_TERRITOIRE.length; i++) {
            String valeur = recupererParametre(exchange, PARAMETRES_TERRITOIRE[i]);
            if (valeur != null) {
                if (codeTerritoire != null) {
                    throw new RequeteInvalideException("Un seul paramètre de territoire est accepté");
                }
                codeTerritoire = valeur;
                typeAttendu = TYPES_TERRITOIRE[i];
            }
        }

        IConvertisseur convertisseur;
        try {
            convertisseur = FabriqueConvertisseur.getConvertisseur(format);
        } catch (IllegalArgumentException e) {
            throw new RequeteInvalideException(e.getMessage(), e);
        }

        // 2) On cherche l'élection et le territoire (erreurs 404)
        Election election = service.getElectoral().trouverElection(codeElection);
        if (election == null) {
            repondre(exchange, 404, ResultatsJson.erreur("Élection inconnue : " + codeElection), "application/json");
            return;
        }

        Territoire territoire = null; // null = TOUS (valeur par défaut)
        boolean tous = codeTerritoire == null
                || (typeAttendu == null && ReferentielGeographique.CODE_TOUS.equals(codeTerritoire));
        if (!tous) {
            territoire = service.getGeographie().trouver(codeTerritoire);
            if (territoire == null || (typeAttendu != null && !typeAttendu.equals(territoire.getType()))) {
                repondre(exchange, 404, ResultatsJson.erreur("Territoire inconnu : " + codeTerritoire), "application/json");
                return;
            }
        }

        // 3) Calcul puis mise en forme avec la stratégie choisie
        Synthese synthese = service.calculer(election, territoire);
        ServiceAffichage affichage = new ServiceAffichage(convertisseur);
        repondre(exchange, 200, affichage.afficher(synthese), affichage.getContentType());
    }

    // Renvoie la valeur décodée du paramètre, ou null s'il est absent
    private String recupererParametre(HttpExchange exchange, String nom) throws RequeteInvalideException {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }

        String valeurTrouvee = null;
        for (String parametre : query.split("&")) {
            String[] parties = parametre.split("=", 2);
            String cle = decoder(parties[0]);
            if (nom.equals(cle)) {
                if (valeurTrouvee != null) {
                    throw new RequeteInvalideException("Le paramètre " + nom + " est répété");
                }
                valeurTrouvee = "";
                if (parties.length == 2) {
                    valeurTrouvee = decoder(parties[1]);
                }
                if (valeurTrouvee.isEmpty()) {
                    throw new RequeteInvalideException("Le paramètre " + nom + " est vide");
                }
            }
        }
        return valeurTrouvee;
    }

    private String decoder(String texte) throws RequeteInvalideException {
        String resultat;
        try {
            resultat = URLDecoder.decode(texte, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new RequeteInvalideException("Encodage invalide dans la requête : " + texte, e);
        }
        // URLDecoder remplace les octets UTF-8 invalides par ce caractère au lieu de lever une erreur
        if (resultat.contains("�")) {
            throw new RequeteInvalideException("Encodage UTF-8 invalide dans la requête : " + texte);
        }
        return resultat;
    }

    private void repondre(HttpExchange exchange, int statut, String corps, String contentType) throws IOException {
        byte[] contenu = corps.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        // Une réponse à HEAD ne doit pas envoyer de corps.
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(statut, -1);
            return;
        }

        // Longueur en octets (et non en caractères, à cause des accents)
        exchange.sendResponseHeaders(statut, contenu.length);
        try (OutputStream sortie = exchange.getResponseBody()) {
            sortie.write(contenu);
        }
    }
}
