package fr.epsi.elections;

import java.util.ArrayList;
import java.util.List;

// Contient tous les territoires et vérifie les règles de rattachement
public class ReferentielGeographique {
    public static final String CODE_TOUS = "TOUS";

    private final List<Territoire> territoires = new ArrayList<>();

    public void ajouter(Territoire territoire) throws DonneeElectoraleInvalideException {
        if (territoire == null) {
            throw new IllegalArgumentException("Territoire absent");
        }
        if (CODE_TOUS.equals(territoire.getCode())) {
            throw new DonneeElectoraleInvalideException("Le code TOUS est réservé à la consultation nationale");
        }
        if (trouver(territoire.getCode()) != null) {
            throw new DonneeElectoraleInvalideException("Code de territoire en double : " + territoire.getCode());
        }
        // Le parent doit déjà être dans le référentiel (et être le même objet)
        Territoire parent = territoire.getParent();
        if (parent != null && trouver(parent.getCode()) != parent) {
            throw new DonneeElectoraleInvalideException("Le parent " + parent.getCode() + " de "
                    + territoire.getCode() + " n'est pas dans le référentiel");
        }

        // On ajoute l'élément dans la liste des enfants de son parent
        if (territoire instanceof Departement) {
            Departement departement = (Departement) territoire;
            departement.getRegion().ajouterDepartement(departement);
        } else if (territoire instanceof Commune) {
            Commune commune = (Commune) territoire;
            commune.getDepartement().ajouterCommune(commune);
        } else if (territoire instanceof CentreVote) {
            CentreVote centre = (CentreVote) territoire;
            centre.getCommune().ajouterCentre(centre);
        } else if (territoire instanceof BureauVote) {
            BureauVote bureau = (BureauVote) territoire;
            bureau.getCentre().ajouterBureau(bureau);
        }
        territoires.add(territoire);
    }

    public Territoire trouver(String code) {
        for (Territoire territoire : territoires) {
            if (territoire.getCode().equals(code)) {
                return territoire;
            }
        }
        return null;
    }

    public BureauVote trouverBureau(String code) {
        Territoire territoire = trouver(code);
        if (territoire instanceof BureauVote) {
            return (BureauVote) territoire;
        }
        return null;
    }

    public List<Territoire> getTerritoires() {
        return new ArrayList<>(territoires);
    }

    // Pour la consultation TOUS
    public List<BureauVote> getTousLesBureaux() {
        List<BureauVote> bureaux = new ArrayList<>();
        for (Territoire territoire : territoires) {
            if (territoire instanceof BureauVote) {
                bureaux.add((BureauVote) territoire);
            }
        }
        return bureaux;
    }
}
