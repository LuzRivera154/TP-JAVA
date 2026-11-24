package fr.epsi.elections;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@InfoTP(auteur = "Rivera_Lujan_Luz", objectif = "Importer les résultats des bureaux et les regrouper par territoire")
public class ServiceResultats {
    private final ReferentielGeographique geographie;
    private final ReferentielElectoral electoral;
    private final FichierResultats fichier;
    // Jeu de résultats actif : il est remplacé en entier par un import valide
    private List<ResultatElection> resultats = new ArrayList<>();

    public ServiceResultats(ReferentielGeographique geographie, ReferentielElectoral electoral) {
        this.geographie = geographie;
        this.electoral = electoral;
        this.fichier = new FichierResultats(geographie, electoral);
    }

    /**
     * Remplace tous les résultats actifs par ceux du fichier.
     * Si le fichier contient une seule erreur, une exception est lancée avant le remplacement :
     * les anciens résultats restent donc actifs.
     *
     * @param chemin fichier CSV à importer
     * @return le nombre de résultats importés
     * @throws IOException si le fichier est absent ou illisible
     * @throws NombreInvalideException si un nombre de voix est invalide
     * @throws DonneeElectoraleInvalideException si une règle métier n'est pas respectée
     */
    public int importer(Path chemin) throws IOException, NombreInvalideException, DonneeElectoraleInvalideException {
        List<ResultatElection> nouveaux = fichier.lire(chemin);
        resultats = nouveaux; // on n'arrive ici que si tout est valide
        return nouveaux.size();
    }

    public void exporter(Path chemin) throws IOException {
        fichier.ecrire(chemin, resultats);
    }

    /**
     * Calcule la synthèse d'une élection sur un territoire.
     *
     * @param election   élection choisie
     * @param territoire territoire choisi, ou null pour la consultation TOUS
     * @return la synthèse (voix, pourcentages, candidats en tête, couverture)
     */
    public Synthese calculer(Election election, Territoire territoire) {
        List<BureauVote> bureaux;
        String code;
        String nom;
        if (territoire == null) {
            bureaux = geographie.getTousLesBureaux();
            code = ReferentielGeographique.CODE_TOUS;
            nom = "Tous les bureaux";
        } else {
            bureaux = territoire.getBureaux();
            code = territoire.getCode();
            nom = territoire.getNom();
        }

        List<Candidat> candidats = election.getCandidats();
        long[] voixParCandidat = new long[candidats.size()]; // même ordre que la liste des candidats
        List<BureauVote> bureauxRenseignes = new ArrayList<>();

        for (ResultatElection resultat : resultats) {
            if (resultat.getElection() == election && bureaux.contains(resultat.getBureau())) {
                int index = candidats.indexOf(resultat.getCandidat());
                voixParCandidat[index] += resultat.getNombreVoix();
                // un bureau n'est compté qu'une seule fois
                if (!bureauxRenseignes.contains(resultat.getBureau())) {
                    bureauxRenseignes.add(resultat.getBureau());
                }
            }
        }

        long total = 0;
        for (long voix : voixParCandidat) {
            total += voix;
        }

        Couverture couverture;
        if (bureauxRenseignes.isEmpty()) {
            couverture = Couverture.AUCUNE_DONNEE;
        } else if (bureauxRenseignes.size() == bureaux.size()) {
            couverture = Couverture.COMPLET;
        } else {
            couverture = Couverture.PARTIEL;
        }

        List<LigneResultat> lignes = new ArrayList<>();
        List<Candidat> enTete = new ArrayList<>();
        // Sans aucune donnée, on ne transforme pas l'absence en zéro : les listes restent vides
        if (couverture != Couverture.AUCUNE_DONNEE) {
            long maximum = 0;
            for (int i = 0; i < candidats.size(); i++) {
                double pourcentage = 0.0;
                if (total > 0) {
                    pourcentage = 100.0 * voixParCandidat[i] / total;
                }
                lignes.add(new LigneResultat(candidats.get(i), voixParCandidat[i], pourcentage));
                if (voixParCandidat[i] > maximum) {
                    maximum = voixParCandidat[i];
                }
            }
            // Si toutes les voix valent zéro, personne n'est en tête
            if (maximum > 0) {
                for (int i = 0; i < candidats.size(); i++) {
                    if (voixParCandidat[i] == maximum) {
                        enTete.add(candidats.get(i));
                    }
                }
            }
        }

        return new Synthese(election, code, nom, couverture, bureaux.size(), bureauxRenseignes.size(),
                total, lignes, enTete);
    }

    public List<ResultatElection> getResultats() {
        return new ArrayList<>(resultats);
    }

    public ReferentielGeographique getGeographie() {
        return geographie;
    }

    public ReferentielElectoral getElectoral() {
        return electoral;
    }
}
