package main.client;

import main.model.Mouvement;
import main.model.Produit;
import main.rmi.PharmFlowRMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RMIClient {

    private static final int    RMI_PORT = 1099;
    private static final String RMI_NAME = "PharmFlowRMI";
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private PharmFlowRMI stub;
    private boolean       connecte = false;
    private final String  host;

    // ══════════════════════════════════════════════════════
    // CONSTRUCTEUR
    // ══════════════════════════════════════════════════════
    public RMIClient(String host) {
        this.host = host;
    }

    // ══════════════════════════════════════════════════════
    // CONNEXION
    // ══════════════════════════════════════════════════════
    public boolean connecter() {
        try {
            Registry registry = LocateRegistry.getRegistry(
                host, RMI_PORT);
            stub     = (PharmFlowRMI) registry.lookup(RMI_NAME);
            connecte = true;
            log("✔ Connecté au registre RMI — "
                + host + ":" + RMI_PORT);
            return true;
        } catch (Exception e) {
            connecte = false;
            log("✘ Connexion RMI échouée : " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════
    // PING
    // ══════════════════════════════════════════════════════
    public String ping() {
        try {
            return stub.ping();
        } catch (Exception e) {
            connecte = false;
            return "✘ RMI non disponible : " + e.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════
    // STATISTIQUES
    // ══════════════════════════════════════════════════════
    public Map<String, Object> getStatistiques() {
        try {
            return stub.getStatistiques();
        } catch (Exception e) {
            log("Erreur getStatistiques : " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    // ══════════════════════════════════════════════════════
    // RAPPORT STOCK
    // ══════════════════════════════════════════════════════
    public List<Produit> getRapportStock() {
        try {
            return stub.getRapportStock();
        } catch (Exception e) {
            log("Erreur getRapportStock : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ══════════════════════════════════════════════════════
    // ALERTES CRITIQUES
    // ══════════════════════════════════════════════════════
    public List<Produit> getAlertesCritiques() {
        try {
            return stub.getAlertesCritiques();
        } catch (Exception e) {
            log("Erreur getAlertesCritiques : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ══════════════════════════════════════════════════════
    // TOP 5 PLUS CHERS
    // ══════════════════════════════════════════════════════
    public List<Produit> getTop5PlusCher() {
        try {
            return stub.getTop5PlusCher();
        } catch (Exception e) {
            log("Erreur getTop5PlusCher : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ══════════════════════════════════════════════════════
    // HISTORIQUE MOUVEMENTS
    // ══════════════════════════════════════════════════════
    public List<Mouvement> getHistoriqueMouvements() {
        try {
            return stub.getHistoriqueMouvements();
        } catch (Exception e) {
            log("Erreur getHistoriqueMouvements : "
                + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ══════════════════════════════════════════════════════
    // VALEUR TOTALE
    // ══════════════════════════════════════════════════════
    public double getValeurTotaleStock() {
        try {
            return stub.getValeurTotaleStock();
        } catch (Exception e) {
            log("Erreur getValeurTotaleStock : " + e.getMessage());
            return 0.0;
        }
    }

    // ══════════════════════════════════════════════════════
    // RÉPARTITION CATÉGORIES
    // ══════════════════════════════════════════════════════
    public Map<String, Long> getRepartitionCategories() {
        try {
            return stub.getRepartitionCategories();
        } catch (Exception e) {
            log("Erreur getRepartitionCategories : "
                + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    // ══════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════
    public boolean isConnecte() { return connecte; }
    public String  getHost()    { return host;     }

    private void log(String msg) {
        System.out.println(
            "[RMI-Client "
            + LocalDateTime.now().format(FMT)
            + "] " + msg);
    }
}