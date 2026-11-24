package fr.epsi.elections;

import java.util.List;

// Stratégie JSON (reprise du convertisseur du TP)
public class ResultatsJson implements IConvertisseur {

    @Override
    public String convertir(Synthese synthese) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"election\": ").append(chaine(synthese.getElection().getCode())).append(",\n");
        json.append("  \"territoire\": ").append(chaine(synthese.getCodeTerritoire())).append(",\n");
        json.append("  \"nomTerritoire\": ").append(chaine(synthese.getNomTerritoire())).append(",\n");
        json.append("  \"couverture\": ").append(chaine(synthese.getCouverture().name())).append(",\n");
        json.append("  \"bureauxTotal\": ").append(synthese.getBureauxTotal()).append(",\n");
        json.append("  \"bureauxRenseignes\": ").append(synthese.getBureauxRenseignes()).append(",\n");
        json.append("  \"totalVoix\": ").append(synthese.getTotalVoix()).append(",\n");

        json.append("  \"resultats\": [");
        List<LigneResultat> lignes = synthese.getResultats();
        for (int i = 0; i < lignes.size(); i++) {
            LigneResultat ligne = lignes.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("\n    {\"candidat\": ").append(chaine(ligne.getCandidat().getCode()))
                    .append(", \"libelle\": ").append(chaine(ligne.getCandidat().getLibelle()))
                    .append(", \"voix\": ").append(ligne.getVoix())
                    .append(", \"pourcentage\": ").append(ligne.getPourcentageArrondi())
                    .append("}");
        }
        if (!lignes.isEmpty()) {
            json.append("\n  ");
        }
        json.append("],\n");

        json.append("  \"enTete\": [");
        List<Candidat> enTete = synthese.getEnTete();
        for (int i = 0; i < enTete.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append(chaine(enTete.get(i).getCode()));
        }
        json.append("]\n");
        json.append("}");
        return json.toString();
    }

    // GET /territoires
    public static String convertirTerritoires(List<Territoire> territoires) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < territoires.size(); i++) {
            Territoire territoire = territoires.get(i);
            if (i > 0) {
                json.append(",");
            }
            String parent = "null";
            if (territoire.getParent() != null) {
                parent = chaine(territoire.getParent().getCode());
            }
            json.append("\n  {\"code\": ").append(chaine(territoire.getCode()))
                    .append(", \"nom\": ").append(chaine(territoire.getNom()))
                    .append(", \"type\": ").append(chaine(territoire.getType()))
                    .append(", \"parent\": ").append(parent)
                    .append("}");
        }
        return json.append("\n]").toString();
    }

    // GET /candidats
    public static String convertirCandidats(List<Candidat> candidats) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < candidats.size(); i++) {
            Candidat candidat = candidats.get(i);
            if (i > 0) {
                json.append(",");
            }
            // getLibelle() : appel polymorphe, la bonne méthode est choisie à l'exécution
            json.append("\n  {\"code\": ").append(chaine(candidat.getCode()))
                    .append(", \"libelle\": ").append(chaine(candidat.getLibelle()))
                    .append(", \"type\": ").append(chaine(candidat.getType()))
                    .append("}");
        }
        return json.append("\n]").toString();
    }

    public static String erreur(String message) {
        return "{\"erreur\": " + chaine(message) + "}";
    }

    // Échappe les caractères spéciaux pour produire une chaîne JSON valide
    static String chaine(String texte) {
        StringBuilder json = new StringBuilder("\"");
        for (int i = 0; i < texte.length(); i++) {
            char caractere = texte.charAt(i);
            switch (caractere) {
                case '"': json.append("\\\""); break;
                case '\\': json.append("\\\\"); break;
                case '\b': json.append("\\b"); break;
                case '\f': json.append("\\f"); break;
                case '\n': json.append("\\n"); break;
                case '\r': json.append("\\r"); break;
                case '\t': json.append("\\t"); break;
                default:
                    if (caractere < 0x20) {
                        json.append(String.format("\\u%04x", (int) caractere));
                    } else {
                        json.append(caractere);
                    }
            }
        }
        return json.append('"').toString();
    }
}
