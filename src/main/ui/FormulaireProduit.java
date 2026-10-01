package main.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import main.model.Produit;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static main.ui.MainApp.*;

public class FormulaireProduit extends Stage {

    public interface Callback { void onValider(Produit p); }

    public FormulaireProduit(Produit existant, Callback cb) {
        boolean isEdit = existant != null;
        setTitle(isEdit ? "✎  Modifier le médicament" : "＋  Nouveau médicament");
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:" + BG_CARD + ";");

        // ── Header ─────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

        StackPane ico = new StackPane();
        Rectangle icoBg = new Rectangle(42, 42);
        icoBg.setArcWidth(12); icoBg.setArcHeight(12);
        icoBg.setFill(Color.web(isEdit ? ACCENT2 + "30" : ACCENT + "30"));
        Label icoLbl = new Label(isEdit ? "✎" : "💊");
        icoLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        icoLbl.setTextFill(Color.web(isEdit ? ACCENT2 : ACCENT));
        ico.getChildren().addAll(icoBg, icoLbl);

        VBox hText = new VBox(3);
        Label htitle = new Label(isEdit ? "Modifier le médicament" : "Ajouter un médicament");
        htitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        htitle.setTextFill(Color.web(TEXT));
        Label hsub = new Label(isEdit
            ? "Modifiez les champs et validez"
            : "Remplissez tous les champs obligatoires (*)");
        hsub.setFont(Font.font("Segoe UI", 11));
        hsub.setTextFill(Color.web(TEXT_SUB));
        hText.getChildren().addAll(htitle, hsub);
        header.getChildren().addAll(ico, hText);

        // ── Formulaire en deux colonnes ─────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.setStyle("-fx-background-color:" + BG_CARD + ";");

        // Colonne gauche
        TextField tfNom   = fChamp(existant != null ? existant.getNom()       : "", "Ex: Paracétamol 500mg");
        TextField tfDci   = fChamp(existant != null ? existant.getDci()       : "", "Ex: Paracétamol");
        TextField tfCat   = fChamp(existant != null ? existant.getCategorie() : "", "Ex: Analgésique");
        TextField tfFourn = fChamp(existant != null ? existant.getFournisseur(): "", "Ex: Sanofi");

        grid.add(fLabel("💊  Médicament *"),  0, 0); grid.add(tfNom,   1, 0);
        grid.add(fLabel("🔬  DCI"),            0, 1); grid.add(tfDci,   1, 1);
        grid.add(fLabel("🏷  Catégorie *"),    0, 2); grid.add(tfCat,   1, 2);
        grid.add(fLabel("🏭  Fournisseur"),    0, 3); grid.add(tfFourn, 1, 3);

        // Colonne droite
        TextField tfPrix  = fChamp(existant != null ? String.valueOf(existant.getPrix())        : "", "Ex: 250.00");
        TextField tfQte   = fChamp(existant != null ? String.valueOf(existant.getQuantite())    : "", "Ex: 100");
        TextField tfLot   = fChamp(existant != null ? existant.getNumeroLot()                   : "", "Ex: LOT2024-001");
        TextField tfSeuil = fChamp(existant != null ? String.valueOf(existant.getSeuilReappro()): "", "Ex: 10");

        // Date expiration
        DatePicker dpExp = new DatePicker();
        dpExp.setPrefWidth(220);
        dpExp.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:8;-fx-background-radius:8;"
        );
        if (existant != null && existant.getDateExpiration() != null)
            dpExp.setValue(existant.getDateExpiration());

        grid.add(fLabel("💰  Prix (DA) *"),   2, 0); grid.add(tfPrix,  3, 0);
        grid.add(fLabel("📦  Quantité *"),    2, 1); grid.add(tfQte,   3, 1);
        grid.add(fLabel("📅  Expiration"),    2, 2); grid.add(dpExp,   3, 2);
        grid.add(fLabel("🔢  N° Lot"),        2, 3); grid.add(tfLot,   3, 3);
        grid.add(fLabel("⚠  Seuil réappro"), 2, 4); grid.add(tfSeuil, 3, 4);

        // ── Indicateur stock temps réel ─────────────────────────
        Label stockInd = new Label("");
        stockInd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        tfQte.textProperty().addListener((o, old, v) -> {
            try {
                int q = Integer.parseInt(v.trim());
                int seuil = 10;
                try { seuil = Integer.parseInt(tfSeuil.getText().trim()); } catch (Exception ignored) {}
                if (q == 0)       { stockInd.setText("🔴  Rupture de stock !");  stockInd.setTextFill(Color.web(RED)); }
                else if (q < seuil){ stockInd.setText("⚡  Stock sous le seuil"); stockInd.setTextFill(Color.web(ORANGE)); }
                else              { stockInd.setText("✔  Stock suffisant");       stockInd.setTextFill(Color.web(GREEN)); }
            } catch (Exception ex) { stockInd.setText(""); }
        });
        grid.add(stockInd, 1, 4, 1, 1);

        // ── Label erreur ────────────────────────────────────────
        Label errLbl = new Label("");
        errLbl.setFont(Font.font("Segoe UI", 11));
        errLbl.setTextFill(Color.web(RED));
        errLbl.setPadding(new Insets(0, 24, 4, 24));
        errLbl.setWrapText(true);

        // ── Boutons ─────────────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(8, 24, 20, 24));
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setFont(Font.font("Segoe UI", 13));
        btnAnnuler.setPadding(new Insets(9, 22, 9, 22));
        btnAnnuler.setCursor(javafx.scene.Cursor.HAND);
        btnAnnuler.setStyle(
            "-fx-background-color:#12122a;-fx-text-fill:" + TEXT_SUB + ";" +
            "-fx-background-radius:8;-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-border-width:1;"
        );
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
            "-fx-background-color:#1a1a3a;-fx-text-fill:" + TEXT + ";" +
            "-fx-background-radius:8;-fx-border-color:" + TEXT_SUB + ";-fx-border-radius:8;-fx-border-width:1;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(
            "-fx-background-color:#12122a;-fx-text-fill:" + TEXT_SUB + ";" +
            "-fx-background-radius:8;-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-border-width:1;"));
        btnAnnuler.setOnAction(e -> close());

        String btnColor = isEdit ? ACCENT2 : ACCENT;
        Button btnOk = new Button(isEdit ? "✔  Enregistrer" : "✔  Ajouter");
        btnOk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnOk.setPadding(new Insets(9, 24, 9, 24));
        btnOk.setCursor(javafx.scene.Cursor.HAND);
        btnOk.setDefaultButton(true);
        btnOk.setStyle("-fx-background-color:" + btnColor + ";-fx-text-fill:white;-fx-background-radius:8;");
        btnOk.setOnMouseEntered(e -> btnOk.setStyle("-fx-background-color:" + btnColor + "cc;-fx-text-fill:white;-fx-background-radius:8;"));
        btnOk.setOnMouseExited (e -> btnOk.setStyle("-fx-background-color:" + btnColor + ";-fx-text-fill:white;-fx-background-radius:8;"));

        // ── Validation ──────────────────────────────────────────
        btnOk.setOnAction(e -> {
            try {
                errLbl.setText("");

                String nom = tfNom.getText().trim();
                if (nom.isEmpty()) throw new Exception("Le nom du médicament est obligatoire");

                String cat = tfCat.getText().trim();
                if (cat.isEmpty()) throw new Exception("La catégorie est obligatoire");

                double prix;
                try { prix = Double.parseDouble(tfPrix.getText().trim()); }
                catch (NumberFormatException ex) { throw new Exception("Prix invalide (ex: 250.00)"); }
                if (prix < 0) throw new Exception("Le prix doit être ≥ 0");

                int qte;
                try { qte = Integer.parseInt(tfQte.getText().trim()); }
                catch (NumberFormatException ex) { throw new Exception("Quantité invalide (ex: 100)"); }
                if (qte < 0) throw new Exception("La quantité doit être ≥ 0");

                int seuil = 10;
                if (!tfSeuil.getText().trim().isEmpty()) {
                    try { seuil = Integer.parseInt(tfSeuil.getText().trim()); }
                    catch (NumberFormatException ex) { throw new Exception("Seuil invalide (ex: 10)"); }
                }

                String    dci    = tfDci.getText().trim();
                String    fourn  = tfFourn.getText().trim();
                String    lot    = tfLot.getText().trim();
                LocalDate expir  = dpExp.getValue();

                cb.onValider(new Produit(0, nom, prix, qte, cat,
                    dci, fourn, lot, expir, seuil));
                close();

            } catch (Exception ex) {
                errLbl.setText("⚠  " + ex.getMessage());
                TranslateTransition shake = new TranslateTransition(Duration.millis(55), root);
                shake.setFromX(0); shake.setByX(9);
                shake.setAutoReverse(true); shake.setCycleCount(4);
                shake.play();
            }
        });

        actions.getChildren().addAll(btnAnnuler, btnOk);
        root.getChildren().addAll(header, grid, errLbl, actions);
        setScene(new Scene(root, 720, 380));
    }

    // ── Helpers ─────────────────────────────────────────────────
    private static Label fLabel(String txt) {
        Label l = new Label(txt);
        l.setFont(Font.font("Segoe UI", 12));
        l.setTextFill(Color.web(TEXT_SUB));
        l.setMinWidth(130);
        return l;
    }

    private static TextField fChamp(String val, String prompt) {
        TextField tf = new TextField(val);
        tf.setPromptText(prompt);
        tf.setPrefWidth(220);
        tf.setFont(Font.font("Segoe UI", 13));
        String base =
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-text-fill:" + TEXT + ";" +
            "-fx-prompt-text-fill:" + TEXT_SUB + ";" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-padding:7 12;";
        tf.setStyle(base + "-fx-border-color:" + BORDER + ";");
        tf.focusedProperty().addListener((o, old, f) ->
            tf.setStyle(base + (f
                ? "-fx-border-color:" + ACCENT + ";"
                : "-fx-border-color:" + BORDER + ";")));
        return tf;
    }
}