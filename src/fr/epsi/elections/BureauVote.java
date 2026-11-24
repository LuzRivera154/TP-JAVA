package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

public class BureauVote extends Territoire {
    // final : un bureau n'a qu'un seul centre, choisi à la construction
    private final CentreVote centre;

    public BureauVote(String code, String nom, CentreVote centre) {
        super(code, nom);
        if (centre == null) {
            throw new IllegalArgumentException("Le bureau " + code + " doit avoir un centre de vote");
        }
        this.centre = centre;
    }

    // Sans nom explicite, le bureau prend son code comme nom
    public BureauVote(String code, CentreVote centre) {
        this(code, code, centre);
    }

    public CentreVote getCentre() {
        return centre;
    }

    @Override
    public String getType() {
        return "BUREAU_VOTE";
    }

    @Override
    public Territoire getParent() {
        return centre;
    }

    @Override
    public List<BureauVote> getBureaux() {
        List<BureauVote> bureaux = new ArrayList<>();
        bureaux.add(this);
        return bureaux;
    }

    // Deux bureaux sont égaux s'ils ont le même code
    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (autre == null || getClass() != autre.getClass()) {
            return false;
        }
        BureauVote autreBureau = (BureauVote) autre;
        return getCode().equals(autreBureau.getCode());
    }

    // Cohérent avec equals : même code => même hashCode
    @Override
    public int hashCode() {
        return getCode().hashCode();
    }
}
