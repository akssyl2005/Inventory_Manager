package main.model;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProduitStore {
    private final List<Produit> stock = new ArrayList<>();
    private final AtomicInteger compteur = new AtomicInteger(1);

    public Produit ajouter(Produit p) {
        p.setId(compteur.getAndIncrement());
        synchronized (stock) { stock.add(p); }
        return p;
    }

    public boolean supprimer(int id) {
        synchronized (stock) {
            return stock.removeIf(p -> p.getId() == id);
        }
    }

    public Produit modifier(Produit maj) {
        synchronized (stock) {
            for (int i = 0; i < stock.size(); i++) {
                if (stock.get(i).getId() == maj.getId()) {
                    stock.set(i, maj);
                    return maj;
                }
            }
        }
        return null;
    }

    public List<Produit> getListe() {
        synchronized (stock) {
            return new ArrayList<>(stock);
        }
    }

    public int taille() {
        synchronized (stock) { return stock.size(); }
    }
}