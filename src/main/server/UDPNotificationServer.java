package main.server;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UDPNotificationServer {

    // ── Broadcast UDP (port 5001) ──────────────────────────
    private static final int    BROADCAST_PORT  = 5001;

    // ── Multicast UDP (port 5002) ──────────────────────────
    private static final int    MULTICAST_PORT  = 5002;
    private static final String MULTICAST_GROUP = "230.0.0.1";

    private DatagramSocket   broadcastSocket;
    private MulticastSocket  multicastSocket;
    private InetAddress      multicastGroup;
    private boolean          running = false;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    // ══════════════════════════════════════════════════════
    // DÉMARRAGE
    // ══════════════════════════════════════════════════════
    public void demarrer() throws IOException {
        // Socket broadcast UDP
        broadcastSocket = new DatagramSocket();
        broadcastSocket.setBroadcast(true);

        // Socket multicast UDP
        multicastSocket = new MulticastSocket(MULTICAST_PORT);
        multicastGroup  = InetAddress.getByName(MULTICAST_GROUP);
        multicastSocket.joinGroup(multicastGroup);

        running = true;
        log("✔ Serveur UDP Broadcast démarré (port " + BROADCAST_PORT + ")");
        log("✔ Serveur UDP Multicast démarré (groupe "
            + MULTICAST_GROUP + ":" + MULTICAST_PORT + ")");
    }

    // ══════════════════════════════════════════════════════
    // ARRÊT
    // ══════════════════════════════════════════════════════
    public void arreter() {
        running = false;
        try {
            if (multicastSocket != null && !multicastSocket.isClosed()) {
                multicastSocket.leaveGroup(multicastGroup);
                multicastSocket.close();
            }
            if (broadcastSocket != null && !broadcastSocket.isClosed())
                broadcastSocket.close();
            log("Serveur UDP arrêté.");
        } catch (IOException e) {
            System.err.println("Erreur arrêt UDP : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // ENVOI BROADCAST (à tous les PCs du réseau)
    // ══════════════════════════════════════════════════════
    public void envoyerBroadcast(String type, String message) {
        if (!running || broadcastSocket == null) return;
        try {
            String payload = buildPayload(type, message);
            byte[] data    = payload.getBytes("UTF-8");

            // Broadcast réseau local
            InetAddress broadcastAddr =
                InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(
                data, data.length, broadcastAddr, BROADCAST_PORT);
            broadcastSocket.send(packet);

            log("📡 Broadcast envoyé [" + type + "] : " + message);
        } catch (IOException e) {
            System.err.println("Erreur broadcast : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // ENVOI MULTICAST (au groupe 230.0.0.1)
    // ══════════════════════════════════════════════════════
    public void envoyerMulticast(String type, String message) {
        if (!running || multicastSocket == null) return;
        try {
            String payload = buildPayload(type, message);
            byte[] data    = payload.getBytes("UTF-8");

            DatagramPacket packet = new DatagramPacket(
                data, data.length, multicastGroup, MULTICAST_PORT);
            multicastSocket.send(packet);

            log("📢 Multicast envoyé [" + type + "] : " + message);
        } catch (IOException e) {
            System.err.println("Erreur multicast : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // NOTIFICATIONS MÉTIER
    // ══════════════════════════════════════════════════════

    // Appelé quand un médicament est ajouté
    public void notifierAjout(String nomMedicament, int quantite) {
        String msg = "AJOUT|" + nomMedicament + "|" + quantite;
        envoyerBroadcast("STOCK_UPDATE", msg);
        envoyerMulticast("STOCK_UPDATE", msg);
    }

    // Appelé quand un médicament est modifié
    public void notifierModification(String nomMedicament) {
        String msg = "MODIF|" + nomMedicament;
        envoyerBroadcast("STOCK_UPDATE", msg);
        envoyerMulticast("STOCK_UPDATE", msg);
    }

    // Appelé quand un médicament est supprimé
    public void notifierSuppression(String nomMedicament) {
        String msg = "SUPPRESSION|" + nomMedicament;
        envoyerBroadcast("STOCK_UPDATE", msg);
        envoyerMulticast("STOCK_UPDATE", msg);
    }

    // Appelé quand une entrée/sortie de stock est faite
    public void notifierMouvement(String type, String nomMedicament,
                                   int quantite) {
        String msg = type + "|" + nomMedicament + "|" + quantite;
        envoyerBroadcast("MOUVEMENT", msg);
        envoyerMulticast("MOUVEMENT", msg);
    }

    // Appelé quand un stock est critique
    public void notifierStockCritique(String nomMedicament, int quantite,
                                       int seuil) {
        String msg = "CRITIQUE|" + nomMedicament
            + "|qte=" + quantite + "|seuil=" + seuil;
        envoyerBroadcast("ALERTE", msg);
        envoyerMulticast("ALERTE", msg);
    }

    // Appelé quand un médicament expire bientôt
    public void notifierExpiration(String nomMedicament, int joursRestants) {
        String msg = "EXPIRATION|" + nomMedicament
            + "|jours=" + joursRestants;
        envoyerBroadcast("ALERTE", msg);
        envoyerMulticast("ALERTE", msg);
    }

    // ══════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════
    private String buildPayload(String type, String message) {
        return "PHARMFLOW|" + type + "|"
            + LocalDateTime.now().format(FMT) + "|" + message;
    }

    private void log(String msg) {
        System.out.println("[UDP " + LocalDateTime.now().format(FMT)
            + "] " + msg);
    }

    // Getters
    public boolean isRunning()      { return running; }
    public int getBroadcastPort()   { return BROADCAST_PORT; }
    public int getMulticastPort()   { return MULTICAST_PORT; }
    public String getMulticastGroup(){ return MULTICAST_GROUP; }
}