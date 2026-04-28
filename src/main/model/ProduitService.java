package main.model;


import java.io.*;

public class ProduitService {
    private final PrintWriter out;
    private final BufferedReader in;

    public ProduitService(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in  = in;
    }

    public void ajouter(Produit p) throws IOException {
        out.println(Protocol.ADD + ":" + p.toJson());
        String reponse = in.readLine();
        if (reponse.startsWith(Protocol.OK + ":")) {
            Produit confirme = Produit.fromJson(reponse.substring(3));
            System.out.println("✔ Produit ajouté → " + confirme);
        } else {
            System.out.println("✘ " + reponse);
        }
    }

    public void supprimer(int id) throws IOException {
        out.println(Protocol.DELETE + ":" + id);
        String reponse = in.readLine();
        if (reponse.startsWith(Protocol.OK)) {
            System.out.println("✔ " + reponse.substring(3));
        } else {
            System.out.println("✘ " + reponse.substring(6));
        }
    }

    public void modifier(int id, Produit p) throws IOException {
        p.setId(id);
        out.println(Protocol.UPDATE + ":" + p.toJson());
        String reponse = in.readLine();
        if (reponse.startsWith(Protocol.OK + ":")) {
            Produit modifie = Produit.fromJson(reponse.substring(3));
            System.out.println("✔ Produit modifié → " + modifie);
        } else {
            System.out.println("✘ " + reponse.substring(6));
        }
    }

    public void afficherListe() throws IOException {
        out.println(Protocol.LIST);
        String reponse = in.readLine();

        if (reponse.equals(Protocol.EMPTY)) {
            System.out.println("Aucun produit enregistré.");
            return;
        }

        String[] produits = reponse.split(";");
        System.out.println(String.format("%-5s %-15s %-10s %-8s %-12s",
                "ID", "Nom", "Prix", "Qté", "Catégorie"));
        System.out.println("─".repeat(55));
        for (String json : produits) {
            Produit p = Produit.fromJson(json);
            if (p != null) {
                System.out.println(String.format("%-5d %-15s %-10.2f %-8d %-12s",
                        p.getId(), p.getNom(), p.getPrix(),
                        p.getQuantite(), p.getCategorie()));
            }
        }
        System.out.println("─".repeat(55));
        System.out.println("Total : " + produits.length + " produit(s)");
    }

    public void quitter() throws IOException {
        out.println(Protocol.EXIT);
        System.out.println("Serveur : " + in.readLine());
    }
}