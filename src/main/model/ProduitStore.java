package main.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProduitStore {

    private final List<Produit>   stock    = new ArrayList<>();
    private final AtomicInteger   compteur = new AtomicInteger(1);

    // ══════════════════════════════════════════════════════════
    // CRUD DE BASE
    // ══════════════════════════════════════════════════════════

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

    public Produit findById(int id) {
        synchronized (stock) {
            return stock.stream()
                .filter(p -> p.getId() == id)
                .findFirst().orElse(null);
        }
    }

    public List<Produit> getListe() {
        synchronized (stock) { return new ArrayList<>(stock); }
    }

    public int taille() {
        synchronized (stock) { return stock.size(); }
    }

    // ══════════════════════════════════════════════════════════
    // MOUVEMENTS DE STOCK
    // ══════════════════════════════════════════════════════════

    public Produit entreeStock(int id, int quantite) {
        synchronized (stock) {
            Produit p = findById(id);
            if (p == null) return null;
            p.setQuantite(p.getQuantite() + quantite);
            return p;
        }
    }

    public Produit sortieStock(int id, int quantite) {
        synchronized (stock) {
            Produit p = findById(id);
            if (p == null) return null;
            if (p.getQuantite() < quantite) return null;
            p.setQuantite(p.getQuantite() - quantite);
            return p;
        }
    }

    // ══════════════════════════════════════════════════════════
    // ALERTES & RECHERCHES
    // ══════════════════════════════════════════════════════════

    public List<Produit> getExpires() {
        synchronized (stock) {
            return stock.stream()
                .filter(Produit::estExpire)
                .collect(Collectors.toList());
        }
    }

    public List<Produit> getExpireBientot() {
        synchronized (stock) {
            return stock.stream()
                .filter(Produit::expireBientot)
                .collect(Collectors.toList());
        }
    }

    public List<Produit> getStockCritique() {
        synchronized (stock) {
            return stock.stream()
                .filter(Produit::stockCritique)
                .collect(Collectors.toList());
        }
    }

    public List<Produit> getAReapprovisionner() {
        synchronized (stock) {
            return stock.stream()
                .filter(p -> p.getQuantite() <= p.getSeuilReappro())
                .collect(Collectors.toList());
        }
    }

    public List<Produit> searchByDci(String dci) {
        synchronized (stock) {
            return stock.stream()
                .filter(p -> p.getDci().toLowerCase()
                    .contains(dci.toLowerCase()))
                .collect(Collectors.toList());
        }
    }

    public List<Produit> searchByFournisseur(String fournisseur) {
        synchronized (stock) {
            return stock.stream()
                .filter(p -> p.getFournisseur().toLowerCase()
                    .contains(fournisseur.toLowerCase()))
                .collect(Collectors.toList());
        }
    }

    public List<Produit> searchByLot(String lot) {
        synchronized (stock) {
            return stock.stream()
                .filter(p -> p.getNumeroLot().toLowerCase()
                    .contains(lot.toLowerCase()))
                .collect(Collectors.toList());
        }
    }

    // ══════════════════════════════════════════════════════════
    // STATISTIQUES
    // ══════════════════════════════════════════════════════════

    public double getValeurTotaleStock() {
        synchronized (stock) {
            return stock.stream()
                .mapToDouble(p -> p.getPrix() * p.getQuantite())
                .sum();
        }
    }

    public long getNombreExpiresDans(int jours) {
        synchronized (stock) {
            LocalDate limite = LocalDate.now().plusDays(jours);
            return stock.stream()
                .filter(p -> p.getDateExpiration() != null
                    && p.getDateExpiration().isBefore(limite)
                    && !p.estExpire())
                .count();
        }
    }

    public long getNombreStockCritique() {
        synchronized (stock) {
            return stock.stream().filter(Produit::stockCritique).count();
        }
    }
}