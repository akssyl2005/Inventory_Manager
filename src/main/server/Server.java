package main.server;

import main.model.MouvementStore;
import main.model.ProduitStore;
import main.rmi.RMIServer;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Server {

    private static final int  PORT = 5000;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {

        ProduitStore   produitStore   = SharedData.getProduitStore();
        MouvementStore mouvementStore = SharedData.getMouvementStore();
        UDPNotificationServer udpServer      = new UDPNotificationServer();
        RMIServer             rmiServer      = new RMIServer();

        afficherBanniere();

        // ── Démarrage serveur UDP ──────────────────────────
        try {
            udpServer.demarrer();
            log("✔ UDP Broadcast  — port "
                + udpServer.getBroadcastPort());
            log("✔ UDP Multicast  — groupe "
                + udpServer.getMulticastGroup()
                + ":" + udpServer.getMulticastPort());
        } catch (IOException e) {
            log("⚠ UDP non disponible : " + e.getMessage());
        }

        // ── Démarrage serveur RMI ──────────────────────────
       try {
    rmiServer.demarrer(produitStore, mouvementStore);
    log("✔ RMI Registry   — port " + rmiServer.getPort());
    log("✔ RMI Service    — nom  " + rmiServer.getName());
} catch (Exception e) {
    log("⚠ RMI non disponible : " + e.getMessage());
}

// ── Démarrage serveur Rapports (hétérogène) ────────
Thread reportThread = new Thread(() -> {
    ReportServer.main(new String[]{});
}, "Report-Server");
reportThread.setDaemon(true);
reportThread.start();
log("✔ Serveur Rapports  — port 5003");

        // ── Démarrage serveur TCP ──────────────────────────
        log("Serveur TCP PharmFlow démarré sur le port " + PORT);
        log("En attente de connexions...");

        // ── Hook d'arrêt propre ────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            udpServer.arreter();
            rmiServer.arreter();
            log("Serveur arrêté proprement.");
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);

            // ── Notifications périodiques UDP ──────────────
            Thread alertThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(30_000);
                        verifierEtNotifierAlertes(
                            produitStore, udpServer);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "Alert-Monitor");
            alertThread.setDaemon(true);
            alertThread.start();

            // ── Boucle principale TCP ──────────────────────
            while (true) {
                Socket socket = serverSocket.accept();
                String clientInfo =
                    socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort();
                log("Nouveau client TCP : " + clientInfo);

                Thread t = new Thread(
                    new ClientHandler(
                        socket, produitStore,
                        mouvementStore, udpServer)
                );
                t.setDaemon(true);
                t.setName("Client-" + clientInfo);
                t.start();

                log("Thread lancé : " + t.getName()
                    + " | Actifs : " + Thread.activeCount());
            }

        } catch (IOException e) {
            System.err.println("Erreur serveur TCP : "
                + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // VÉRIFICATION PÉRIODIQUE DES ALERTES
    // ══════════════════════════════════════════════════════
    private static void verifierEtNotifierAlertes(
            ProduitStore store,
            UDPNotificationServer udp) {

        store.getStockCritique().forEach(p -> {
            udp.notifierStockCritique(
                p.getNom(),
                p.getQuantite(),
                p.getSeuilReappro()
            );
            log("⚠ Alerte stock critique : "
                + p.getNom()
                + " (" + p.getQuantite() + " unités)");
        });

        store.getExpireBientot().forEach(p -> {
            udp.notifierExpiration(
                p.getNom(),
                p.joursAvantExpiration()
            );
            log("⚠ Alerte expiration : "
                + p.getNom()
                + " (J-" + p.joursAvantExpiration() + ")");
        });

        store.getExpires().forEach(p -> {
            udp.envoyerBroadcast("ALERTE",
                "EXPIRÉ|" + p.getNom() + "|date="
                + (p.getDateExpiration() != null
                    ? p.getDateExpiration().toString()
                    : "N/A"));
            log("🔴 Médicament expiré : " + p.getNom());
        });
    }

    // ══════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════
    private static void log(String msg) {
        System.out.println(
            "[" + LocalDateTime.now().format(FMT) + "] " + msg);
    }

    private static void afficherBanniere() {
        System.out.println(
            "╔══════════════════════════════════════════════════╗");
        System.out.println(
            "║         PharmFlow Pro — Serveur Distribué        ║");
        System.out.println(
            "║        Gestion de Stocks Pharmacie  v3.0         ║");
        System.out.println(
            "╠══════════════════════════════════════════════════╣");
        System.out.println(
            "║  TCP  : port 5000  (clients)                     ║");
        System.out.println(
            "║  UDP  : port 5001  (broadcast notifications)     ║");
        System.out.println(
            "║  UDP  : port 5002  (multicast 230.0.0.1)         ║");
        System.out.println(
            "║  RMI  : port 1099  (rapports & statistiques)     ║");
        System.out.println(
            "║  Threads : Serveur concurrent multi-clients      ║");
        System.out.println(
            "╚══════════════════════════════════════════════════╝");
    }
}