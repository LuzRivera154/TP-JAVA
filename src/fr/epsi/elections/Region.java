package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

public class Region extends Territoire {
    private final List<Departement> departements = new ArrayList<>();

    public Region(String code, String nom) {
        super(code, nom);
    }

    @Override
    public String getType() {
        return "REGION";
    }

    @Override
    public Territoire getParent() {
        return null;
    }

    public List<Departement> getDepartements() {
        return new ArrayList<>(departements);
    }

    // Visibilité package : seul le référentiel rattache les enfants
    void ajouterDepartement(Departement departement) {
        departements.add(departement);
    }

    @Override
    public List<BureauVote> getBureaux() {
        List<BureauVote> bureaux = new ArrayList<>();
        for (Departement departement : departements) {
            bureaux.addAll(departement.getBureaux());
        }
        return bureaux;
    }
}
