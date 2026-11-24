package fr.epsi.elections;

// Crée en Java les territoires, élections et candidats de la section 9 du sujet
public class JeuDemonstration {

    public static ReferentielGeographique creerGeographie() throws DonneeElectoraleInvalideException {
        ReferentielGeographique geographie = new ReferentielGeographique();

        Region nord = new Region("R-N", "Région du Nord");
        Region sud = new Region("R-S", "Région du Sud");
        Departement dNord = new Departement("D-N", "Département du Nord", nord);
        Departement dSud = new Departement("D-S", "Département du Sud", sud);
        Commune rives = new Commune("C-R", "Commune des Rives", dNord);
        Commune plateau = new Commune("C-P", "Commune du Plateau", dNord);
        Commune vallee = new Commune("C-V", "Commune de la Vallée", dSud);
        CentreVote cvRives = new CentreVote("CV-R", "Centre des Rives", rives);
        CentreVote cvPlateau = new CentreVote("CV-P", "Centre du Plateau", plateau);
        CentreVote cvVallee = new CentreVote("CV-V", "Centre de la Vallée", vallee);

        // Les parents doivent être ajoutés avant leurs enfants
        geographie.ajouter(nord);
        geographie.ajouter(sud);
        geographie.ajouter(dNord);
        geographie.ajouter(dSud);
        geographie.ajouter(rives);
        geographie.ajouter(plateau);
        geographie.ajouter(vallee);
        geographie.ajouter(cvRives);
        geographie.ajouter(cvPlateau);
        geographie.ajouter(cvVallee);
        geographie.ajouter(new BureauVote("BV-01", "Bureau 01", cvRives));
        geographie.ajouter(new BureauVote("BV-02", "Bureau 02", cvRives));
        geographie.ajouter(new BureauVote("BV-03", "Bureau 03", cvPlateau));
        geographie.ajouter(new BureauVote("BV-04", "Bureau 04", cvVallee));
        return geographie;
    }

    public static ReferentielElectoral creerElections() throws DonneeElectoraleInvalideException {
        ReferentielElectoral electoral = new ReferentielElectoral();
        electoral.ajouterElection(new Election("PRES-01", "Présidentielle pédagogique", TypeElection.PRESIDENTIELLE));
        electoral.ajouterElection(new Election("LEG-01", "Législatives pédagogiques", TypeElection.LEGISLATIVE));

        electoral.inscrire("PRES-01", new CandidatPersonne("CP-01", "Diallo", "Awa"));
        electoral.inscrire("PRES-01", new CandidatPersonne("CP-02", "Fall", "Moussa"));
        electoral.inscrire("LEG-01", new CandidatListe("CL-01", "Élan citoyen", 3));
        electoral.inscrire("LEG-01", new CandidatListe("CL-02", "Union locale", 4));

        // La liste des candidats est fixée avant le chargement des résultats
        electoral.fermerInscriptions();
        return electoral;
    }

    public static ServiceResultats creerService() throws DonneeElectoraleInvalideException {
        return new ServiceResultats(creerGeographie(), creerElections());
    }
}
