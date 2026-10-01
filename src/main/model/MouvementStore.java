package main.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MouvementStore {

    private final List<Mouvement> mouvements = new ArrayList<>();
    private final AtomicInteger   compteur   = new AtomicInteger(1);

    // ══════════════════════════════════════════════════════════
    // AJOUT DE MOUVEMENT
    // ══════════════════════════════════════════════════════════

    public Mouvement enregistrer(int produitId, String produitNom,
                                  Mouvement.Type type, int quantite,
                                  String motif, String operateur) {
        Mouvement m = new Mouvement(
            compteur.getAndIncrement(),
            produitId, produitNom,
            type, quantite, motif, operateur
        );
        synchronized (mouvements) {
            mouvements.add(m);
        }
        return m;
    }

    // ══════════════════════════════════════════════════════════
    // RÉCUPÉRATION
    // ══════════════════════════════════════════════════════════

    public List<Mouvement> getTous() {
        synchronized (mouvements) {
            return new ArrayList<>(mouvements);
        }
    }

    public List<Mouvement> getByProduit(int produitId) {
        synchronized (mouvements) {
            return mouvements.stream()
                .filter(m -> m.getProduitId() == produitId)
                .collect(Collectors.toList());
        }
    }

    public List<Mouvement> getEntrees() {
        synchronized (mouvements) {
            return mouvements.stream()
                .filter(m -> m.getType() == Mouvement.Type.ENTREE)
                .collect(Collectors.toList());
        }
    }

    public List<Mouvement> getSorties() {
        synchronized (mouvements) {
            return mouvements.stream()
                .filter(m -> m.getType() == Mouvement.Type.SORTIE)
                .collect(Collectors.toList());
        }
    }

    public List<Mouvement> getByPeriode(LocalDateTime debut, LocalDateTime fin) {
        synchronized (mouvements) {
            return mouvements.stream()
                .filter(m -> m.getDateHeure() != null
                    && !m.getDateHeure().isBefore(debut)
                    && !m.getDateHeure().isAfter(fin))
                .collect(Collectors.toList());
        }
    }

    public List<Mouvement> getDerniers(int n) {
        synchronized (mouvements) {
            int size = mouvements.size();
            if (size <= n) return new ArrayList<>(mouvements);
            return new ArrayList<>(mouvements.subList(size - n, size));
        }
    }

    // ══════════════════════════════════════════════════════════
    // STATISTIQUES
    // ══════════════════════════════════════════════════════════

    public int getTotalEntrees() {
        synchronized (mouvements) {
            return mouvements.stream()
                .filter(m -> m.getType() == Mouvement.Type.ENTREE)
                .mapToInt(Mouvement::getQuantite)
                .sum();
        }
    }

    public int getTotalSorties() {
        synchronized (mouvements) {
            return mouvements.stream()
                .filter(m -> m.getType() == Mouvement.Type.SORTIE)
                .mapToInt(Mouvement::getQuantite)
                .sum();
        }
    }

    public int getNombreMouvements() {
        synchronized (mouvements) { return mouvements.size(); }
    }

    public String serialiserListe(List<Mouvement> liste) {
        if (liste.isEmpty()) return Protocol.EMPTY;
        return liste.stream()
            .map(Mouvement::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM));
    }
}