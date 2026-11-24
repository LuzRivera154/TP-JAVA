# ✅ Vérifications

> 🧪 Toutes les vérifications sont automatisées dans `src/fr/epsi/elections/Verifications.java`. Elles sont reproductibles avec :

```
javac -encoding UTF-8 -d out @sources.txt
java -cp out fr.epsi.elections.Verifications
```

Chaque cas affiche `[OK]` ou `[ERREUR]` et le programme termine par un bilan.

> 🏆 **Bilan observé : 70 OK ✅, 0 ERREUR ❌** (JDK 25, Windows 11)

📌 **Principe :** chaque test d'import refusé charge d'abord le jeu valide `data/resultats.csv`, tente l'import invalide dans la même exécution, puis compare la liste des triplets actifs avant et après. Les fichiers invalides sont des copies séparées dans `data/tests/` ; `data/resultats.csv` n'est jamais modifié.

---

## 🧮 1. Calculs

| Cas | Préparation | Action | Attendu | Observé |
|---|---|---|---|---|
| Référence PRES-01 / BV-01 | import de `resultats.csv` | synthèse | CP-01 120, CP-02 80, total 200, en tête CP-01 | identique ✅ |
| Référence PRES-01 / C-R | idem | synthèse | 210 / 190, total 400, CP-01 52,5 % | identique ✅ |
| Référence PRES-01 / R-N | idem | synthèse | 270 / 230, total 500, CP-01 54 % | identique ✅ |
| Référence PRES-01 / TOUS | idem | synthèse | 340 / 360, total 700, CP-02 51,43 % | identique ✅ |
| Égalité LEG-01 / BV-01 | idem | synthèse | 100 / 100, CL-01 et CL-02 en tête, 50 % chacun | identique ✅ |
| Référence LEG-01 / TOUS | idem | synthèse | 325 / 375, total 700, CL-02 53,57 % | identique ✅ |
| Centre CV-R | idem | synthèse PRES-01 | 2 bureaux, 210 / 190, total 400 | identique ✅ |
| Département D-N | idem | synthèse PRES-01 | 3 bureaux, 270 / 230, total 500 (pas de double comptage) | identique ✅ |
| Voix à zéro | import de `tests/zero_pres_bv04.csv` (PRES-01 / BV-04 à 0 et 0) | synthèse PRES-01 / BV-04 | COMPLET, total 0, 2 lignes à 0 %, aucun en tête | identique ✅ |

---

## 📥 2. Imports valides particuliers

| Cas | Préparation | Action | Attendu | Observé |
|---|---|---|---|---|
| Retrait des deux lignes PRES-01 / BV-02 | `tests/sans_pres_bv02.csv` | import puis synthèse PRES-01 / C-R | import valide ; PARTIEL, 1 bureau sur 2, total 200, CP-01 120, CP-02 80 | identique ; LEG-01 / C-R reste COMPLET ✅ |
| En-tête seul | `tests/entete_seul.csv` | import puis synthèse PRES-01 / TOUS | 0 résultat, AUCUNE_DONNEE, total 0, listes vides | identique ✅ |
| Ordre des lignes différent | `tests/ordre_different.csv` (lignes inversées) | import | mêmes triplets que la référence | identique ✅ |

---

## 🚫 3. Imports refusés

> 🛡️ **Préparation commune :** import de `data/resultats.csv` (16 résultats actifs). **Action commune :** `service.importer(...)` sur le fichier de `data/tests/` indiqué, dans la même exécution. Dans **tous** les cas, les données actives observées restent inchangées (aucun import partiel).

| Cas | Préparation du fichier | Action | Attendu | Observé |
|---|---|---|---|---|
| Une seule ligne de PRES-01 / BV-02 retirée | `pres_bv02_une_ligne.csv` (sans la ligne CP-02) | import | refus, élection et bureau précisés | DonneeElectoraleInvalideException : Groupe incomplet pour l'élection PRES-01 et le bureau BV-02 : il manque CP-02 ✅ |
| Voix négative | `voix_negative.csv` (ligne 2 : `-5`) | import | refus, pas de remplacement par zéro | DonneeElectoraleInvalideException : Ligne 2 : Le nombre de voix doit être positif ou nul (reçu : -5) ✅ |
| Voix non numérique | `voix_non_numerique.csv` (ligne 2 : `abc`) | import | refus, pas de remplacement par zéro | NombreInvalideException : Ligne 2 : nombre de voix invalide 'abc' (entier attendu, maximum 2147483647) ✅ |
| Voix hors capacité d'un int | `voix_hors_int.csv` (ligne 2 : `3000000000`) | import | refus, pas de remplacement par zéro | NombreInvalideException : Ligne 2 : nombre de voix invalide '3000000000' (entier attendu, maximum 2147483647) ✅ |
| Doublon (même nombre de voix) | `doublon.csv` (première ligne de données répétée à la fin) | import | refus avec message précis | DonneeElectoraleInvalideException : Ligne 18 : doublon pour PRES-01 / BV-01 / CP-01 ✅ |
| Candidat non inscrit | `candidat_non_inscrit.csv` (ligne 3 : CL-02 dans PRES-01) | import | refus avec message précis | DonneeElectoraleInvalideException : Ligne 3 : le candidat CL-02 n'est pas inscrit à l'élection PRES-01 ✅ |
| Bureau inconnu | `bureau_inconnu.csv` (ligne 16 : BV-99) | import | refus avec message précis | DonneeElectoraleInvalideException : Ligne 16 : bureau de vote inconnu BV-99 ✅ |
| Fichier absent | `fichier_absent.csv` (n'existe pas) | import | erreur d'accès signalée | IOException : java.nio.file.NoSuchFileException: data\tests\fichier_absent.csv ✅ |
| Fichier vide | `vide.csv` (0 octet) | import | refus | DonneeElectoraleInvalideException : Fichier vide : l'en-tête electionId;bureauId;candidatId;voix est obligatoire ✅ |
| En-tête incorrect | `entete_incorrect.csv` (`election;bureau;candidat;voix`) | import | refus | DonneeElectoraleInvalideException : Ligne 1 : en-tête incorrect, attendu : electionId;bureauId;candidatId;voix ✅ |
| Colonne manquante | `colonne_manquante.csv` (ligne 2 : 3 colonnes) | import | refus | DonneeElectoraleInvalideException : Ligne 2 : 3 colonne(s) au lieu de 4 ✅ |
| Colonne supplémentaire | `colonne_supplementaire.csv` (ligne 2 : 5 colonnes) | import | refus | DonneeElectoraleInvalideException : Ligne 2 : 5 colonne(s) au lieu de 4 ✅ |
| Ligne vide intérieure | `ligne_vide.csv` (ligne 6 vide) | import | refus | DonneeElectoraleInvalideException : Ligne 6 : ligne vide interdite ✅ |
| Dernière ligne incorrecte | `derniere_ligne_incorrecte.csv` (lignes valides puis `11O` à la ligne 17) | import | refus, aucune modification partielle | NombreInvalideException : Ligne 17 : nombre de voix invalide '11O' (entier attendu, maximum 2147483647) ✅ |

> 💡 Le même test est aussi visible dans la démonstration console (`Main`, section « Import invalide pendant l'exécution ») : refus, puis « Données actives inchangées : 16 résultats, total C-R = 400 ».

---

## 🔁 4. Export, identité, configuration, formats

| Cas | Préparation | Action | Attendu | Observé |
|---|---|---|---|---|
| Export puis réimport | import de `resultats.csv` | export vers `data/export_resultats.csv`, réimport dans un nouveau service | mêmes triplets et mêmes voix | identique ✅ |
| Export d'un jeu vide | import de `tests/entete_seul.csv` | export (fichier temporaire), réimport | 0 résultat | 0 résultat ✅ |
| Deux bureaux BV-01 | référentiel de démonstration | `new BureauVote("BV-01", centre)` à côté du BV-01 existant | `==` faux, `equals` vrai, même `hashCode` ; nom par défaut BV-01 | identique ✅ |
| Double inscription | la copie de BV-01 ci-dessus | `geographie.ajouter(copie)` | refus, le centre garde 2 bureaux | « Code de territoire en double : BV-01 », 2 bureaux ✅ |
| Parent absent | région R-X créée mais non inscrite | ajout d'un département D-X rattaché à R-X | refus | « Le parent R-X de D-X n'est pas dans le référentiel » ✅ |
| Parent nul | aucune | `new Departement("D-Y", "Sans région", null)` | refus | « Le département D-Y doit avoir une région » ✅ |
| Candidat incompatible | élection PRESIDENTIELLE de test | ajout d'une `CandidatListe` | refus | « Une présidentielle n'accepte que des personnes : CL-T refusé » ✅ |
| Inscriptions fermées | élections de démonstration (inscriptions fermées) | inscription de CP-03 à PRES-01 | refus | « Les inscriptions de l'élection PRES-01 sont fermées » ✅ |
| Code de candidat déjà utilisé | élection PRES-02 avec un autre candidat CP-01 | `electoral.ajouterElection(PRES-02)` | refus, PRES-02 non ajoutée | « Code de candidat déjà utilisé : CP-01 », PRES-02 absente ✅ |
| Code avec point-virgule | aucune | création d'un candidat `CP;01`, d'une élection `PRES;01`, d'une région `R;N` | refus des trois | « Code de candidat invalide : 'CP;01' », « Code d'élection invalide : 'PRES;01' », « Code de territoire invalide : 'R;N' » ✅ |
| Singleton | aucune | deux appels à `ConfigApp.getInstance()` | même référence, port 8080, dossier data | identique ✅ |
| Strategy | import de `resultats.csv`, synthèse PRES-01 / C-R | conversion en json puis en texte | total 400 dans les deux sorties | identique ✅ |
| Fabrique | aucune | formats `null` et `"xml"` | JSON par défaut ; refus explicite | `ResultatsJson` ; « Format inconnu : xml (valeurs acceptées : texte, json) » ✅ |
| Accents et caractères spéciaux | liste nommée `Liste "Égalité" \ Sud` | conversion du libellé en chaîne JSON | guillemets et barre oblique inverse échappés, accent conservé | `"Liste \"Égalité\" \\ Sud (2 membres)"` ✅ |

---

## 🌐 5. HTTP

> 🖥️ **Préparation commune :** import de `resultats.csv`, puis démarrage du serveur sur `127.0.0.1:8080` pendant le test (il est arrêté ensuite). **Action :** envoi de la requête indiquée avec `HttpClient`.

| Action (requête) | Attendu | Observé |
|---|---|---|
| `GET /territoires` | 200, `application/json; charset=UTF-8`, parent `null` pour R-N | identique ✅ |
| `GET /candidats?election=LEG-01` | 200, « Élan citoyen (3 membres) », type LISTE | identique ✅ |
| `GET /resultats?election=PRES-01&commune=C-R&format=json` | 200, totalVoix 400 | identique ✅ |
| `GET /resultats?format=json&commune=C-R&election=PRES-01` | même corps que la requête précédente | identique ✅ |
| `GET /resultats?election=PRES-01` | 200, TOUS en JSON | identique ✅ |
| `GET /resultats?election=PRES-01&territoire=R-N&format=texte` | 200, `text/plain; charset=UTF-8`, total 500 | identique ✅ |
| `GET /resultats?election=PRES%2D01&bv=BV%2D01` | décodage, 200, total 200 | identique ✅ |
| election absent / vide / répété | 400 + `{"erreur": ...}` | 400, « Le paramètre election est obligatoire » / « est vide » / « est répété » ✅ |
| `format=xml` / `format=` | 400 | 400, « Format inconnu : xml... » / « Le paramètre format est vide » ✅ |
| `election=%FF` (UTF-8 invalide) | 400 | 400, « Encodage UTF-8 invalide dans la requête : %FF » ✅ |
| `commune=C-R&region=R-N` | 400 (un seul territoire) | 400, « Un seul paramètre de territoire est accepté » ✅ |
| `GET /candidats` sans election | 400 | 400, « Le paramètre election est obligatoire » ✅ |
| élection inconnue, territoire inconnu, `commune=R-N`, `/candidats?election=XX` | 404 | 404, « Élection inconnue : ... » ou « Territoire inconnu : ... » ✅ |
| `/inconnu`, `/territoires/abc` | 404 | 404, « Ressource introuvable : ... » ✅ |
| `/territoires?x=1` | 200 (paramètre inconnu ignoré) | identique ✅ |
| `POST /territoires` | 405, `Allow: GET`, corps JSON `erreur` | identique ✅ |
| `HEAD /resultats?election=PRES-01` | 405, `Allow: GET`, sans corps | identique ✅ |
| Erreur 500 | 500, corps JSON `erreur` | non provoquée automatiquement ; vérifiée par lecture du `catch (RuntimeException e)` de `ElectionHandler.handle` 👀 |
