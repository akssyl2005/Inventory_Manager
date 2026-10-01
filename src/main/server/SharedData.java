package main.server;

import main.model.MouvementStore;
import main.model.ProduitStore;

public class SharedData {

    // ── Instances partagées entre les 2 serveurs ───────────
    private static final ProduitStore   produitStore
        = new ProduitStore();
    private static final MouvementStore mouvementStore
        = new MouvementStore();

    public static ProduitStore   getProduitStore()
        { return produitStore; }
    public static MouvementStore getMouvementStore()
        { return mouvementStore; }
}