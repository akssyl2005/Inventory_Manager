package main.model;

import java.util.Locale;

public class Produit {
    private int id;
    private String nom;
    private double prix;
    private int quantite;
    private String categorie;

    public Produit(int id, String nom, double prix, int quantite, String categorie) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
        this.quantite = quantite;
        this.categorie = categorie;
    }

    // Getters & Setters
    public int getId()           { return id; }
    public String getNom()       { return nom; }
    public double getPrix()      { return prix; }
    public int getQuantite()     { return quantite; }
    public String getCategorie() { return categorie; }

    public void setId(int id)            { this.id = id; }
    public void setNom(String nom)       { this.nom = nom; }
    public void setPrix(double prix)     { this.prix = prix; }
    public void setQuantite(int q)       { this.quantite = q; }
    public void setCategorie(String cat) { this.categorie = cat; }

   

    // Sérialisation — forcer le point comme séparateur décimal
    public String toJson() {
        return String.format(
            Locale.US,  // ← forcer le point décimal
            "{\"id\":%d,\"nom\":\"%s\",\"prix\":%.2f,\"quantite\":%d,\"categorie\":\"%s\"}",
            id, nom, prix, quantite, categorie
        );
    }

    // Désérialisation — parser avec Locale.US aussi
    public static Produit fromJson(String json) {
        try {
            json = json.trim().replaceAll("[{}]", "");
            String[] fields = json.split(",(?=\")"); // ← split uniquement avant les guillemets ou chiffres
            int id = 0;
            String nom = "", categorie = "";
            double prix = 0;
            int quantite = 0;

            for (String field : fields) {
                int colonIdx = field.indexOf(':');
                String key = field.substring(0, colonIdx).replaceAll("\"", "").trim();
                String val = field.substring(colonIdx + 1).replaceAll("\"", "").trim();

                switch (key) {
                    case "id"        -> id        = Integer.parseInt(val);
                    case "nom"       -> nom       = val;
                    case "categorie" -> categorie = val;
                    case "prix"      -> prix      = Double.parseDouble(val);
                    case "quantite"  -> quantite  = Integer.parseInt(val);
                }
            }
            return new Produit(id, nom, prix, quantite, categorie);
        } catch (Exception e) {
            System.err.println("fromJson error: " + e.getMessage() + " | json=" + json);
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Produit{id=%d, nom='%s', prix=%.2f DA, quantite=%d, categorie='%s'}",
                id, nom, prix, quantite, categorie);
    }
}