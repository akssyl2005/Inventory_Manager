package main.client;

import java.util.Scanner;

public class Menu {
    private final Scanner scanner;

    public Menu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void afficher() {
        System.out.println("\n╔══════════════════════════╗");
        System.out.println("║      GESTION PRODUITS    ║");
        System.out.println("╠══════════════════════════╣");
        System.out.println("║  1. Ajouter un produit   ║");
        System.out.println("║  2. Supprimer un produit ║");
        System.out.println("║  3. Modifier un produit  ║");
        System.out.println("║  4. Afficher la liste    ║");
        System.out.println("║  0. Quitter              ║");
        System.out.println("╚══════════════════════════╝");
        System.out.print("Votre choix : ");
    }

    public String lireChoix() {
        return scanner.nextLine().trim();
    }
}