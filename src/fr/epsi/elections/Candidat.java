package fr.epsi.elections;

public abstract class Candidat {
    private final String code;
    private final String nom;

    protected Candidat(String code, String nom) {
        if (!ValidationCode.estValide(code)) {
            throw new IllegalArgumentException("Code de candidat invalide : '" + code + "'");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du candidat " + code + " est obligatoire");
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

    // Chaque sous-classe construit son libellé à sa façon
    public abstract String getLibelle();

    // PERSONNE ou LISTE
    public abstract String getType();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{code=" + code + ", libelle=" + getLibelle() + "}";
    }
}
