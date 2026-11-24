package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

// Contient toutes les élections et vérifie que les codes (élections et candidats) sont uniques
public class ReferentielElectoral {
    private final List<Election> elections = new ArrayList<>();

    public void ajouterElection(Election election) throws DonneeElectoraleInvalideException {
        if (trouverElection(election.getCode()) != null) {
            throw new DonneeElectoraleInvalideException("Code d'élection en double : " + election.getCode());
        }
        // L'élection peut déjà avoir des candidats : leurs codes doivent aussi être uniques
        for (Candidat candidat : election.getCandidats()) {
            verifierCodeCandidat(candidat);
        }
        elections.add(election);
    }

    public void inscrire(String codeElection, Candidat candidat) throws DonneeElectoraleInvalideException {
        Election election = trouverElection(codeElection);
        if (election == null) {
            throw new DonneeElectoraleInvalideException("Élection inconnue : " + codeElection);
        }
        verifierCodeCandidat(candidat);
        election.ajouterCandidat(candidat);
    }

    // Le même code ne peut pas désigner deux candidats différents
    private void verifierCodeCandidat(Candidat candidat) throws DonneeElectoraleInvalideException {
        Candidat existant = trouverCandidat(candidat.getCode());
        if (existant != null && existant != candidat) {
            throw new DonneeElectoraleInvalideException("Code de candidat déjà utilisé : " + candidat.getCode());
        }
    }

    public void fermerInscriptions() {
        for (Election election : elections) {
            election.fermerInscriptions();
        }
    }

    public Election trouverElection(String code) {
        for (Election election : elections) {
            if (election.getCode().equals(code)) {
                return election;
            }
        }
        return null;
    }

    public Candidat trouverCandidat(String code) {
        for (Election election : elections) {
            Candidat candidat = election.trouverCandidat(code);
            if (candidat != null) {
                return candidat;
            }
        }
        return null;
    }

    public List<Election> getElections() {
        return new ArrayList<>(elections);
    }
}
