package fr.epsi.elections;

public class CandidatListe extends Candidat {
    private final int nombreMembres;

    public CandidatListe(String code, String nom, int nombreMembres) {
        super(code, nom);
        if (nombreMembres < 1) {
            throw new IllegalArgumentException("Une liste doit avoir au moins un membre");
        }
        this.nombreMembres = nombreMembres;
    }

    public int getNombreMembres() {
        return nombreMembres;
    }

    @Override
    public String getLibelle() {
        return getNom() + " (" + nombreMembres + " membres)";
    }

    @Override
    public String getType() {
        return "LISTE";
    }

    @Override
    public String toString() {
        return super.toString() + " avec " + nombreMembres + " membres";
    }
}
