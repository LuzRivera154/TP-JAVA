package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

public class Election {
    private final String code;
    private final String nom;
    private final TypeElection type;
    private final List<Candidat> candidats = new ArrayList<>();
    // Une fois fermées, on ne peut plus ajouter de candidat (avant le chargement des résultats)
    private boolean inscriptionsFermees = false;

    public Election(String code, String nom, TypeElection type) {
        if (!ValidationCode.estValide(code)) {
            throw new IllegalArgumentException("Code d'élection invalide : '" + code + "'");
        }
        if (type == null) {
            throw new IllegalArgumentException("Le type de l'élection est obligatoire");
        }
        this.code = code;
        this.nom = nom;
        this.type = type;
    }

    // Visibilité package : on inscrit un candidat avec ReferentielElectoral.inscrire()
    void ajouterCandidat(Candidat candidat) throws DonneeElectoraleInvalideException {
        if (inscriptionsFermees) {
            throw new DonneeElectoraleInvalideException("Les inscriptions de l'élection " + code + " sont fermées");
        }
        if (type == TypeElection.PRESIDENTIELLE && !(candidat instanceof CandidatPersonne)) {
            throw new DonneeElectoraleInvalideException("Une présidentielle n'accepte que des personnes : " + candidat.getCode() + " refusé");
        }
        if (type == TypeElection.LEGISLATIVE && !(candidat instanceof CandidatListe)) {
            throw new DonneeElectoraleInvalideException("Une législative n'accepte que des listes : " + candidat.getCode() + " refusé");
        }
        if (trouverCandidat(candidat.getCode()) != null) {
            throw new DonneeElectoraleInvalideException("Le candidat " + candidat.getCode() + " est déjà inscrit à " + code);
        }
        candidats.add(candidat);
    }

    public void fermerInscriptions() {
        inscriptionsFermees = true;
    }

    public Candidat trouverCandidat(String codeCandidat) {
        for (Candidat candidat : candidats) {
            if (candidat.getCode().equals(codeCandidat)) {
                return candidat;
            }
        }
        return null;
    }

    // On renvoie une copie pour que personne ne modifie la liste de l'extérieur
    public List<Candidat> getCandidats() {
        return new ArrayList<>(candidats);
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    public TypeElection getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Election{code=" + code + ", nom=" + nom + ", type=" + type + ", candidats=" + candidats.size() + "}";
    }
}
