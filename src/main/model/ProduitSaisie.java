package main.model;

import java.util.Scanner;

public class ProduitSaisie {
    private final Scanner scanner;

    public ProduitSaisie(Scanner scanner) {
        this.scanner = scanner;
    }

    public Produit saisir() {
        try {
            System.out.print("Nom : ");
            String nom = scanner.nextLine().trim();
            if (nom.isEmpty()) { System.out.println("⚠ Nom invalide."); return null; }

            System.out.print("Prix : ");
            double prix = Double.parseDouble(scanner.nextLine().trim());
            if (prix < 0) { System.out.println("⚠ Prix invalide."); return null; }

            System.out.print("Quantité : ");
            int quantite = Integer.parseInt(scanner.nextLine().trim());
            if (quantite < 0) { System.out.println("⚠ Quantité invalide."); return null; }

            System.out.print("Catégorie : ");
            String categorie = scanner.nextLine().trim();

            return new Produit(0, nom, prix, quantite, categorie);

        } catch (NumberFormatException e) {
            System.out.println("⚠ Valeur numérique invalide.");
            return null;
        }
    }

    public int saisirId(String action) {
        try {
            System.out.print("ID du produit à " + action + " : ");
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("⚠ ID invalide.");
            return -1;
        }
    }
}