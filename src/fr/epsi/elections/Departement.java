package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

public class Departement extends Territoire {
    private final Region region;
    private final List<Commune> communes = new ArrayList<>();

    public Departement(String code, String nom, Region region) {
        super(code, nom);
        if (region == null) {
            throw new IllegalArgumentException("Le département " + code + " doit avoir une région");
        }
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String getType() {
        return "DEPARTEMENT";
    }

    @Override
    public Territoire getParent() {
        return region;
    }

    public List<Commune> getCommunes() {
        return new ArrayList<>(communes);
    }

    void ajouterCommune(Commune commune) {
        communes.add(commune);
    }

    @Override
    public List<BureauVote> getBureaux() {
        List<BureauVote> bureaux = new ArrayList<>();
        for (Commune commune : communes) {
            bureaux.addAll(commune.getBureaux());
        }
        return bureaux;
    }
}
