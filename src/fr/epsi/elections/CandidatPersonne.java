package fr.epsi.elections;

public class CandidatPersonne extends Candidat {
    private final String prenom;

    public CandidatPersonne(String code, String nom, String prenom) {
        super(code, nom);
        this.prenom = prenom;
    }

    // Candidat sans prénom : on délègue à l'autre constructeur
    public CandidatPersonne(String code, String nom) {
        this(code, nom, "");
    }

    public String getPrenom() {
        return prenom;
    }

    @Override
    public String getLibelle() {
        if (prenom == null || prenom.isEmpty()) {
            return getNom();
        }
        return prenom + " " + getNom();
    }

    @Override
    public String getType() {
        return "PERSONNE";
    }
}
