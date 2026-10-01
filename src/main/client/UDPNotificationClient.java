package main.client;

import javafx.application.Platform;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UDPNotificationClient implements Runnable {

    // ── Ports ──────────────────────────────────────────────
    private static final int    BROADCAST_PORT  = 5001;
    private static final int    MULTICAST_PORT  = 5002;
    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int    BUFFER_SIZE     = 1024;

    // ── Sockets ────────────────────────────────────────────
    private DatagramSocket  broadcastSocket;
    private MulticastSocket multicastSocket;
    private InetAddress     multicastGroup;
    private boolean         running = false;

    // ── Callback vers l'UI ─────────────────────────────────
    private final NotificationCallback callback;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    // ══════════════════════════════════════════════════════
    // INTERFACE CALLBACK
    // ══════════════════════════════════════════════════════
    public interface NotificationCallback {
        void onStockUpdate(String action, String medicament, String details);
        void onMouvement(String type, String medicament, int quantite);
        void onAlerte(String type, String medicament, String details);
    }

    // ══════════════════════════════════════════════════════
    // CONSTRUCTEUR
    // ══════════════════════════════════════════════════════
    public UDPNotificationClient(NotificationCallback callback) {
        this.callback = callback;
    }

    // ══════════════════════════════════════════════════════
    // DÉMARRAGE
    // ══════════════════════════════════════════════════════
    public void demarrer() throws IOException {
        // Socket broadcast
        broadcastSocket = new DatagramSocket(BROADCAST_PORT);
        broadcastSocket.setBroadcast(true);
        broadcastSocket.setSoTimeout(1000);

        // Socket multicast
        multicastSocket = new MulticastSocket(MULTICAST_PORT);
        multicastGroup  = InetAddress.getByName(MULTICAST_GROUP);
        multicastSocket.joinGroup(multicastGroup);
        multicastSocket.setSoTimeout(1000);

        running = true;

        // Thread broadcast
        Thread tBroadcast = new Thread(this::ecouterBroadcast,
            "UDP-Broadcast-Listener");
        tBroadcast.setDaemon(true);
        tBroadcast.start();

        // Thread multicast
        Thread tMulticast = new Thread(this::ecouterMulticast,
            "UDP-Multicast-Listener");
        tMulticast.setDaemon(true);
        tMulticast.start();

        log("✔ Écoute Broadcast UDP (port " + BROADCAST_PORT + ")");
        log("✔ Écoute Multicast UDP (groupe "
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
            log("Client UDP arrêté.");
        } catch (IOException e) {
            System.err.println("Erreur arrêt UDP client : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // ÉCOUTE BROADCAST
    // ══════════════════════════════════════════════════════
    private void ecouterBroadcast() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length);
                broadcastSocket.receive(packet);
                String msg = new String(
                    packet.getData(), 0,
                    packet.getLength(), "UTF-8");
                log("📡 Broadcast reçu de "
                    + packet.getAddress().getHostAddress()
                    + " : " + msg);
                traiterMessage(msg);
            } catch (SocketTimeoutException ignored) {
                // Timeout normal pour vérifier running
            } catch (IOException e) {
                if (running)
                    System.err.println("Erreur broadcast : "
                        + e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // ÉCOUTE MULTICAST
    // ══════════════════════════════════════════════════════
    private void ecouterMulticast() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String msg = new String(
                    packet.getData(), 0,
                    packet.getLength(), "UTF-8");
                log("📢 Multicast reçu de "
                    + packet.getAddress().getHostAddress()
                    + " : " + msg);
                traiterMessage(msg);
            } catch (SocketTimeoutException ignored) {
                // Timeout normal pour vérifier running
            } catch (IOException e) {
                if (running)
                    System.err.println("Erreur multicast : "
                        + e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // TRAITEMENT DES MESSAGES
    // Format: PHARMFLOW|TYPE|HEURE|CONTENU
    // ══════════════════════════════════════════════════════
    private void traiterMessage(String msg) {
        try {
            String[] parts = msg.split("\\|");
            if (parts.length < 4) return;
            if (!parts[0].equals("PHARMFLOW")) return;

            String type    = parts[1]; // STOCK_UPDATE, MOUVEMENT, ALERTE
            // parts[2] = heure
            String contenu = parts[3]; // action|medicament|details

            String[] details = contenu.split("\\|");
            if (details.length == 0) return;

            switch (type) {
                case "STOCK_UPDATE" -> {
                    String action    = details[0];
                    String medicament= details.length > 1 ? details[1] : "?";
                    String info      = details.length > 2 ? details[2] : "";
                    Platform.runLater(() ->
                        callback.onStockUpdate(action, medicament, info));
                }
                case "MOUVEMENT" -> {
                    String typeMouv  = details[0];
                    String medicament= details.length > 1 ? details[1] : "?";
                    int    quantite  = details.length > 2
                        ? Integer.parseInt(details[2]) : 0;
                    Platform.runLater(() ->
                        callback.onMouvement(typeMouv, medicament, quantite));
                }
                case "ALERTE" -> {
                    String typeAlerte = details[0];
                    String medicament = details.length > 1 ? details[1] : "?";
                    String info       = details.length > 2 ? details[2] : "";
                    Platform.runLater(() ->
                        callback.onAlerte(typeAlerte, medicament, info));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur traitement message UDP : "
                + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // RUNNABLE (pour utilisation dans un Thread)
    // ══════════════════════════════════════════════════════
    @Override
    public void run() {
        try {
            demarrer();
        } catch (IOException e) {
            System.err.println("Impossible de démarrer UDP client : "
                + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════
    private void log(String msg) {
        System.out.println("[UDP-Client "
            + LocalDateTime.now().format(FMT) + "] " + msg);
    }

    public boolean isRunning() { return running; }
}