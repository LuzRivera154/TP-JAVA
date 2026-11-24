package fr.epsi.elections;

// Service qui reçoit la stratégie dans son constructeur : il ne sait pas quel format il produit
public class ServiceAffichage {
    private final IConvertisseur convertisseur;

    public ServiceAffichage(IConvertisseur convertisseur) {
        if (convertisseur == null) {
            throw new IllegalArgumentException("Le convertisseur est obligatoire");
        }
        this.convertisseur = convertisseur;
    }

    public String afficher(Synthese synthese) {
        return convertisseur.convertir(synthese);
    }

    public String getContentType() {
        return convertisseur.getContentType();
    }
}
