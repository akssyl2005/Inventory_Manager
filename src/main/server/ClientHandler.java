package main.server;

import main.model.Produit;
import main.model.ProduitStore;
import main.model.Protocol;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ProduitStore store;
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClientHandler(Socket socket, ProduitStore store) {
        this.socket = socket;
        this.store  = store;
    }

    @Override
    public void run() {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String ligne;
            while ((ligne = in.readLine()) != null) {
                System.out.println("[" + now() + "] Reçu : " + ligne);

                if (ligne.equalsIgnoreCase(Protocol.EXIT)) {
                    out.println("Au revoir !");
                    break;
                }

                int sep         = ligne.indexOf(':');
                String commande = sep == -1 ? ligne : ligne.substring(0, sep);
                String data     = sep == -1 ? ""    : ligne.substring(sep + 1);

                switch (commande) {
                    case Protocol.ADD    -> handleAdd(out, data);
                    case Protocol.DELETE -> handleDelete(out, data);
                    case Protocol.UPDATE -> handleUpdate(out, data);
                    case Protocol.LIST   -> handleList(out);
                    default              -> out.println(Protocol.ERROR + ":Commande inconnue.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur client : " + e.getMessage());
        }
        System.out.println("[" + now() + "] Client déconnecté.");
    }

    private void handleAdd(PrintWriter out, String data) {
        Produit p = Produit.fromJson(data);
        if (p == null) { out.println(Protocol.ERROR + ":Données invalides."); return; }
        Produit added = store.ajouter(p);
        System.out.println("  → Ajouté : " + added + " | Stock=" + store.taille());
        out.println(Protocol.OK + ":" + added.toJson());
    }

    private void handleDelete(PrintWriter out, String data) {
        try {
            int id = Integer.parseInt(data.trim());
            if (store.supprimer(id)) {
                System.out.println("  → Supprimé ID=" + id);
                out.println(Protocol.OK + ":Produit ID=" + id + " supprimé.");
            } else {
                out.println(Protocol.ERROR + ":Produit ID=" + id + " introuvable.");
            }
        } catch (NumberFormatException e) {
            out.println(Protocol.ERROR + ":ID invalide.");
        }
    }

    private void handleUpdate(PrintWriter out, String data) {
        Produit maj = Produit.fromJson(data);
        if (maj == null) { out.println(Protocol.ERROR + ":Données invalides."); return; }
        Produit result = store.modifier(maj);
        if (result != null) {
            System.out.println("  → Modifié : " + result);
            out.println(Protocol.OK + ":" + result.toJson());
        } else {
            out.println(Protocol.ERROR + ":Produit ID=" + maj.getId() + " introuvable.");
        }
    }

    private void handleList(PrintWriter out) {
        List<Produit> liste = store.getListe();
        if (liste.isEmpty()) { out.println(Protocol.EMPTY); return; }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < liste.size(); i++) {
            sb.append(liste.get(i).toJson());
            if (i < liste.size() - 1) sb.append(";");
        }
        out.println(sb);
    }

    private String now() {
        return LocalDateTime.now().format(fmt);
    }
}