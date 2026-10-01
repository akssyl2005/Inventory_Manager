package main.server;

import main.model.*;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket                socket;
    private final ProduitStore          produitStore;
    private final MouvementStore        mouvementStore;
    private final UDPNotificationServer udpServer;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClientHandler(Socket socket, ProduitStore ps,
                         MouvementStore ms,
                         UDPNotificationServer udp) {
        this.socket         = socket;
        this.produitStore   = ps;
        this.mouvementStore = ms;
        this.udpServer      = udp;
    }

    @Override
    public void run() {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(
                socket.getOutputStream(), true)
        ) {
            String ligne;
            while ((ligne = in.readLine()) != null) {
                log("Reçu : " + ligne);

                if (ligne.equalsIgnoreCase(Protocol.EXIT)) {
                    out.println("Au revoir !");
                    break;
                }

                int    sep      = ligne.indexOf(':');
                String commande = sep == -1
                    ? ligne.trim()
                    : ligne.substring(0, sep).trim();
                String data     = sep == -1
                    ? ""
                    : ligne.substring(sep + 1).trim();

                switch (commande) {
                    // ── Produits ───────────────────────────
                    case Protocol.ADD    -> handleAdd(out, data);
                    case Protocol.DELETE -> handleDelete(out, data);
                    case Protocol.UPDATE -> handleUpdate(out, data);
                    case Protocol.LIST   -> handleList(out);

                    // ── Mouvements ─────────────────────────
                    case Protocol.ENTREE ->
                        handleEntree(out, data);
                    case Protocol.SORTIE ->
                        handleSortie(out, data);
                    case Protocol.LIST_MOUVEMENTS ->
                        handleListMouvements(out);
                    case Protocol.LIST_MOUVEMENTS_PRODUIT ->
                        handleListMouvementsProduit(out, data);

                    // ── Alertes ────────────────────────────
                    case Protocol.LIST_EXPIRES ->
                        handleListExpires(out);
                    case Protocol.LIST_EXPIRE_BIENTOT ->
                        handleListExpireBientot(out);
                    case Protocol.LIST_STOCK_CRITIQUE ->
                        handleListStockCritique(out);
                    case Protocol.LIST_REAPPRO ->
                        handleListReappro(out);

                    // ── Recherches ─────────────────────────
                    case Protocol.SEARCH_DCI ->
                        handleSearchDci(out, data);
                    case Protocol.SEARCH_FOURNISSEUR ->
                        handleSearchFournisseur(out, data);
                    case Protocol.SEARCH_LOT ->
                        handleSearchLot(out, data);

                    default -> out.println(
                        Protocol.ERROR + ":Commande inconnue : "
                        + commande);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur client : " + e.getMessage());
        }
        log("Client déconnecté : " + socket.getInetAddress());
    }

    // ══════════════════════════════════════════════════════
    // HANDLERS PRODUITS + NOTIFICATIONS UDP
    // ══════════════════════════════════════════════════════
    private void handleAdd(PrintWriter out, String data) {
        Produit p = Produit.fromJson(data);
        if (p == null) {
            out.println(Protocol.ERROR + ":Données invalides.");
            return;
        }
        Produit added = produitStore.ajouter(p);
        log("Ajouté : " + added);

        // Mouvement initial
        mouvementStore.enregistrer(
            added.getId(), added.getNom(),
            Mouvement.Type.ENTREE, added.getQuantite(),
            "Ajout initial", "Système"
        );

        // ── Notification UDP ───────────────────────────────
        udpServer.notifierAjout(added.getNom(), added.getQuantite());

        // Vérifier si stock critique après ajout
        if (added.stockCritique()) {
            udpServer.notifierStockCritique(
                added.getNom(),
                added.getQuantite(),
                added.getSeuilReappro()
            );
        }

        // Vérifier expiration
        if (added.estExpire()) {
            udpServer.envoyerBroadcast("ALERTE",
                "EXPIRÉ|" + added.getNom());
        } else if (added.expireBientot()) {
            udpServer.notifierExpiration(
                added.getNom(),
                added.joursAvantExpiration()
            );
        }

        out.println(Protocol.OK + ":" + added.toJson());
    }

    private void handleDelete(PrintWriter out, String data) {
        try {
            int id = Integer.parseInt(data.trim());
            Produit p = produitStore.findById(id);
            if (p == null) {
                out.println(Protocol.ERROR
                    + ":Produit ID=" + id + " introuvable.");
                return;
            }

            mouvementStore.enregistrer(
                id, p.getNom(),
                Mouvement.Type.SORTIE, p.getQuantite(),
                "Suppression produit", "Système"
            );
            produitStore.supprimer(id);
            log("Supprimé ID=" + id);

            // ── Notification UDP ───────────────────────────
            udpServer.notifierSuppression(p.getNom());

            out.println(Protocol.OK
                + ":Médicament ID=" + id + " supprimé.");
        } catch (NumberFormatException e) {
            out.println(Protocol.ERROR + ":ID invalide.");
        }
    }

    private void handleUpdate(PrintWriter out, String data) {
        Produit maj = Produit.fromJson(data);
        if (maj == null) {
            out.println(Protocol.ERROR + ":Données invalides.");
            return;
        }
        Produit result = produitStore.modifier(maj);
        if (result != null) {
            log("Modifié : " + result);

            // ── Notification UDP ───────────────────────────
            udpServer.notifierModification(result.getNom());

            // Vérifier alertes après modification
            if (result.stockCritique()) {
                udpServer.notifierStockCritique(
                    result.getNom(),
                    result.getQuantite(),
                    result.getSeuilReappro()
                );
            }
            if (result.expireBientot()) {
                udpServer.notifierExpiration(
                    result.getNom(),
                    result.joursAvantExpiration()
                );
            }

            out.println(Protocol.OK + ":" + result.toJson());
        } else {
            out.println(Protocol.ERROR
                + ":Produit ID=" + maj.getId() + " introuvable.");
        }
    }

    private void handleList(PrintWriter out) {
        List<Produit> liste = produitStore.getListe();
        if (liste.isEmpty()) {
            out.println(Protocol.EMPTY);
            return;
        }
        out.println(liste.stream()
            .map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    // ══════════════════════════════════════════════════════
    // HANDLERS MOUVEMENTS + NOTIFICATIONS UDP
    // ══════════════════════════════════════════════════════
    private void handleEntree(PrintWriter out, String data) {
        try {
            String[] parts    = data.split("\\|");
            int    produitId  = Integer.parseInt(parts[0].trim());
            int    quantite   = Integer.parseInt(parts[1].trim());
            String motif      = parts.length > 2
                ? parts[2].trim() : "Entrée stock";
            String operateur  = parts.length > 3
                ? parts[3].trim() : "Pharmacien";

            Produit p = produitStore.entreeStock(produitId, quantite);
            if (p == null) {
                out.println(Protocol.ERROR
                    + ":Produit ID=" + produitId + " introuvable.");
                return;
            }

            Mouvement m = mouvementStore.enregistrer(
                produitId, p.getNom(),
                Mouvement.Type.ENTREE, quantite, motif, operateur
            );
            log("Entrée stock : " + p.getNom() + " +" + quantite);

            // ── Notification UDP ───────────────────────────
            udpServer.notifierMouvement(
                "ENTREE", p.getNom(), quantite);

            out.println(Protocol.OK + ":"
                + p.toJson() + "|" + m.toJson());
        } catch (Exception e) {
            out.println(Protocol.ERROR
                + ":Format invalide. Attendu: "
                + "id|quantite|motif|operateur");
        }
    }

    private void handleSortie(PrintWriter out, String data) {
        try {
            String[] parts    = data.split("\\|");
            int    produitId  = Integer.parseInt(parts[0].trim());
            int    quantite   = Integer.parseInt(parts[1].trim());
            String motif      = parts.length > 2
                ? parts[2].trim() : "Sortie stock";
            String operateur  = parts.length > 3
                ? parts[3].trim() : "Pharmacien";

            Produit p = produitStore.sortieStock(produitId, quantite);
            if (p == null) {
                out.println(Protocol.ERROR
                    + ":Stock insuffisant ou produit introuvable.");
                return;
            }

            Mouvement m = mouvementStore.enregistrer(
                produitId, p.getNom(),
                Mouvement.Type.SORTIE, quantite, motif, operateur
            );
            log("Sortie stock : " + p.getNom() + " -" + quantite);

            // ── Notification UDP ───────────────────────────
            udpServer.notifierMouvement(
                "SORTIE", p.getNom(), quantite);

            // Vérifier stock critique après sortie
            if (p.stockCritique()) {
                udpServer.notifierStockCritique(
                    p.getNom(),
                    p.getQuantite(),
                    p.getSeuilReappro()
                );
            }

            out.println(Protocol.OK + ":"
                + p.toJson() + "|" + m.toJson());
        } catch (Exception e) {
            out.println(Protocol.ERROR
                + ":Format invalide. Attendu: "
                + "id|quantite|motif|operateur");
        }
    }

    private void handleListMouvements(PrintWriter out) {
        List<Mouvement> liste = mouvementStore.getTous();
        out.println(mouvementStore.serialiserListe(liste));
    }

    private void handleListMouvementsProduit(
            PrintWriter out, String data) {
        try {
            int id = Integer.parseInt(data.trim());
            List<Mouvement> liste =
                mouvementStore.getByProduit(id);
            out.println(mouvementStore.serialiserListe(liste));
        } catch (NumberFormatException e) {
            out.println(Protocol.ERROR + ":ID invalide.");
        }
    }

    // ══════════════════════════════════════════════════════
    // HANDLERS ALERTES
    // ══════════════════════════════════════════════════════
    private void handleListExpires(PrintWriter out) {
        List<Produit> liste = produitStore.getExpires();
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    private void handleListExpireBientot(PrintWriter out) {
        List<Produit> liste = produitStore.getExpireBientot();
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    private void handleListStockCritique(PrintWriter out) {
        List<Produit> liste = produitStore.getStockCritique();
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    private void handleListReappro(PrintWriter out) {
        List<Produit> liste = produitStore.getAReapprovisionner();
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    // ══════════════════════════════════════════════════════
    // HANDLERS RECHERCHES
    // ══════════════════════════════════════════════════════
    private void handleSearchDci(PrintWriter out, String data) {
        List<Produit> liste = produitStore.searchByDci(data);
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    private void handleSearchFournisseur(
            PrintWriter out, String data) {
        List<Produit> liste =
            produitStore.searchByFournisseur(data);
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    private void handleSearchLot(PrintWriter out, String data) {
        List<Produit> liste = produitStore.searchByLot(data);
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        out.println(liste.stream().map(Produit::toJson)
            .collect(Collectors.joining(Protocol.SEP_ITEM)));
    }

    // ══════════════════════════════════════════════════════
    // UTILITAIRE
    // ══════════════════════════════════════════════════════
    private void log(String msg) {
        System.out.println("[" + LocalDateTime.now().format(FMT)
            + "] " + msg);
    }
}