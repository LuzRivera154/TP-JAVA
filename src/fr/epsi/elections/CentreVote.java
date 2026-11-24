package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

public class CentreVote extends Territoire {
    private final Commune commune;
    private final List<BureauVote> bureaux = new ArrayList<>();

    public CentreVote(String code, String nom, Commune commune) {
        super(code, nom);
        if (commune == null) {
            throw new IllegalArgumentException("Le centre de vote " + code + " doit avoir une commune");
        }
        this.commune = commune;
    }

    public Commune getCommune() {
        return commune;
    }

    @Override
    public String getType() {
        return "CENTRE_VOTE";
    }

    @Override
    public Territoire getParent() {
        return commune;
    }

    void ajouterBureau(BureauVote bureau) {
        bureaux.add(bureau);
    }

    @Override
    public List<BureauVote> getBureaux() {
        return new ArrayList<>(bureaux);
    }
}
