package fr.epsi.elections;

import java.util.List;

// Stratégie texte lisible (remplace ResultatsText du TP)
public class ResultatsTexte implements IConvertisseur {

    @Override
    public String convertir(Synthese synthese) {
        StringBuilder texte = new StringBuilder();
        Election election = synthese.getElection();
        texte.append("Élection : ").append(election.getCode()).append(" - ").append(election.getNom()).append("\n");
        texte.append("Territoire : ").append(synthese.getCodeTerritoire()).append(" - ").append(synthese.getNomTerritoire()).append("\n");
        texte.append("Couverture : ").append(synthese.getCouverture())
                .append(" (").append(synthese.getBureauxRenseignes()).append(" bureau(x) renseigné(s) sur ")
                .append(synthese.getBureauxTotal()).append(")\n");
        texte.append("Total des voix connues : ").append(synthese.getTotalVoix()).append("\n");

        List<LigneResultat> lignes = synthese.getResultats();
        if (lignes.isEmpty()) {
            texte.append("Aucun résultat disponible\n");
        }
        for (LigneResultat ligne : lignes) {
            texte.append("  - ").append(ligne.getCandidat().getCode()).append(" ")
                    .append(ligne.getCandidat().getLibelle()).append(" : ")
                    .append(ligne.getVoix()).append(" voix (")
                    .append(ligne.getPourcentageArrondi()).append(" %)\n");
        }

        // Un résultat partiel ne désigne pas un vainqueur définitif
        if (synthese.getCouverture() == Couverture.PARTIEL) {
            texte.append("En tête (provisoire, résultats partiels) : ");
        } else {
            texte.append("En tête : ");
        }
        List<Candidat> enTete = synthese.getEnTete();
        if (enTete.isEmpty()) {
            texte.append("aucun");
        }
        for (int i = 0; i < enTete.size(); i++) {
            if (i > 0) {
                texte.append(", ");
            }
            texte.append(enTete.get(i).getCode()).append(" ").append(enTete.get(i).getLibelle());
        }
        texte.append("\n");
        return texte.toString();
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }
}
