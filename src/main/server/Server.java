package main.server;

import main.model.Produit;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 5000;
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<Produit> stockProduits = new ArrayList<>();
    private static final AtomicInteger compteur = new AtomicInteger(1); // générateur d'id

    public static void main(String[] args) {
        System.out.println("Serveur démarré sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[" + now() + "] Client connecté : " + socket.getInetAddress());
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String ligne;
            while ((ligne = in.readLine()) != null) {

                // Déconnexion propre
                if (ligne.equalsIgnoreCase("exit")) {
                    out.println("Au revoir !");
                    System.out.println("[" + now() + "] Client déconnecté.");
                    break;
                }

                // Désérialisation
                Produit produit = Produit.fromJson(ligne);
                if (produit == null) {
                    out.println("ERREUR : données invalides.");
                    continue;
                }

                // Assignation de l'id par le serveur
                int id = compteur.getAndIncrement();
                produit.setId(id);

                // Stockage thread-safe
                synchronized (stockProduits) {
                    stockProduits.add(produit);
                }

                System.out.println("[" + now() + "] Reçu → " + produit);
                System.out.println("Stock total : " + stockProduits.size() + " produit(s)");

                // Renvoyer le produit complet (avec id) au client
                out.println("OK:" + produit.toJson());
            }
        } catch (IOException e) {
            System.err.println("Erreur client : " + e.getMessage());
        }
    }

    private static String now() {
        return LocalDateTime.now().format(fmt);
    }
}