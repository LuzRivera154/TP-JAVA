package fr.epsi.elections;

// Singleton du TP, simplifié : uniquement le port HTTP et le dossier de données, en lecture seule
public class ConfigApp {
    private static ConfigApp INSTANCE;

    private final int port;
    private final String dossierDonnees;

    // Constructeur privé : on ne peut pas faire new ConfigApp() ailleurs
    private ConfigApp() {
        this.port = 8080;
        this.dossierDonnees = "data";
    }

    public static ConfigApp getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigApp();
        }
        return INSTANCE;
    }

    public int getPort() {
        return port;
    }

    public String getDossierDonnees() {
        return dossierDonnees;
    }
}
