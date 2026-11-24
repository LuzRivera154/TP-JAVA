package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

public class Commune extends Territoire {
    private final Departement departement;
    private final List<CentreVote> centres = new ArrayList<>();

    public Commune(String code, String nom, Departement departement) {
        super(code, nom);
        if (departement == null) {
            throw new IllegalArgumentException("La commune " + code + " doit avoir un département");
        }
        this.departement = departement;
    }

    public Departement getDepartement() {
        return departement;
    }

    @Override
    public String getType() {
        return "COMMUNE";
    }

    @Override
    public Territoire getParent() {
        return departement;
    }

    public List<CentreVote> getCentres() {
        return new ArrayList<>(centres);
    }

    void ajouterCentre(CentreVote centre) {
        centres.add(centre);
    }

    @Override
    public List<BureauVote> getBureaux() {
        List<BureauVote> bureaux = new ArrayList<>();
        for (CentreVote centre : centres) {
            bureaux.addAll(centre.getBureaux());
        }
        return bureaux;
    }
}
