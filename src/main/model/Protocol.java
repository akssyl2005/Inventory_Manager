package main.model;

public class Protocol {

    // ── Commandes produits ─────────────────────────────────────
    public static final String ADD        = "ADD";
    public static final String DELETE     = "DELETE";
    public static final String UPDATE     = "UPDATE";
    public static final String LIST       = "LIST";

    // ── Commandes mouvements ───────────────────────────────────
    public static final String ENTREE          = "ENTREE";
    public static final String SORTIE          = "SORTIE";
    public static final String LIST_MOUVEMENTS = "LIST_MOUVEMENTS";
    public static final String LIST_MOUVEMENTS_PRODUIT = "LIST_MOUVEMENTS_PRODUIT";

    // ── Commandes alertes ──────────────────────────────────────
    public static final String LIST_EXPIRES        = "LIST_EXPIRES";
    public static final String LIST_EXPIRE_BIENTOT = "LIST_EXPIRE_BIENTOT";
    public static final String LIST_STOCK_CRITIQUE  = "LIST_STOCK_CRITIQUE";
    public static final String LIST_REAPPRO         = "LIST_REAPPRO";

    // ── Commandes recherche ────────────────────────────────────
    public static final String SEARCH_DCI           = "SEARCH_DCI";
    public static final String SEARCH_FOURNISSEUR   = "SEARCH_FOURNISSEUR";
    public static final String SEARCH_LOT           = "SEARCH_LOT";

    // ── Réponses serveur ───────────────────────────────────────
    public static final String OK      = "OK";
    public static final String ERROR   = "ERROR";
    public static final String EMPTY   = "EMPTY";
    public static final String EXIT    = "EXIT";

    // ── Séparateurs ───────────────────────────────────────────
    public static final String SEP_ITEM  = ";";   // entre items
    public static final String SEP_FIELD = "|";   // entre champs si besoin
}