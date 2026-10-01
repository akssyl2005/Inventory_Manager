package main.rmi;

import main.model.Produit;
import main.model.Mouvement;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface PharmFlowRMI extends Remote {

    // ── Statistiques générales ─────────────────────────────
    Map<String, Object> getStatistiques() throws RemoteException;

    // ── Rapport stock complet ──────────────────────────────
    List<Produit> getRapportStock() throws RemoteException;

    // ── Alertes critiques ──────────────────────────────────
    List<Produit> getAlertesCritiques() throws RemoteException;

    // ── Top 5 médicaments les plus chers ──────────────────
    List<Produit> getTop5PlusCher() throws RemoteException;

    // ── Historique mouvements ──────────────────────────────
    List<Mouvement> getHistoriqueMouvements() throws RemoteException;

    // ── Valeur totale du stock ─────────────────────────────
    double getValeurTotaleStock() throws RemoteException;

    // ── Nombre de médicaments par catégorie ───────────────
    Map<String, Long> getRepartitionCategories() throws RemoteException;

    // ── Ping (test de connexion RMI) ───────────────────────
    String ping() throws RemoteException;
}