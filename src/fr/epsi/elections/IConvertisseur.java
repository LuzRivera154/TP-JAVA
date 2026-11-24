package fr.epsi.elections;

// Strategy : une seule méthode abstraite qui transforme une synthèse en texte
@FunctionalInterface
public interface IConvertisseur {
    String convertir(Synthese synthese);

    // Méthode par défaut (pas abstraite) : JSON sauf si la stratégie la redéfinit
    default String getContentType() {
        return "application/json";
    }
}
