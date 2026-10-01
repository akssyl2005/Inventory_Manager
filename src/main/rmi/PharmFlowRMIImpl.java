package main.rmi;

import main.model.Mouvement;
import main.model.MouvementStore;
import main.model.Produit;
import main.model.ProduitStore;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Collectors;

public class PharmFlowRMIImpl
        extends UnicastRemoteObject
        implements PharmFlowRMI {

    private final ProduitStore   produitStore;
    private final MouvementStore mouvementStore;

    public PharmFlowRMIImpl(ProduitStore ps, MouvementStore ms)
            throws RemoteException {
        super();
        this.produitStore   = ps;
        this.mouvementStore = ms;
    }

    // ── Ping ───────────────────────────────────────────────
    @Override
    public String ping() throws RemoteException {
        return "✔ PharmFlow RMI opérationnel — "
            + new java.util.Date();
    }

    // ── Statistiques générales ─────────────────────────────
    @Override
    public Map<String, Object> getStatistiques()
            throws RemoteException {
        List<Produit> liste = produitStore.getListe();
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalMedicaments", liste.size());
        stats.put("valeurTotale",
            liste.stream()
                .mapToDouble(p -> p.getPrix() * p.getQuantite())
                .sum());
        stats.put("stockCritique",
            liste.stream()
                .filter(Produit::stockCritique)
                .count());
        stats.put("expires",
            liste.stream()
                .filter(Produit::estExpire)
                .count());
        stats.put("expireBientot",
            liste.stream()
                .filter(Produit::expireBientot)
                .count());
        stats.put("totalMouvements",
            mouvementStore.getTous().size());
        stats.put("totalEntrees",
            mouvementStore.getTous().stream()
                .filter(m -> m.getType() == Mouvement.Type.ENTREE)
                .mapToInt(Mouvement::getQuantite).sum());
        stats.put("totalSorties",
            mouvementStore.getTous().stream()
                .filter(m -> m.getType() == Mouvement.Type.SORTIE)
                .mapToInt(Mouvement::getQuantite).sum());
        stats.put("categories",
            liste.stream()
                .map(Produit::getCategorie)
                .distinct().count());
        stats.put("prixMoyen",
            liste.stream()
                .mapToDouble(Produit::getPrix)
                .average().orElse(0));

        return stats;
    }

    // ── Rapport stock complet ──────────────────────────────
    @Override
    public List<Produit> getRapportStock()
            throws RemoteException {
        return new ArrayList<>(produitStore.getListe());
    }

    // ── Alertes critiques ──────────────────────────────────
    @Override
    public List<Produit> getAlertesCritiques()
            throws RemoteException {
        return produitStore.getListe().stream()
            .filter(p -> p.stockCritique()
                      || p.estExpire()
                      || p.expireBientot())
            .collect(Collectors.toList());
    }

    // ── Top 5 plus chers ───────────────────────────────────
    @Override
    public List<Produit> getTop5PlusCher()
            throws RemoteException {
        return produitStore.getListe().stream()
            .sorted(Comparator
                .comparingDouble(Produit::getPrix)
                .reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    // ── Historique mouvements ──────────────────────────────
    @Override
    public List<Mouvement> getHistoriqueMouvements()
            throws RemoteException {
        return new ArrayList<>(mouvementStore.getTous());
    }

    // ── Valeur totale ──────────────────────────────────────
    @Override
    public double getValeurTotaleStock()
            throws RemoteException {
        return produitStore.getListe().stream()
            .mapToDouble(p -> p.getPrix() * p.getQuantite())
            .sum();
    }

    // ── Répartition par catégorie ──────────────────────────
    @Override
    public Map<String, Long> getRepartitionCategories()
            throws RemoteException {
        return produitStore.getListe().stream()
            .collect(Collectors.groupingBy(
                Produit::getCategorie,
                Collectors.counting()));
    }
}