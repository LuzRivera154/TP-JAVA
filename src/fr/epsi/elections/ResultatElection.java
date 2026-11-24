package fr.epsi.elections;

// Classe du TP adaptée : un résultat connaît maintenant son élection et son bureau
public class ResultatElection {
    private final Election election;
    private final BureauVote bureau;
    private final Candidat candidat;
    private final int nombreVoix;

    public ResultatElection(Election election, BureauVote bureau, Candidat candidat, int nombreVoix)
            throws DonneeElectoraleInvalideException {
        if (election == null || bureau == null || candidat == null) {
            throw new IllegalArgumentException("L'élection, le bureau et le candidat sont obligatoires");
        }
        if (nombreVoix < 0) {
            throw new DonneeElectoraleInvalideException("Le nombre de voix doit être positif ou nul (reçu : " + nombreVoix + ")");
        }
        if (election.trouverCandidat(candidat.getCode()) != candidat) {
            throw new DonneeElectoraleInvalideException("Le candidat " + candidat.getCode()
                    + " n'est pas inscrit à l'élection " + election.getCode());
        }
        this.election = election;
        this.bureau = bureau;
        this.candidat = candidat;
        this.nombreVoix = nombreVoix;
    }

    public Election getElection() {
        return election;
    }

    public BureauVote getBureau() {
        return bureau;
    }

    public Candidat getCandidat() {
        return candidat;
    }

    public int getNombreVoix() {
        return nombreVoix;
    }

    @Override
    public String toString() {
        return election.getCode() + ";" + bureau.getCode() + ";" + candidat.getCode() + ";" + nombreVoix;
    }
}
