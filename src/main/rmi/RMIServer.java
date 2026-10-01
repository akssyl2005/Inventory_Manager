package main.rmi;

import main.model.MouvementStore;
import main.model.ProduitStore;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RMIServer {

    private static final int    RMI_PORT = 1099;
    private static final String RMI_NAME = "PharmFlowRMI";
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private Registry        registry;
    private PharmFlowRMIImpl impl;

    // ══════════════════════════════════════════════════════
    // DÉMARRAGE
    // ══════════════════════════════════════════════════════
    public void demarrer(ProduitStore ps, MouvementStore ms) {
        try {
            // Créer l'implémentation RMI
            impl = new PharmFlowRMIImpl(ps, ms);

            // Créer le registre RMI sur le port 1099
            registry = LocateRegistry.createRegistry(RMI_PORT);

            // Enregistrer le service
            registry.rebind(RMI_NAME, impl);

            log("✔ Registre RMI démarré sur le port " + RMI_PORT);
            log("✔ Service enregistré sous le nom : " + RMI_NAME);
            log("✔ URL RMI : rmi://localhost:"
                + RMI_PORT + "/" + RMI_NAME);

        } catch (Exception e) {
            System.err.println("Erreur démarrage RMI : "
                + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // ARRÊT
    // ══════════════════════════════════════════════════════
    public void arreter() {
        try {
            if (registry != null) {
                registry.unbind(RMI_NAME);
                log("Registre RMI arrêté.");
            }
        } catch (Exception e) {
            System.err.println("Erreur arrêt RMI : "
                + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // GETTERS
    // ══════════════════════════════════════════════════════
    public int    getPort() { return RMI_PORT; }
    public String getName() { return RMI_NAME; }

    // ══════════════════════════════════════════════════════
    // UTILITAIRE
    // ══════════════════════════════════════════════════════
    private void log(String msg) {
        System.out.println(
            "[RMI " + LocalDateTime.now().format(FMT)
            + "] " + msg);
    }
}