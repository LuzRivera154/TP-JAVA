# 🗳️ Mini-projet Java — Résultats électoraux par territoire

> 👤 **Auteur :** Rivera_Lujan_Luz
> 🎓 **Module :** DEVE514 — Bachelor Concepteur Développeur d'Applications, parties 1 et 2  
> ☕ **Technologie :** Java standard (JDK uniquement) — ❌ sans Spring, ❌ sans Maven, ❌ sans base de données

---

## ✨ En bref

Cette application :

- 🗺️ crée un référentiel géographique (**région → département → commune → centre → bureau**) et deux élections ;
- 📥 importe les résultats des bureaux depuis `data/resultats.csv` et 📤 les exporte vers `data/export_resultats.csv` ;
- 🧮 calcule une synthèse (voix, pourcentages, candidats en tête, couverture) pour n'importe quel territoire ;
- 🖥️ affiche ces synthèses dans la console et 🌐 les sert en HTTP (texte ou JSON).

> ℹ️ Toutes les données sont **fictives**.

---

## 🚀 1. Installation et commandes

> ⚠️ **Prérequis :** un JDK 11 ou plus récent (testé avec le JDK 25).  
> 📂 Toutes les commandes se lancent **depuis le dossier du projet** (celui qui contient `src/` et `data/`), car les chemins `data/...` sont relatifs.

```
# Compilation (sources.txt contient la liste des fichiers .java)
javac -encoding UTF-8 -d out @sources.txt

# Sous PowerShell, il faut mettre des guillemets autour de @sources.txt
javac -encoding UTF-8 -d out "@sources.txt"

# Démonstration console + serveur HTTP sur 127.0.0.1:8080 (Ctrl+C pour arrêter)
java -cp out fr.epsi.elections.Main

# Démonstration console seulement
java -cp out fr.epsi.elections.Main --sans-serveur

# Vérifications automatiques de la section 10 (affiche OK / ERREUR et un bilan)
java -cp out fr.epsi.elections.Verifications
```

> 💡 **Astuce :** si les accents s'affichent mal dans la console Windows, ajouter `-Dstdout.encoding=UTF-8` après `java` (ou lancer `chcp 65001` avant).

🔄 Pour régénérer `sources.txt` après l'ajout d'une classe :
- Git Bash / Linux / macOS : `find src -name "*.java" > sources.txt`
- PowerShell : `Get-ChildItem -Recurse src -Filter *.java | Resolve-Path -Relative > sources.txt`

### 🌐 Exemples de requêtes HTTP

```
http://127.0.0.1:8080/territoires
http://127.0.0.1:8080/candidats?election=PRES-01
http://127.0.0.1:8080/resultats?election=PRES-01
http://127.0.0.1:8080/resultats?election=PRES-01&commune=C-R&format=json
http://127.0.0.1:8080/resultats?election=LEG-01&bv=BV-01&format=texte
http://127.0.0.1:8080/resultats?election=PRES-01&territoire=R-N
```

🔧 **Paramètres de `/resultats` :**

| Paramètre | Obligatoire ? | Valeurs |
|---|---|---|
| `election` | ✅ oui | `PRES-01`, `LEG-01` |
| territoire | ➖ non (défaut : `TOUS`) | un seul paramètre parmi `territoire`, `region`, `departement`, `commune`, `cv`, `bv` |
| `format` | ➖ non (défaut : `json`) | `json` ou `texte` |

- Avec `territoire`, n'importe quel code est accepté (y compris `TOUS`).
- Avec un nom de niveau, le code doit être de ce niveau (`commune=R-N` donne 404).

🚦 **Codes de statut :**

| Statut | Quand ? |
|---|---|
| 🟢 **200** | consultation valide, y compris sans données |
| 🟠 **400** | paramètre absent, vide, répété, mal encodé, format inconnu, deux territoires |
| 🔴 **404** | chemin, élection ou territoire inconnu |
| 🟣 **405** | méthode autre que GET (y compris HEAD, sans corps), avec l'en-tête `Allow: GET` |
| ⚫ **500** | erreur imprévue pendant le traitement |

> 📦 Les erreurs sont des objets JSON : `{"erreur": "..."}`.

---

## 🧱 2. Organisation du code

Toutes les classes sont dans le paquet `fr.epsi.elections` (`src/fr/epsi/elections/`), comme dans le TP de cours dont le projet est repris.

| Rôle | Classes |
|---|---|
| 👥 Candidats | `Candidat` (abstraite), `CandidatPersonne`, `CandidatListe` |
| 🗳️ Élections | `TypeElection`, `Election`, `ReferentielElectoral` |
| 🗺️ Géographie | `Territoire` (abstraite), `Region`, `Departement`, `Commune`, `CentreVote`, `BureauVote`, `ReferentielGeographique` |
| 🧮 Résultats et calcul | `ResultatElection`, `ServiceResultats`, `Synthese`, `LigneResultat`, `Couverture` |
| 📄 Fichiers | `FichierResultats` |
| 🎨 Formats (Strategy + fabrique) | `IConvertisseur`, `ResultatsJson`, `ResultatsTexte`, `FabriqueConvertisseur`, `ServiceAffichage` |
| ⚙️ Configuration (Singleton) | `ConfigApp` |
| 🌐 HTTP | `ElectionHandler` (serveur créé dans `Main`) |
| 🚨 Exceptions | `DonneeElectoraleInvalideException`, `NombreInvalideException`, `RequeteInvalideException` |
| 🔤 Contrôle des codes | `ValidationCode` |
| 🏷️ Annotation | `InfoTP` |
| ▶️ Démarrage et tests | `Main`, `JeuDemonstration`, `Verifications` |

♻️ Classes du TP reprises et adaptées : `Candidat`, `CandidatPersonne`, `CandidatListe`, `Election`, `ResultatElection`, `IConvertisseur`, `FabriqueConvertisseur`, `ResultatsJson`, `ElectionHandler`, `ConfigApp`, `NombreInvalideException`, `Main`. `ResultatsText` est devenue `ResultatsTexte`. `DatabaseManager`, `ResultatsHandler` et `CandidatureInvalideException` ont été retirées : il n'y a plus de base de données, un seul handler sert les trois chemins, et la dernière exception n'était pas utilisée.

---

## 🧠 3. Choix de conception

### 🌳 3.1 Classes, héritage et composition

```
Candidat (abstraite)             Territoire (abstraite : code, nom)
 ├─ CandidatPersonne              ├─ Region      ──contient──> Departement
 └─ CandidatListe                 ├─ Departement ──contient──> Commune
                                  ├─ Commune     ──contient──> CentreVote
Election ──contient──> Candidat   ├─ CentreVote  ──contient──> BureauVote
ResultatElection ──> Election, BureauVote, Candidat
```

- **Héritage** (« est un ») : une `CandidatPersonne` *est un* `Candidat` ; une `Region` *est un* `Territoire`. `Territoire` ne sert qu'à partager le code, le nom et trois méthodes abstraites (`getType()`, `getParent()`, `getBureaux()`).
- **Composition / association** (« appartient à ») : un département *appartient à* une région. `Departement` a donc un attribut `final Region region`, et la région a une `List<Departement>`. `Departement` n'hérite **pas** de `Region` : un département n'est pas une région.
- Un bureau ne peut pas appartenir à deux centres : son centre est un attribut `final` fixé dans le constructeur (`BureauVote.java`). Les listes d'enfants ne sont remplies que par `ReferentielGeographique.ajouter()`, qui vérifie d'abord que le code n'existe pas déjà (`TOUS` est réservé) et que le parent est bien l'objet déjà inscrit. C'est pourquoi les méthodes `ajouterDepartement`, `ajouterCommune`… ont la visibilité *package* (sans `public`).
- Les attributs sont `private`, et les codes sont `final` sans setter : un code ne change plus après la construction. `ValidationCode.estValide()` refuse les codes vides, entourés d'espaces ou contenant `;`, un guillemet ou un saut de ligne, pour que l'export CSV reste toujours relisible. Les getters de listes renvoient des copies (`new ArrayList<>(...)`), pour qu'on ne puisse pas contourner les règles de l'extérieur.
- `getBureaux()` est redéfinie à chaque niveau : un bureau renvoie lui-même, un centre ses bureaux, une commune les bureaux de ses centres, etc. Chaque bureau n'est donc présent qu'une fois, et on ne code jamais les appartenances à partir des codes de l'exemple.

### 🎭 3.2 Polymorphisme, surcharge et redéfinition

- **Appel polymorphe** : dans `Main.demonstration()` et `ResultatsJson.convertirCandidats()`, on parcourt une `List<Candidat>` et on appelle `candidat.getLibelle()`. Le type déclaré est `Candidat`, mais la JVM exécute la version de la classe réelle : « Awa Diallo » pour une personne, « Élan citoyen (3 membres) » pour une liste. Même chose pour `territoire.getBureaux()` dans `ServiceResultats.calculer()`.
- **Surcharge de constructeur** : même nom, paramètres différents, et le choix se fait à la compilation. `BureauVote(code, nom, centre)` et `BureauVote(code, centre)`, ce dernier déléguant avec `this(code, code, centre)` pour utiliser le code comme nom. Pareil pour `CandidatPersonne(code, nom)` → `this(code, nom, "")`.
- **Redéfinition** (`@Override`) : même signature que dans la classe parente, et le choix se fait à l'exécution. Exemples : `getLibelle()`, `getType()`, `getBureaux()`, `toString()`, `equals()`/`hashCode()` dans `BureauVote`, `getContentType()` dans `ResultatsTexte`.
- `super(...)` : les constructeurs de `CandidatPersonne` et `CandidatListe` appellent `super(code, nom)`. `CandidatListe.toString()` réutilise `super.toString()`.

### 🧩 3.3 Design patterns

- 🎯 **Strategy** : `IConvertisseur` (annotée `@FunctionalInterface`) n'a qu'une méthode abstraite, `String convertir(Synthese)`. `getContentType()` est une méthode `default`, donc pas abstraite. `ResultatsJson` et `ResultatsTexte` sont deux stratégies. `ServiceAffichage` reçoit la stratégie dans son constructeur et ne sait pas quel format il produit. Le calcul (`ServiceResultats.calculer`) ne dépend d'aucun format : on change la sortie sans toucher au calcul (voir la démonstration « Même synthèse au format JSON »). Les stratégies ne modifient rien : `Synthese` ne renvoie que des copies de ses listes et n'a pas de setter.
- 🏭 **Fabrique simple** : `FabriqueConvertisseur.getConvertisseur(format)` est le seul endroit qui fait `new ResultatsJson()` ou `new ResultatsTexte()`. `null` donne le format par défaut (json). Une valeur inconnue lance une `IllegalArgumentException`, que le handler transforme en 400.
- 🔒 **Singleton** : `ConfigApp` a un constructeur `private`, une instance `static` et `getInstance()`. Elle expose seulement, en lecture seule, le port 8080 et le dossier `data`. Les données métier restent dans les référentiels et le service.
  - ⚠️ *Limite du Singleton* : c'est un état global caché. Une classe qui appelle `ConfigApp.getInstance()` dépend de cette configuration sans que cela se voie dans ses paramètres, et on ne peut pas facilement utiliser une autre configuration (par exemple un autre port pour des tests en parallèle). De plus, la création paresseuse (`if (INSTANCE == null)`) n'est pas sûre si plusieurs threads l'appellent en même temps la première fois. Ici, elle est appelée au démarrage dans `main`.
- 🔌 **Adapter** (non implémenté) : utile si l'on devait lire des résultats fournis par un autre système avec un format différent (par exemple un fichier XML ou une API qui renvoie « nb_voix » et « id_bv »). Une classe adaptateur convertirait ces objets étrangers en `ResultatElection`, sans modifier le reste du programme.
- 🎁 **Decorator** (non implémenté) : utile pour ajouter un comportement à un `IConvertisseur` sans le modifier. Par exemple, un `ConvertisseurAvecHorodatage` qui entoure n'importe quel convertisseur et ajoute la date de génération, ou un décorateur qui écrit chaque réponse produite dans un journal.

### ⚖️ 3.4 Types et comparaisons

- **`==` et `equals()`** : `==` compare les références (est-ce le même objet en mémoire ?), `equals()` compare le sens métier. Deux `new BureauVote("BV-01", ...)` sont deux objets différents (`==` faux) mais représentent le même bureau (`equals` vrai, car il compare le code). `hashCode()` est redéfini avec le code pour rester cohérent : deux objets égaux doivent avoir le même hashCode, sinon `HashMap` et `HashSet` se comportent mal. Le référentiel refuse quand même la deuxième inscription (« code en double »). Dans le calcul, `bureaux.contains(...)` utilise `equals()`. Pour les élections et les candidats, on compare avec `==`, car on travaille toujours avec les objets uniques du référentiel.
- **`int` et `Integer`** : `int` est un type primitif (une valeur, jamais `null`), `Integer` est un objet qui l'enveloppe (il peut être `null` et sert dans les collections, par exemple `List<Integer>`). Les voix sont des `int`, car une voix est un entier et ne doit jamais être `null` : une absence de résultat est représentée par l'absence de ligne, pas par `null` ni par 0.
- **Choix des types** : voix d'un bureau en `int` (contrat du fichier : un nombre trop grand pour un int est refusé) ; totaux en `long`, car la somme de nombreux bureaux pourrait dépasser la capacité d'un `int` ; pourcentages en `double` (calcul décimal `100.0 * voix / total`), arrondis à deux décimales seulement à l'affichage (`LigneResultat.getPourcentageArrondi()`).
- **Tableau et liste** : un tableau a une taille fixe, une liste (`ArrayList`) grandit à la demande. On utilise un `String[]` pour les colonnes d'une ligne CSV (toujours 4 cases, `ligne.split(";", -1)`) et un `long[]` pour cumuler les voix par candidat (taille connue : le nombre d'inscrits). Les collections qui évoluent (territoires, candidats, résultats) sont des `List`/`ArrayList`.

### 🐞 3.5 Parcours d'une ligne CSV invalide

Exemple : `PRES-01;BV-01;CP-01;abc` à la ligne 2.

1. 🔍 **Détection** : `FichierResultats.lire()` lit le fichier ligne par ligne avec un `BufferedReader`. `lireLigne()` découpe la ligne, vérifie les colonnes et les références, puis `convertirVoix()` appelle `Integer.parseInt("abc")`, qui lance une `NumberFormatException`.
2. 💥 **Exception** : `convertirVoix()` l'attrape (`catch`) et lance (`throw`) une `NombreInvalideException("Ligne 2 : nombre de voix invalide 'abc' ...", e)`. Le message est plus clair et la cause d'origine est conservée. On ne remplace jamais la valeur par zéro.
3. ⬆️ **Propagation** : `convertirVoix`, `lireLigne`, `lire` et `ServiceResultats.importer` déclarent `throws NombreInvalideException`. L'exception remonte et interrompt la lecture. Le `try-with-resources` ferme automatiquement le `BufferedReader` pendant cette remontée.
4. 🛠️ **Traitement** : l'appelant (`Main` ou `Verifications`) attrape séparément `IOException` (fichier absent ou illisible), `NombreInvalideException` (conversion impossible) et `DonneeElectoraleInvalideException` (règle métier : doublon, candidat non inscrit, voix négative, groupe incomplet…), et affiche un message.
5. 🛡️ **État final** : dans `importer()`, la ligne `resultats = nouveaux;` n'est jamais atteinte. L'ancienne liste reste donc active, sans aucune modification partielle. Au démarrage, un échec du premier import arrête le programme avant le serveur.

- `throw` lance une exception ; `throws` (dans la signature) annonce qu'une méthode peut la laisser remonter ; `catch` l'attrape et la traite ; `finally` s'exécute toujours, qu'il y ait eu une exception ou non. Dans `ElectionHandler.handle()`, `finally { exchange.close(); }` garantit que l'échange HTTP est fermé dans tous les cas.
- Le **try-with-resources** (`try (BufferedReader lecteur = ...)`) ferme la ressource automatiquement à la fin du bloc, même en cas d'exception. C'est l'équivalent d'un `finally { lecteur.close(); }`, en plus court et sans oubli possible. Pour l'export, `Main` n'annonce « Export terminé » qu'après la sortie du bloc, c'est-à-dire après la fermeture normale du fichier.

### 🏷️ 3.6 Annotations, `final` et `finally`

- `@Override` est placé sur toutes les méthodes qui redéfinissent ou implémentent un contrat : le compilateur signale une erreur si la signature ne correspond à rien.
- `@FunctionalInterface` sur `IConvertisseur` : le compilateur vérifie qu'il n'y a qu'une méthode abstraite.
- `@InfoTP` (`InfoTP.java`) : `auteur` est obligatoire (pas de valeur par défaut), `objectif` est optionnel (`default ""`). `@Target(ElementType.TYPE)` la limite aux classes, et `@Retention(RetentionPolicy.RUNTIME)` la conserve à l'exécution. `ServiceResultats` est annotée, et `Main.afficherInfoTP()` la lit avec `getAnnotation(InfoTP.class)`, en gérant le cas `null` (démontré sur `Main`, qui n'est pas annotée).
- Une annotation personnalisée **n'exécute rien seule** : c'est une étiquette (des métadonnées). Il faut un code qui la lise, comme ici `afficherInfoTP()` par réflexion, ou un outil comme le compilateur ou un framework.
- Avec une **rétention SOURCE**, l'annotation serait supprimée par `javac` : elle n'existerait plus dans le `.class`, et `getAnnotation` renverrait toujours `null`. Le programme afficherait donc « pas d'annotation ». SOURCE convient aux annotations destinées uniquement au compilateur, comme `@Override`.
- `@Deprecated` signale aux utilisateurs d'une méthode ou d'une classe qu'elle est obsolète (le compilateur avertit ceux qui l'utilisent). `@SuppressWarnings("...")` demande au compilateur de ne pas afficher certains avertissements dans le code annoté. La première informe les autres, la seconde fait taire le compilateur. Aucune des deux n'était utile ici.
- `final` est un mot-clé de déclaration : une variable ou un attribut `final` ne peut plus être réaffecté (ici, les codes), une méthode `final` ne peut pas être redéfinie, une classe `final` ne peut pas être héritée (`FabriqueConvertisseur`). `finally` est un bloc lié à `try` qui s'exécute toujours. Ils n'ont rien à voir malgré leur nom.

### 🛣️ 3.7 Trajet d'une requête HTTP

`GET /resultats?election=PRES-01&commune=C-R&format=texte` :

1. `Main` crée un `HttpServer` sur `127.0.0.1:8080` et enregistre `ElectionHandler` sur le contexte `/`. Les données sont chargées **avant** `serveur.start()`.
2. Le serveur appelle `ElectionHandler.handle(exchange)`. Le chemin exact est vérifié (sinon 404), puis la méthode (sinon 405 + `Allow: GET`).
3. `recupererParametre()` lit `getRawQuery()`, découpe sur `&` et `=`, et décode avec `URLDecoder` en UTF-8. On cherche le paramètre par son nom, donc l'ordre n'a pas d'importance. Un paramètre absent, vide, répété ou mal encodé lance une `RequeteInvalideException` (400).
4. La fabrique choisit le convertisseur (format inconnu → 400). Puis on cherche l'élection et le territoire dans les référentiels (inconnus → 404).
5. `ServiceResultats.calculer(election, territoire)` produit la `Synthese` à partir des résultats des bureaux.
6. `ServiceAffichage` applique la stratégie, et `repondre()` encode le texte en UTF-8, envoie le `Content-Type` et la longueur en **octets**, écrit le corps (pas de corps pour HEAD) puis ferme le flux. `finally` ferme l'échange. Une `RuntimeException` imprévue est écrite dans la console et renvoie 500 avec un message simple.

### ☕ 3.8 javac, java, JDK, bytecode et JVM

- Le **JDK** (Java Development Kit) contient les outils de développement (`javac`, `java`…) et la bibliothèque standard. On n'utilise que celle-ci, y compris `com.sun.net.httpserver` pour le serveur.
- `javac` compile les fichiers `.java` en fichiers `.class` qui contiennent du **bytecode**, un code intermédiaire indépendant du système d'exploitation. L'option `-d out` les range dans le dossier `out/`.
- `java -cp out fr.epsi.elections.Main` démarre la **JVM** (machine virtuelle Java), qui charge les `.class` depuis `out/`, vérifie et exécute le bytecode (en l'interprétant ou en le compilant à la volée), en commençant par la méthode `main`. C'est pour cela que le même `.class` fonctionne sous Windows, Linux ou macOS.

---

## 🚧 4. Limites et choix assumés

- Le CSV est volontairement simple : pas de guillemets ni d'échappement, en-tête exact, et un fichier commençant par un BOM UTF-8 est refusé (en-tête incorrect).
- Les recherches se font par parcours de listes (`for`) : c'est suffisant pour ce jeu de données, mais une `HashMap` serait plus rapide sur des milliers de bureaux.
- Les territoires et les élections sont créés en Java (`JeuDemonstration`) ; seuls les résultats sont importés et exportés.
- Le cas 500 n'est pas testé automatiquement : il n'arrive que sur un bug imprévu. Il est traité par le `catch (RuntimeException e)` de `ElectionHandler`.
- Le candidat en tête d'un résultat `PARTIEL` est affiché comme « provisoire » en texte. En JSON, le champ `couverture` l'indique.

---

## 📚 5. Ressources

- 📘 Le TP de cours DEVE514 (classes reprises, listées en section 2).
- 📖 La documentation officielle du JDK (`java.nio.file.Files`, `com.sun.net.httpserver`, `java.net.URLDecoder`, `java.net.http.HttpClient` pour les vérifications).

---

