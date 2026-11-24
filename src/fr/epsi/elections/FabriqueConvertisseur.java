package fr.epsi.elections;

// Fabrique simple : le choix de la classe concrète est fait uniquement ici
public final class FabriqueConvertisseur {
    public static final String FORMAT_PAR_DEFAUT = "json";

    private FabriqueConvertisseur() {
    }

    public static IConvertisseur getConvertisseur(String format) {
        if (format == null) {
            format = FORMAT_PAR_DEFAUT;
        }
        if ("json".equals(format)) {
            return new ResultatsJson();
        }
        if ("texte".equals(format)) {
            return new ResultatsTexte();
        }
        // Une valeur inconnue est refusée explicitement
        throw new IllegalArgumentException("Format inconnu : " + format + " (valeurs acceptées : texte, json)");
    }
}
