package fr.epsi.elections;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Lecture et écriture du fichier CSV des résultats
public class FichierResultats {
    public static final String EN_TETE = "electionId;bureauId;candidatId;voix";

    private final ReferentielGeographique geographie;
    private final ReferentielElectoral electoral;

    public FichierResultats(ReferentielGeographique geographie, ReferentielElectoral electoral) {
        this.geographie = geographie;
        this.electoral = electoral;
    }

    /**
     * Lit et valide entièrement un fichier de résultats.
     * Les résultats sont mis dans une liste temporaire : rien n'est publié tant que tout n'est pas valide.
     *
     * @param fichier chemin du fichier CSV (UTF-8, séparateur ;)
     * @return la liste des résultats lus, tous valides
     * @throws IOException si le fichier est absent ou illisible
     * @throws NombreInvalideException si un nombre de voix ne peut pas être converti en int
     * @throws DonneeElectoraleInvalideException si la structure ou une règle métier n'est pas respectée
     */
    public List<ResultatElection> lire(Path fichier)
            throws IOException, NombreInvalideException, DonneeElectoraleInvalideException {
        List<ResultatElection> resultats = new ArrayList<>();

        // try-with-resources : le lecteur est fermé même si une exception est lancée
        try (BufferedReader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            String ligne = lecteur.readLine();
            if (ligne == null) {
                throw new DonneeElectoraleInvalideException("Fichier vide : l'en-tête " + EN_TETE + " est obligatoire");
            }
            if (!EN_TETE.equals(ligne)) {
                throw new DonneeElectoraleInvalideException("Ligne 1 : en-tête incorrect, attendu : " + EN_TETE);
            }

            int numeroLigne = 1;
            ligne = lecteur.readLine();
            while (ligne != null) {
                numeroLigne++;
                resultats.add(lireLigne(ligne, numeroLigne, resultats));
                ligne = lecteur.readLine();
            }
        }

        // Contrôle des groupes (élection, bureau) une fois tout le fichier lu
        verifierGroupes(resultats);
        return resultats;
    }

    private ResultatElection lireLigne(String ligne, int numero, List<ResultatElection> dejaLus)
            throws NombreInvalideException, DonneeElectoraleInvalideException {
        if (ligne.isEmpty()) {
            throw new DonneeElectoraleInvalideException("Ligne " + numero + " : ligne vide interdite");
        }

        // Tableau de chaînes : une case par colonne
        String[] colonnes = ligne.split(";", -1);
        if (colonnes.length != 4) {
            throw new DonneeElectoraleInvalideException("Ligne " + numero + " : " + colonnes.length
                    + " colonne(s) au lieu de 4");
        }
        for (String colonne : colonnes) {
            if (colonne.isEmpty() || !colonne.equals(colonne.trim())) {
                throw new DonneeElectoraleInvalideException("Ligne " + numero + " : valeur vide ou entourée d'espaces");
            }
        }

        Election election = electoral.trouverElection(colonnes[0]);
        if (election == null) {
            throw new DonneeElectoraleInvalideException("Ligne " + numero + " : élection inconnue " + colonnes[0]);
        }
        BureauVote bureau = geographie.trouverBureau(colonnes[1]);
        if (bureau == null) {
            throw new DonneeElectoraleInvalideException("Ligne " + numero + " : bureau de vote inconnu " + colonnes[1]);
        }
        Candidat candidat = election.trouverCandidat(colonnes[2]);
        if (candidat == null) {
            throw new DonneeElectoraleInvalideException("Ligne " + numero + " : le candidat " + colonnes[2]
                    + " n'est pas inscrit à l'élection " + election.getCode());
        }
        int voix = convertirVoix(colonnes[3], numero);

        // Le triplet (élection, bureau, candidat) doit être unique
        for (ResultatElection dejaLu : dejaLus) {
            if (dejaLu.getElection() == election && dejaLu.getBureau() == bureau && dejaLu.getCandidat() == candidat) {
                throw new DonneeElectoraleInvalideException("Ligne " + numero + " : doublon pour "
                        + election.getCode() + " / " + bureau.getCode() + " / " + candidat.getCode());
            }
        }

        try {
            return new ResultatElection(election, bureau, candidat, voix);
        } catch (DonneeElectoraleInvalideException e) {
            // On ajoute le numéro de ligne au message, en gardant la cause
            throw new DonneeElectoraleInvalideException("Ligne " + numero + " : " + e.getMessage(), e);
        }
    }

    private int convertirVoix(String texte, int numero) throws NombreInvalideException {
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            // Pas de remplacement par zéro : on refuse la valeur
            throw new NombreInvalideException("Ligne " + numero + " : nombre de voix invalide '" + texte
                    + "' (entier attendu, maximum " + Integer.MAX_VALUE + ")", e);
        }
    }

    // Pour chaque (élection, bureau) présent, il faut une ligne par candidat inscrit
    private void verifierGroupes(List<ResultatElection> resultats) throws DonneeElectoraleInvalideException {
        for (ResultatElection resultat : resultats) {
            Election election = resultat.getElection();
            BureauVote bureau = resultat.getBureau();
            List<String> manquants = new ArrayList<>();
            for (Candidat inscrit : election.getCandidats()) {
                if (!contient(resultats, election, bureau, inscrit)) {
                    manquants.add(inscrit.getCode());
                }
            }
            if (!manquants.isEmpty()) {
                throw new DonneeElectoraleInvalideException("Groupe incomplet pour l'élection " + election.getCode()
                        + " et le bureau " + bureau.getCode() + " : il manque " + String.join(", ", manquants));
            }
        }
    }

    private boolean contient(List<ResultatElection> resultats, Election election, BureauVote bureau, Candidat candidat) {
        for (ResultatElection resultat : resultats) {
            if (resultat.getElection() == election && resultat.getBureau() == bureau && resultat.getCandidat() == candidat) {
                return true;
            }
        }
        return false;
    }

    public void ecrire(Path fichier, List<ResultatElection> resultats) throws IOException {
        if (fichier.getParent() != null) {
            Files.createDirectories(fichier.getParent());
        }
        // Le fichier cible est remplacé ; il est fermé à la fin du bloc try
        try (BufferedWriter ecrivain = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            ecrivain.write(EN_TETE);
            ecrivain.write("\n");
            for (ResultatElection resultat : resultats) {
                ecrivain.write(resultat.toString());
                ecrivain.write("\n");
            }
        }
    }
}
