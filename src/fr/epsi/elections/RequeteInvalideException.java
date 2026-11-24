package fr.epsi.elections;

// Paramètre HTTP absent, vide, répété ou mal encodé (réponse 400)
public class RequeteInvalideException extends Exception {

    public RequeteInvalideException(String message) {
        super(message);
    }

    // Permet de reformuler une exception sans perdre sa cause
    public RequeteInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
