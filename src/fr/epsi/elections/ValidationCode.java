package fr.epsi.elections;

// Règle commune aux codes (territoires, élections, candidats), voir le contrat du fichier CSV
final class ValidationCode {

    private ValidationCode() {
    }

    // Un code contenant un de ces caractères rendrait l'export CSV impossible à relire
    static boolean estValide(String code) {
        return code != null && !code.isBlank() && code.equals(code.trim())
                && code.indexOf(';') < 0 && code.indexOf('"') < 0
                && code.indexOf('\n') < 0 && code.indexOf('\r') < 0;
    }
}
