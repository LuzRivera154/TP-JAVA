package fr.epsi.elections;

// Une ligne de la synthèse : un candidat, ses voix et son pourcentage
public class LigneResultat {
    private final Candidat candidat;
    private final long voix;
    private final double pourcentage;

    public LigneResultat(Candidat candidat, long voix, double pourcentage) {
        this.candidat = candidat;
        this.voix = voix;
        this.pourcentage = pourcentage;
    }

    public Candidat getCandidat() {
        return candidat;
    }

    public long getVoix() {
        return voix;
    }

    public double getPourcentage() {
        return pourcentage;
    }

    // Pourcentage arrondi à deux décimales pour l'affichage (52.5, 51.43...)
    public double getPourcentageArrondi() {
        return Math.round(pourcentage * 100) / 100.0;
    }
}
