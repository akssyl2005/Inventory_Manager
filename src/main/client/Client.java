package main.client;

import main.model.Produit;
import main.model.ProduitService;
import main.model.ProduitSaisie;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT)) {

            PrintWriter   out     = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner        scanner = new Scanner(System.in);

            ProduitService service = new ProduitService(out, in);
            ProduitSaisie  saisie  = new ProduitSaisie(scanner);
            Menu           menu    = new Menu(scanner);

            System.out.println("Connecté au serveur " + HOST + ":" + PORT);

            boolean running = true;
            while (running) {
                menu.afficher();
                String choix = menu.lireChoix();

                switch (choix) {
                    case "1" -> {
                        System.out.println("\n── Ajouter un produit ──");
                        Produit p = saisie.saisir();
                        if (p != null) service.ajouter(p);
                    }
                    case "2" -> {
                        System.out.println("\n── Supprimer un produit ──");
                        int id = saisie.saisirId("supprimer");
                        if (id != -1) service.supprimer(id);
                    }
                    case "3" -> {
                        System.out.println("\n── Modifier un produit ──");
                        int id = saisie.saisirId("modifier");
                        if (id != -1) {
                            Produit p = saisie.saisir();
                            if (p != null) service.modifier(id, p);
                        }
                    }
                    case "4" -> {
                        System.out.println("\n── Liste des produits ──");
                        service.afficherListe();
                    }
                    case "0" -> {
                        service.quitter();
                        running = false;
                    }
                    default -> System.out.println("⚠ Choix invalide.");
                }
            }

            System.out.println("Connexion fermée.");

        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter. Le serveur est-il démarré ?");
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}