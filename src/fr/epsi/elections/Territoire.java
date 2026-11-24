package fr.epsi.elections;

import java.util.List;

/**
 * Partie commune à tous les éléments géographiques : un code et un nom.
 * Attention : l'héritage sert seulement à partager ce code commun.
 * Le rattachement (un département appartient à une région) se fait par un attribut parent,
 * pas par héritage : Departement n'hérite pas de Region.
 */
public abstract class Territoire {
    private final String code; // final : le code ne peut plus changer après construction
    private final String nom;

    protected Territoire(String code, String nom) {
        if (!ValidationCode.estValide(code)) {
            throw new IllegalArgumentException("Code de territoire invalide : '" + code + "'");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du territoire " + code + " est obligatoire");
        }
        this.code = code;
        this.nom = nom;
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    // REGION, DEPARTEMENT, COMMUNE, CENTRE_VOTE ou BUREAU_VOTE
    public abstract String getType();

    // null pour une région
    public abstract Territoire getParent();

    // Tous les bureaux de vote qui sont sous ce territoire
    public abstract List<BureauVote> getBureaux();

    @Override
    public String toString() {
        String parent = "aucun";
        if (getParent() != null) {
            parent = getParent().getCode();
        }
        return getType() + "{code=" + code + ", nom=" + nom + ", parent=" + parent + "}";
    }
}
