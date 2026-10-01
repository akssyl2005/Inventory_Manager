package main.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Produit implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Champs de base ─────────────────────────────────────────
    private int    id;
    private String nom;
    private double prix;
    private int    quantite;
    private String categorie;

    // ── Nouveaux champs pharmacie ──────────────────────────────
    private String    dci;           // Dénomination Commune Internationale
    private String    fournisseur;
    private String    numeroLot;
    private LocalDate dateExpiration;
    private int       seuilReappro;  // seuil de réapprovisionnement

    // ── Constructeur complet ───────────────────────────────────
    public Produit(int id, String nom, double prix, int quantite, String categorie,
                   String dci, String fournisseur, String numeroLot,
                   LocalDate dateExpiration, int seuilReappro) {
        this.id             = id;
        this.nom            = nom;
        this.prix           = prix;
        this.quantite       = quantite;
        this.categorie      = categorie;
        this.dci            = dci == null ? "" : dci;
        this.fournisseur    = fournisseur == null ? "" : fournisseur;
        this.numeroLot      = numeroLot == null ? "" : numeroLot;
        this.dateExpiration = dateExpiration;
        this.seuilReappro   = seuilReappro;
    }

    // ── Constructeur simple (rétrocompatibilité) ───────────────
    public Produit(int id, String nom, double prix, int quantite, String categorie) {
        this(id, nom, prix, quantite, categorie, "", "", "", null, 10);
    }

    // ── Getters ────────────────────────────────────────────────
    public int       getId()             { return id; }
    public String    getNom()            { return nom; }
    public double    getPrix()           { return prix; }
    public int       getQuantite()       { return quantite; }
    public String    getCategorie()      { return categorie; }
    public String    getDci()            { return dci; }
    public String    getFournisseur()    { return fournisseur; }
    public String    getNumeroLot()      { return numeroLot; }
    public LocalDate getDateExpiration() { return dateExpiration; }
    public int       getSeuilReappro()   { return seuilReappro; }

    // ── Setters ────────────────────────────────────────────────
    public void setId(int id)                           { this.id = id; }
    public void setNom(String nom)                      { this.nom = nom; }
    public void setPrix(double prix)                    { this.prix = prix; }
    public void setQuantite(int q)                      { this.quantite = q; }
    public void setCategorie(String c)                  { this.categorie = c; }
    public void setDci(String dci)                      { this.dci = dci; }
    public void setFournisseur(String f)                { this.fournisseur = f; }
    public void setNumeroLot(String l)                  { this.numeroLot = l; }
    public void setDateExpiration(LocalDate d)          { this.dateExpiration = d; }
    public void setSeuilReappro(int s)                  { this.seuilReappro = s; }

    // ── Méthodes métier ────────────────────────────────────────
    public boolean estExpire() {
        return dateExpiration != null && dateExpiration.isBefore(LocalDate.now());
    }

    public boolean expireBientot() {
        if (dateExpiration == null) return false;
        return dateExpiration.isBefore(LocalDate.now().plusDays(30))
            && !estExpire();
    }

    public boolean stockCritique() {
        return quantite < seuilReappro;
    }

    public int joursAvantExpiration() {
        if (dateExpiration == null) return Integer.MAX_VALUE;
        return (int) LocalDate.now().until(dateExpiration).getDays();
    }

    // ── Sérialisation JSON ─────────────────────────────────────
    public String toJson() {
        return String.format(Locale.US,
            "{\"id\":%d,\"nom\":\"%s\",\"prix\":%.2f,\"quantite\":%d," +
            "\"categorie\":\"%s\",\"dci\":\"%s\",\"fournisseur\":\"%s\"," +
            "\"numeroLot\":\"%s\",\"dateExpiration\":\"%s\",\"seuilReappro\":%d}",
            id, escape(nom), prix, quantite,
            escape(categorie), escape(dci), escape(fournisseur),
            escape(numeroLot),
            dateExpiration != null ? dateExpiration.format(FMT) : "",
            seuilReappro
        );
    }

    public static Produit fromJson(String json) {
        try {
            json = json.trim().replaceAll("^\\{|\\}$", "");
            String[] fields = json.split(",(?=\")");

            int       id = 0, quantite = 0, seuilReappro = 10;
            double    prix = 0;
            String    nom = "", categorie = "", dci = "", fournisseur = "", numeroLot = "", dateExp = "";

            for (String field : fields) {
                int ci = field.indexOf(':');
                if (ci == -1) continue;
                String key = field.substring(0, ci).replaceAll("\"", "").trim();
                String val = field.substring(ci + 1).replaceAll("\"", "").trim();
                switch (key) {
                    case "id"             -> id           = Integer.parseInt(val);
                    case "nom"            -> nom          = val;
                    case "prix"           -> prix         = Double.parseDouble(val);
                    case "quantite"       -> quantite     = Integer.parseInt(val);
                    case "categorie"      -> categorie    = val;
                    case "dci"            -> dci          = val;
                    case "fournisseur"    -> fournisseur  = val;
                    case "numeroLot"      -> numeroLot    = val;
                    case "dateExpiration" -> dateExp      = val;
                    case "seuilReappro"   -> seuilReappro = Integer.parseInt(val);
                }
            }

            LocalDate date = null;
            if (!dateExp.isEmpty()) {
                try { date = LocalDate.parse(dateExp, FMT); }
                catch (Exception ignored) {}
            }

            return new Produit(id, nom, prix, quantite, categorie,
                               dci, fournisseur, numeroLot, date, seuilReappro);
        } catch (Exception e) {
            System.err.println("Produit.fromJson error: " + e.getMessage());
            return null;
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"").replace("\n", "");
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
            "Produit{id=%d, nom='%s', dci='%s', prix=%.2f DA, " +
            "quantite=%d, categorie='%s', lot='%s', fournisseur='%s', expiration=%s, seuil=%d}",
            id, nom, dci, prix, quantite, categorie,
            numeroLot, fournisseur,
            dateExpiration != null ? dateExpiration.format(FMT) : "N/A",
            seuilReappro);
    }
}