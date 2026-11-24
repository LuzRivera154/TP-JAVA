package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

// Résultat du calcul pour une élection et un territoire. Ne contient que des données, pas de format.
public class Synthese {
    private final Election election;
    private final String codeTerritoire;
    private final String nomTerritoire;
    private final Couverture couverture;
    private final int bureauxTotal;
    private final int bureauxRenseignes;
    private final long totalVoix;
    private final List<LigneResultat> resultats;
    private final List<Candidat> enTete;

    public Synthese(Election election, String codeTerritoire, String nomTerritoire, Couverture couverture,
                    int bureauxTotal, int bureauxRenseignes, long totalVoix,
                    List<LigneResultat> resultats, List<Candidat> enTete) {
        this.election = election;
        this.codeTerritoire = codeTerritoire;
        this.nomTerritoire = nomTerritoire;
        this.couverture = couverture;
        this.bureauxTotal = bureauxTotal;
        this.bureauxRenseignes = bureauxRenseignes;
        this.totalVoix = totalVoix;
        this.resultats = new ArrayList<>(resultats);
        this.enTete = new ArrayList<>(enTete);
    }

    public Election getElection() {
        return election;
    }

    public String getCodeTerritoire() {
        return codeTerritoire;
    }

    public String getNomTerritoire() {
        return nomTerritoire;
    }

    public Couverture getCouverture() {
        return couverture;
    }

    public int getBureauxTotal() {
        return bureauxTotal;
    }

    public int getBureauxRenseignes() {
        return bureauxRenseignes;
    }

    public long getTotalVoix() {
        return totalVoix;
    }

    // Copies : un convertisseur ne peut pas modifier la synthèse
    public List<LigneResultat> getResultats() {
        return new ArrayList<>(resultats);
    }

    public List<Candidat> getEnTete() {
        return new ArrayList<>(enTete);
    }
}
