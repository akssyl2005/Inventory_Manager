package main.server;

import main.model.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportClientHandler implements Runnable {

    private final Socket         socket;
    private final ProduitStore   produitStore;
    private final MouvementStore mouvementStore;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Commandes spéciales du serveur de rapports ─────────
    private static final String STATS      = "STATS";
    private static final String TOP5       = "TOP5";
    private static final String ALERTES    = "ALERTES";
    private static final String RAPPORT    = "RAPPORT";
    private static final String VALEUR     = "VALEUR";

    public ReportClientHandler(Socket socket,
            ProduitStore ps, MouvementStore ms) {
        this.socket         = socket;
        this.produitStore   = ps;
        this.mouvementStore = ms;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)
        ) {
            String ligne;
            while ((ligne = in.readLine()) != null) {
                log("Reçu : " + ligne);

                if (ligne.equalsIgnoreCase("exit")) {
                    out.println("Au revoir !");
                    break;
                }

                switch (ligne.trim()) {
                    case STATS   -> handleStats(out);
                    case TOP5    -> handleTop5(out);
                    case ALERTES -> handleAlertes(out);
                    case RAPPORT -> handleRapport(out);
                    case VALEUR  -> handleValeur(out);
                    default      -> out.println(
                        "ERROR:Commande inconnue : " + ligne);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur rapport client : "
                + e.getMessage());
        }
        log("Client rapport déconnecté.");
    }

    // ── Statistiques générales ─────────────────────────────
    private void handleStats(PrintWriter out) {
        List<Produit> liste = produitStore.getListe();
        long critique = liste.stream()
            .filter(Produit::stockCritique).count();
        long expires  = liste.stream()
            .filter(Produit::estExpire).count();
        double valeur = liste.stream()
            .mapToDouble(p -> p.getPrix() * p.getQuantite())
            .sum();
        int entrees = mouvementStore.getTous().stream()
            .filter(m -> m.getType() == Mouvement.Type.ENTREE)
            .mapToInt(Mouvement::getQuantite).sum();
        int sorties = mouvementStore.getTous().stream()
            .filter(m -> m.getType() == Mouvement.Type.SORTIE)
            .mapToInt(Mouvement::getQuantite).sum();

        out.println("OK:total=" + liste.size()
            + "|critique=" + critique
            + "|expires="  + expires
            + "|valeur="   + String.format("%.2f", valeur)
            + "|entrees="  + entrees
            + "|sorties="  + sorties
            + "|mouvements=" + mouvementStore.getTous().size());

        log("Stats envoyées — " + liste.size()
            + " médicaments");
    }

    // ── Top 5 plus chers ───────────────────────────────────
    private void handleTop5(PrintWriter out) {
        List<Produit> top5 = produitStore.getListe().stream()
            .sorted((a, b) -> Double.compare(
                b.getPrix(), a.getPrix()))
            .limit(5)
            .collect(Collectors.toList());

        if (top5.isEmpty()) {
            out.println("EMPTY");
            return;
        }
        out.println("OK:" + top5.stream()
            .map(Produit::toJson)
            .collect(Collectors.joining(";")));

        log("Top5 envoyé");
    }

    // ── Alertes critiques ──────────────────────────────────
    private void handleAlertes(PrintWriter out) {
        List<Produit> alertes = produitStore.getListe()
            .stream()
            .filter(p -> p.stockCritique()
                      || p.estExpire()
                      || p.expireBientot())
            .collect(Collectors.toList());

        if (alertes.isEmpty()) {
            out.println("EMPTY");
            return;
        }
        out.println("OK:" + alertes.stream()
            .map(Produit::toJson)
            .collect(Collectors.joining(";")));

        log("Alertes envoyées — " + alertes.size());
    }

    // ── Rapport complet ────────────────────────────────────
    private void handleRapport(PrintWriter out) {
        List<Produit> liste = produitStore.getListe();
        if (liste.isEmpty()) {
            out.println("EMPTY");
            return;
        }
        out.println("OK:" + liste.stream()
            .map(Produit::toJson)
            .collect(Collectors.joining(";")));

        log("Rapport complet envoyé — "
            + liste.size() + " médicaments");
    }

    // ── Valeur totale ──────────────────────────────────────
    private void handleValeur(PrintWriter out) {
        double valeur = produitStore.getListe().stream()
            .mapToDouble(p -> p.getPrix() * p.getQuantite())
            .sum();
        out.println("OK:" + String.format("%.2f", valeur));
        log("Valeur totale envoyée : "
            + String.format("%.2f DA", valeur));
    }

    private void log(String msg) {
        System.out.println("[REPORT-Handler "
            + LocalDateTime.now().format(FMT)
            + "] " + msg);
    }
}