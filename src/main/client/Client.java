package main.client;

import main.model.Produit;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (
            Socket socket         = new Socket(HOST, PORT);
            PrintWriter out       = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner       = new Scanner(System.in)
        ) {
            System.out.println("Connecté au serveur " + HOST + ":" + PORT);
            System.out.println("Tapez 'exit' pour quitter.\n");

            while (true) {
                Produit produit = saisirProduit(scanner);

                if (produit == null) {
                    out.println("exit");
                    String rep = in.readLine();
                    System.out.println("Serveur : " + rep);
                    break;
                }

                // Envoyer le produit (sans id, le serveur l'assigne)
                out.println(produit.toJson());

                // Lire la réponse du serveur
                String reponse = in.readLine();

                if (reponse != null && reponse.startsWith("OK:")) {
                    // Reconstruire le produit avec l'id assigné par le serveur
                    Produit confirme = Produit.fromJson(reponse.substring(3));
                    if (confirme != null) {
                        System.out.println("✔ Enregistré avec ID=" + confirme.getId() + " → " + confirme);
                    }
                } else {
                    System.out.println("Serveur : " + reponse);
                }

                System.out.println();
            }

            System.out.println("Connexion fermée.");

        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter. Le serveur est-il démarré ?");
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private static Produit saisirProduit(Scanner scanner) {
        try {
            System.out.print("Nom du produit (ou 'exit') : ");
            String nom = scanner.nextLine().trim();
            if (nom.equalsIgnoreCase("exit")) return null;
            if (nom.isEmpty()) {
                System.out.println("⚠ Nom invalide, réessayez.");
                return saisirProduit(scanner);
            }

            System.out.print("Prix : ");
            double prix = Double.parseDouble(scanner.nextLine().trim());
            if (prix < 0) { System.out.println("⚠ Prix invalide."); return saisirProduit(scanner); }

            System.out.print("Quantité : ");
            int quantite = Integer.parseInt(scanner.nextLine().trim());
            if (quantite < 0) { System.out.println("⚠ Quantité invalide."); return saisirProduit(scanner); }

            System.out.print("Catégorie : ");
            String categorie = scanner.nextLine().trim();

            // id=0 par défaut, sera assigné par le serveur
            return new Produit(0, nom, prix, quantite, categorie);

        } catch (NumberFormatException e) {
            System.out.println("⚠ Valeur numérique invalide, réessayez.");
            return saisirProduit(scanner);
        }
    }
}