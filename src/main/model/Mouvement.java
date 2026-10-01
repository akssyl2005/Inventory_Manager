package main.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mouvement implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Types de mouvement ─────────────────────────────────────
    public enum Type { ENTREE, SORTIE }

    // ── Champs ────────────────────────────────────────────────
    private int           id;
    private int           produitId;
    private String        produitNom;
    private Type          type;
    private int           quantite;
    private String        motif;
    private String        operateur;
    private LocalDateTime dateHeure;

    // ── Constructeur ──────────────────────────────────────────
    public Mouvement(int id, int produitId, String produitNom,
                     Type type, int quantite, String motif, String operateur) {
        this.id         = id;
        this.produitId  = produitId;
        this.produitNom = produitNom;
        this.type       = type;
        this.quantite   = quantite;
        this.motif      = motif == null ? "" : motif;
        this.operateur  = operateur == null ? "Système" : operateur;
        this.dateHeure  = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────
    public int           getId()         { return id; }
    public int           getProduitId()  { return produitId; }
    public String        getProduitNom() { return produitNom; }
    public Type          getType()       { return type; }
    public int           getQuantite()   { return quantite; }
    public String        getMotif()      { return motif; }
    public String        getOperateur()  { return operateur; }
    public LocalDateTime getDateHeure()  { return dateHeure; }

    // ── Setters ───────────────────────────────────────────────
    public void setId(int id)               { this.id = id; }
    public void setDateHeure(LocalDateTime d){ this.dateHeure = d; }

    // ── Méthodes métier ───────────────────────────────────────
    public String getTypeLabel() {
        return type == Type.ENTREE ? "Entrée" : "Sortie";
    }

    public String getDateHeureStr() {
        return dateHeure != null ? dateHeure.format(FMT) : "";
    }

    // ── Sérialisation JSON ────────────────────────────────────
    public String toJson() {
        return String.format(
            "{\"id\":%d,\"produitId\":%d,\"produitNom\":\"%s\"," +
            "\"type\":\"%s\",\"quantite\":%d,\"motif\":\"%s\"," +
            "\"operateur\":\"%s\",\"dateHeure\":\"%s\"}",
            id, produitId, escape(produitNom),
            type.name(), quantite,
            escape(motif), escape(operateur),
            getDateHeureStr()
        );
    }

    public static Mouvement fromJson(String json) {
        try {
            json = json.trim().replaceAll("^\\{|\\}$", "");
            String[] fields = json.split(",(?=\")");

            int    id = 0, produitId = 0, quantite = 0;
            String produitNom = "", typeStr = "ENTREE";
            String motif = "", operateur = "Système", dateStr = "";

            for (String field : fields) {
                int ci = field.indexOf(':');
                if (ci == -1) continue;
                String key = field.substring(0, ci).replaceAll("\"", "").trim();
                String val = field.substring(ci + 1).replaceAll("\"", "").trim();
                switch (key) {
                    case "id"         -> id         = Integer.parseInt(val);
                    case "produitId"  -> produitId  = Integer.parseInt(val);
                    case "produitNom" -> produitNom = val;
                    case "type"       -> typeStr    = val;
                    case "quantite"   -> quantite   = Integer.parseInt(val);
                    case "motif"      -> motif      = val;
                    case "operateur"  -> operateur  = val;
                    case "dateHeure"  -> dateStr    = val;
                }
            }

            Type type = typeStr.equals("ENTREE") ? Type.ENTREE : Type.SORTIE;
            Mouvement m = new Mouvement(id, produitId, produitNom,
                                        type, quantite, motif, operateur);
            m.setId(id);
            if (!dateStr.isEmpty()) {
                try { m.setDateHeure(LocalDateTime.parse(dateStr, FMT)); }
                catch (Exception ignored) {}
            }
            return m;

        } catch (Exception e) {
            System.err.println("Mouvement.fromJson error: " + e.getMessage());
            return null;
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"").replace("\n", "");
    }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s x%d (%s) par %s",
            getDateHeureStr(), getTypeLabel(),
            produitNom, quantite, motif, operateur);
    }
}