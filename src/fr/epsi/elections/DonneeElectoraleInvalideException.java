package fr.epsi.elections;

/**
 * Exception métier : une règle du projet n'est pas respectée
 * (code en double, candidat non inscrit, voix négatives, groupe incomplet...).
 * C'est une exception vérifiée : on est obligé de la déclarer avec throws ou de la traiter.
 */
public class DonneeElectoraleInvalideException extends Exception {

    public DonneeElectoraleInvalideException(String message) {
        super(message);
    }

    // Permet de reformuler une exception sans perdre sa cause
    public DonneeElectoraleInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
