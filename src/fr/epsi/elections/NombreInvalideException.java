package fr.epsi.elections;

/**
 * Conversion impossible d'un texte en nombre (reprise du TP).
 * On garde la NumberFormatException d'origine comme cause.
 */
public class NombreInvalideException extends Exception {

    public NombreInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
