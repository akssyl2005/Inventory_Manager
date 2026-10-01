package main.ui;

import javafx.collections.ObservableList;
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

import main.model.Mouvement;
import main.model.Produit;

import static main.ui.MainApp.*;

public class FormulaireMouvement extends Stage {

    public interface Callback {
        void onValider(int produitId, int quantite, String motif, String operateur);
    }

    public FormulaireMouvement(Mouvement.Type type,
                                Produit preselect,
                                ObservableList<Produit> produits,
                                Callback cb) {
        boolean isEntree = type == Mouvement.Type.ENTREE;
        String  color    = isEntree ? GREEN : ORANGE;
        String  icon     = isEntree ? "📥" : "📤";

        setTitle(icon + "  " + (isEntree ? "Entrée de stock" : "Sortie de stock"));
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:" + BG_CARD + ";");

        // ── Header ──────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

        StackPane ico = new StackPane();
        Rectangle icoBg = new Rectangle(42, 42);
        icoBg.setArcWidth(12); icoBg.setArcHeight(12);
        icoBg.setFill(Color.web(color + "30"));
        Label icoLbl = new Label(icon);
        icoLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        icoLbl.setTextFill(Color.web(color));
        ico.getChildren().addAll(icoBg, icoLbl);

        VBox hText = new VBox(3);
        Label htitle = new Label(isEntree ? "Entrée de stock" : "Sortie de stock");
        htitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        htitle.setTextFill(Color.web(TEXT));
        Label hsub = new Label(isEntree
            ? "Enregistrez une réception de médicaments"
            : "Enregistrez une dispensation ou consommation");
        hsub.setFont(Font.font("Segoe UI", 11));
        hsub.setTextFill(Color.web(TEXT_SUB));
        hText.getChildren().addAll(htitle, hsub);
        header.getChildren().addAll(ico, hText);

        // ── Champs ───────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.setStyle("-fx-background-color:" + BG_CARD + ";");

        // Sélection médicament
        ComboBox<Produit> cbProduit = new ComboBox<>(produits);
        cbProduit.setPrefWidth(340);
        cbProduit.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-text-fill:" + TEXT + ";"
        );
        cbProduit.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Produit p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(p.getId() + " — " + p.getNom() + " (" + p.getQuantite() + " en stock)");
            }
        });
        cbProduit.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Produit p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText("Sélectionner un médicament..."); setTextFill(Color.web(TEXT_SUB)); return; }
                setText(p.getId() + " — " + p.getNom());
                setTextFill(Color.web(TEXT));
            }
        });
        if (preselect != null) cbProduit.setValue(preselect);

        // Quantité
        TextField tfQte = fChamp("", "Ex: 50");

        // Info stock courant
        Label stockInfo = new Label("");
        stockInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        stockInfo.setTextFill(Color.web(BLUE));

        cbProduit.setOnAction(e -> {
            Produit sel = cbProduit.getValue();
            if (sel != null) {
                stockInfo.setText("Stock actuel : " + sel.getQuantite()
                    + " unité(s)  |  Seuil : " + sel.getSeuilReappro());
            }
        });
        if (preselect != null)
            stockInfo.setText("Stock actuel : " + preselect.getQuantite()
                + " unité(s)  |  Seuil : " + preselect.getSeuilReappro());

        // Motif
        ComboBox<String> cbMotif = new ComboBox<>();
        cbMotif.setPrefWidth(340);
        applyComboStyle(cbMotif);
        if (isEntree) {
            cbMotif.getItems().addAll(
                "Réception commande",
                "Réapprovisionnement",
                "Retour fournisseur",
                "Correction inventaire",
                "Don / Subvention",
                "Autre"
            );
        } else {
            cbMotif.getItems().addAll(
                "Dispensation patient",
                "Usage interne",
                "Périmé — destruction",
                "Retour patient",
                "Correction inventaire",
                "Autre"
            );
        }
        cbMotif.setValue(isEntree ? "Réception commande" : "Dispensation patient");
        cbMotif.setEditable(true);

        // Opérateur
        TextField tfOper = fChamp("Pharmacien", "Ex: Dr. Benali");

        // Aperçu après mouvement
        Label apercu = new Label("");
        apercu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        tfQte.textProperty().addListener((o, old, v) -> {
            Produit sel = cbProduit.getValue();
            if (sel == null) return;
            try {
                int q = Integer.parseInt(v.trim());
                int apres = isEntree ? sel.getQuantite() + q : sel.getQuantite() - q;
                String col = apres < 0 ? RED : apres < sel.getSeuilReappro() ? ORANGE : GREEN;
                apercu.setText("Stock après : " + apres + " unité(s)");
                apercu.setTextFill(Color.web(col));
                if (!isEntree && apres < 0)
                    apercu.setText("⚠  Stock insuffisant ! (disponible : " + sel.getQuantite() + ")");
            } catch (Exception ex) { apercu.setText(""); }
        });

        grid.add(fLabel("💊  Médicament *"),  0, 0); grid.add(cbProduit, 1, 0);
        grid.add(stockInfo,                   1, 1);
        grid.add(fLabel("📦  Quantité *"),    0, 2); grid.add(tfQte,     1, 2);
        grid.add(apercu,                      1, 3);
        grid.add(fLabel("📝  Motif *"),       0, 4); grid.add(cbMotif,   1, 4);
        grid.add(fLabel("👤  Opérateur"),     0, 5); grid.add(tfOper,    1, 5);

        // ── Label erreur ─────────────────────────────────────────
        Label errLbl = new Label("");
        errLbl.setFont(Font.font("Segoe UI", 11));
        errLbl.setTextFill(Color.web(RED));
        errLbl.setPadding(new Insets(0, 24, 4, 24));
        errLbl.setWrapText(true);

        // ── Boutons ──────────────────────────────────────────────
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

        Button btnOk = new Button(isEntree ? "✔  Confirmer l'entrée" : "✔  Confirmer la sortie");
        btnOk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnOk.setPadding(new Insets(9, 24, 9, 24));
        btnOk.setCursor(javafx.scene.Cursor.HAND);
        btnOk.setDefaultButton(true);
        btnOk.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:8;");
        btnOk.setOnMouseEntered(e -> btnOk.setStyle("-fx-background-color:" + color + "cc;-fx-text-fill:white;-fx-background-radius:8;"));
        btnOk.setOnMouseExited (e -> btnOk.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:8;"));

        btnOk.setOnAction(e -> {
            try {
                errLbl.setText("");
                Produit sel = cbProduit.getValue();
                if (sel == null) throw new Exception("Sélectionnez un médicament");

                int qte;
                try { qte = Integer.parseInt(tfQte.getText().trim()); }
                catch (NumberFormatException ex) { throw new Exception("Quantité invalide (ex: 50)"); }
                if (qte <= 0) throw new Exception("La quantité doit être > 0");
                if (!isEntree && qte > sel.getQuantite())
                    throw new Exception("Stock insuffisant (disponible : " + sel.getQuantite() + ")");

                String motif = cbMotif.getValue();
                if (motif == null || motif.trim().isEmpty())
                    throw new Exception("Le motif est obligatoire");

                String operateur = tfOper.getText().trim();
                if (operateur.isEmpty()) operateur = "Pharmacien";

                cb.onValider(sel.getId(), qte, motif.trim(), operateur);
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
        setScene(new Scene(root, 580, 440));
    }

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
        tf.setPrefWidth(340);
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

    private static void applyComboStyle(ComboBox<?> cb) {
        cb.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-text-fill:" + TEXT + ";"
        );
    }
}