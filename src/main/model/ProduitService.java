package main.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService {

    private final PrintWriter    out;
    private final BufferedReader in;

    public ProduitService(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in  = in;
    }

    // ══════════════════════════════════════════════════════════
    // CRUD PRODUITS
    // ══════════════════════════════════════════════════════════

    public Produit ajouter(Produit p) throws IOException {
        out.println(Protocol.ADD + ":" + p.toJson());
        String rep = in.readLine();
        if (rep != null && rep.startsWith(Protocol.OK + ":"))
            return Produit.fromJson(rep.substring(3));
        System.err.println("Erreur ajout : " + rep);
        return null;
    }

    public boolean supprimer(int id) throws IOException {
        out.println(Protocol.DELETE + ":" + id);
        String rep = in.readLine();
        return rep != null && rep.startsWith(Protocol.OK);
    }

    public Produit modifier(Produit p) throws IOException {
        out.println(Protocol.UPDATE + ":" + p.toJson());
        String rep = in.readLine();
        if (rep != null && rep.startsWith(Protocol.OK + ":"))
            return Produit.fromJson(rep.substring(3));
        System.err.println("Erreur modification : " + rep);
        return null;
    }

    public List<Produit> getListe() throws IOException {
        out.println(Protocol.LIST);
        return parseProduits(in.readLine());
    }

    // ══════════════════════════════════════════════════════════
    // MOUVEMENTS DE STOCK
    // ══════════════════════════════════════════════════════════

    public Object[] entreeStock(int produitId, int quantite,
                                 String motif, String operateur) throws IOException {
        out.println(Protocol.ENTREE + ":"
            + produitId + "|" + quantite + "|"
            + motif     + "|" + operateur);
        String rep = in.readLine();
        if (rep != null && rep.startsWith(Protocol.OK + ":")) {
            String body  = rep.substring(3);
            int    split = body.indexOf("|{");
            if (split != -1) {
                Produit   p = Produit.fromJson(body.substring(0, split));
                Mouvement m = Mouvement.fromJson(body.substring(split + 1));
                return new Object[]{p, m};
            }
        }
        System.err.println("Erreur entrée stock : " + rep);
        return null;
    }

    public Object[] sortieStock(int produitId, int quantite,
                                 String motif, String operateur) throws IOException {
        out.println(Protocol.SORTIE + ":"
            + produitId + "|" + quantite + "|"
            + motif     + "|" + operateur);
        String rep = in.readLine();
        if (rep != null && rep.startsWith(Protocol.OK + ":")) {
            String body  = rep.substring(3);
            int    split = body.indexOf("|{");
            if (split != -1) {
                Produit   p = Produit.fromJson(body.substring(0, split));
                Mouvement m = Mouvement.fromJson(body.substring(split + 1));
                return new Object[]{p, m};
            }
        }
        System.err.println("Erreur sortie stock : " + rep);
        return null;
    }

    public List<Mouvement> getListeMouvements() throws IOException {
        out.println(Protocol.LIST_MOUVEMENTS);
        return parseMouvements(in.readLine());
    }

    public List<Mouvement> getListeMouvementsProduit(int id) throws IOException {
        out.println(Protocol.LIST_MOUVEMENTS_PRODUIT + ":" + id);
        return parseMouvements(in.readLine());
    }

    // ══════════════════════════════════════════════════════════
    // ALERTES
    // ══════════════════════════════════════════════════════════

    public List<Produit> getExpires() throws IOException {
        out.println(Protocol.LIST_EXPIRES);
        return parseProduits(in.readLine());
    }

    public List<Produit> getExpireBientot() throws IOException {
        out.println(Protocol.LIST_EXPIRE_BIENTOT);
        return parseProduits(in.readLine());
    }

    public List<Produit> getStockCritique() throws IOException {
        out.println(Protocol.LIST_STOCK_CRITIQUE);
        return parseProduits(in.readLine());
    }

    public List<Produit> getAReapprovisionner() throws IOException {
        out.println(Protocol.LIST_REAPPRO);
        return parseProduits(in.readLine());
    }

    // ══════════════════════════════════════════════════════════
    // RECHERCHES
    // ══════════════════════════════════════════════════════════

    public List<Produit> searchByDci(String dci) throws IOException {
        out.println(Protocol.SEARCH_DCI + ":" + dci);
        return parseProduits(in.readLine());
    }

    public List<Produit> searchByFournisseur(String f) throws IOException {
        out.println(Protocol.SEARCH_FOURNISSEUR + ":" + f);
        return parseProduits(in.readLine());
    }

    public List<Produit> searchByLot(String lot) throws IOException {
        out.println(Protocol.SEARCH_LOT + ":" + lot);
        return parseProduits(in.readLine());
    }

    // ══════════════════════════════════════════════════════════
    // DÉCONNEXION
    // ══════════════════════════════════════════════════════════

    public void quitter() throws IOException {
        out.println(Protocol.EXIT);
        try { String rep = in.readLine();
              if (rep != null) System.out.println("Serveur : " + rep);
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════
    // PARSERS PRIVÉS
    // ══════════════════════════════════════════════════════════

    private List<Produit> parseProduits(String rep) {
        List<Produit> liste = new ArrayList<>();
        if (rep == null || rep.equals(Protocol.EMPTY)) return liste;
        if (rep.startsWith(Protocol.ERROR)) {
            System.err.println(rep); return liste;
        }
        for (String json : rep.split(Protocol.SEP_ITEM)) {
            Produit p = Produit.fromJson(json);
            if (p != null) liste.add(p);
        }
        return liste;
    }

    private List<Mouvement> parseMouvements(String rep) {
        List<Mouvement> liste = new ArrayList<>();
        if (rep == null || rep.equals(Protocol.EMPTY)) return liste;
        if (rep.startsWith(Protocol.ERROR)) {
            System.err.println(rep); return liste;
        }
        for (String json : rep.split(Protocol.SEP_ITEM)) {
            Mouvement m = Mouvement.fromJson(json);
            if (m != null) liste.add(m);
        }
        return liste;
    }
}