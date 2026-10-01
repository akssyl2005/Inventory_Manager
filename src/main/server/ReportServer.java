package main.server;

import main.model.*;
import main.rmi.RMIServer;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportServer {

    private static final int PORT = 5003;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {

        // ── Partage des données avec Server principal ──────
        // ReportServer utilise les MÊMES stores
        // passés en paramètre depuis Server.java
        ProduitStore   produitStore   = SharedData.getProduitStore();
        MouvementStore mouvementStore = SharedData.getMouvementStore();

        afficherBanniere();
        log("Serveur de RAPPORTS démarré sur port " + PORT);
        log("Spécialisé : statistiques, rapports, alertes");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket socket = serverSocket.accept();
                String clientInfo =
                    socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort();
                log("Client rapport connecté : " + clientInfo);

                Thread t = new Thread(
                    new ReportClientHandler(
                        socket, produitStore, mouvementStore)
                );
                t.setDaemon(true);
                t.setName("Report-Client-" + clientInfo);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Erreur ReportServer : "
                + e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println(
            "[REPORT " + LocalDateTime.now().format(FMT)
            + "] " + msg);
    }

    private static void afficherBanniere() {
        System.out.println(
            "╔══════════════════════════════════════════════════╗");
        System.out.println(
            "║      PharmFlow Pro — Serveur de Rapports         ║");
        System.out.println(
            "║         Serveur HÉTÉROGÈNE  port 5003            ║");
        System.out.println(
            "╠══════════════════════════════════════════════════╣");
        System.out.println(
            "║  Rôle : Statistiques, Rapports, Alertes          ║");
        System.out.println(
            "║  Différent du serveur principal (port 5000)      ║");
        System.out.println(
            "╚══════════════════════════════════════════════════╝");
    }
}