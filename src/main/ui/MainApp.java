package main.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import main.model.Produit;
import main.model.Protocol;
import java.io.*;
import java.net.Socket;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainApp extends Application {

    // ── Réseau ─────────────────────────────────────────────────
    private static final String HOST = "localhost";
    private static final int    PORT = 5000;
    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;

    // ── Données ────────────────────────────────────────────────
    private final ObservableList<Produit> produits       = FXCollections.observableArrayList();
    private FilteredList<Produit>         produitsFiltres;

    // ── UI refs ────────────────────────────────────────────────
    private TableView<Produit> table;
    private Label  statusLabel, countLabel, alertLabel, clockLabel;
    private Label  valStatVal, alertStatVal, maxStatVal, minStatVal;
    private TextField    searchField;
    private ComboBox<String> filterCat, sortBox;
    private VBox   chartBox;
    private Circle statusDot;

    // ── Palette Pharmacie ──────────────────────────────────────
    static final String BG        = "#060610";
    static final String BG_CARD   = "#0b0b1a";
    static final String BG_PANEL  = "#10102200";
    static final String BG_ROW    = "#0d0d1f";
    static final String BORDER    = "#1a1a35";
    static final String ACCENT    = "#00b4d8";   // bleu médical
    static final String ACCENT2   = "#90e0ef";   // bleu clair
    static final String GREEN     = "#00f5a0";   // vert santé
    static final String ORANGE    = "#ffb347";
    static final String RED       = "#ff4d6d";   // rouge alerte
    static final String BLUE      = "#48cae4";
    static final String PURPLE    = "#c77dff";
    static final String TEXT      = "#e8f4f8";
    static final String TEXT_SUB  = "#5577aa";

    // ══════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {
        stage.setTitle("PharmFlow — Gestion de Stocks Pharmacie");
        stage.setMinWidth(1200);
        stage.setMinHeight(720);

        produitsFiltres = new FilteredList<>(produits, p -> true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());

        Scene scene = new Scene(root, 1400, 840);
        stage.setScene(scene);
        stage.show();

        startClock();
        connecterServeur();
        if (socket != null) rafraichirListe();
    }

    // ── Horloge temps réel ─────────────────────────────────────
    private void startClock() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e ->
            Platform.runLater(() -> {
                if (clockLabel != null)
                    clockLabel.setText(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss  —  dd/MM/yyyy")));
            })));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    // ── Helpers UI ─────────────────────────────────────────────
    private Label sectionLabel(String txt) {
        Label l = new Label(txt);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        l.setTextFill(Color.web(TEXT_SUB));
        l.setPadding(new Insets(2, 0, 4, 0));
        return l;
    }

    private Button navBtn(String txt, String color, Runnable action) {
        Button b = new Button(txt);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("Segoe UI", 13));
        b.setPadding(new Insets(10, 14, 10, 14));
        b.setCursor(javafx.scene.Cursor.HAND);
        String n = "-fx-background-color:transparent;-fx-text-fill:" + color + ";-fx-background-radius:8;";
        String h = "-fx-background-color:" + color + "20;-fx-text-fill:" + color + ";-fx-background-radius:8;";
        b.setStyle(n);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited (e -> b.setStyle(n));
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button actionBtn(String txt, String color) {
        Button b = new Button(txt);
        b.setFont(Font.font("Segoe UI", 11));
        b.setPadding(new Insets(4, 12, 4, 12));
        b.setCursor(javafx.scene.Cursor.HAND);
        String n = "-fx-background-color:" + color + "20;-fx-text-fill:" + color + ";-fx-background-radius:6;-fx-border-color:" + color + "50;-fx-border-radius:6;";
        String h = "-fx-background-color:" + color + "50;-fx-text-fill:" + color + ";-fx-background-radius:6;-fx-border-color:" + color + ";-fx-border-radius:6;";
        b.setStyle(n);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited (e -> b.setStyle(n));
        return b;
    }

    private <T> TableColumn<Produit,T> col(String title, String prop, double w) {
        TableColumn<Produit,T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setStyle("-fx-alignment:CENTER;");
        return c;
    }

    private void applyFieldStyle(TextField tf) {
        String base = "-fx-background-color:" + BG_PANEL + ";-fx-text-fill:" + TEXT +
            ";-fx-prompt-text-fill:" + TEXT_SUB + ";-fx-border-radius:10;-fx-background-radius:10;-fx-padding:8 14;";
        tf.setStyle(base + "-fx-border-color:" + BORDER + ";");
        tf.focusedProperty().addListener((o, old, f) ->
            tf.setStyle(base + (f ? "-fx-border-color:" + ACCENT + ";" : "-fx-border-color:" + BORDER + ";")));
    }

    private void applyComboStyle(ComboBox<?> cb) {
        cb.setStyle("-fx-background-color:" + BG_PANEL + ";-fx-border-color:" + BORDER +
            ";-fx-border-radius:10;-fx-background-radius:10;-fx-text-fill:" + TEXT + ";");
    }

    private VBox miniStatCard(String label, String initVal, String color) {
        VBox v = new VBox(3);
        v.setPadding(new Insets(10, 12, 10, 12));
        v.setStyle("-fx-background-color:" + color + "15;-fx-background-radius:10;" +
            "-fx-border-color:" + color + "40;-fx-border-radius:10;-fx-border-width:1;");
        HBox.setHgrow(v, Priority.ALWAYS);
        Label lbl = new Label(label); lbl.setFont(Font.font("Segoe UI", 9)); lbl.setTextFill(Color.web(color));
        Label val = new Label(initVal); val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); val.setTextFill(Color.web(TEXT));
        v.getChildren().addAll(lbl, val);
        v.setUserData(val);
        return v;
    }

    private void statut(String msg) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setTextFill(Color.web(msg.startsWith("✔") ? GREEN : RED));
            statusDot.setFill(Color.web(msg.startsWith("✔") ? GREEN : RED));
        });
    }

    private void alerte(String t, String m) { Alert a = new Alert(Alert.AlertType.WARNING, m, ButtonType.OK); a.setTitle(t); a.showAndWait(); }
    private void info(String t, String m)   { Alert a = new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK); a.setTitle(t); a.showAndWait(); }

    private boolean connecte() {
        if (socket == null || socket.isClosed()) { connecterServeur(); return socket != null && !socket.isClosed(); }
        return true;
    }
    // ══════════════════════════════════════════════════════════
    // SIDEBAR
    // ══════════════════════════════════════════════════════════
    private VBox buildSidebar() {
        VBox sb = new VBox(0);
        sb.setPrefWidth(275);
        sb.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");
        sb.getChildren().addAll(buildLogo(), buildSideStats(), buildNavSection(), buildAlertSection(), buildConnStatus());
        return sb;
    }

    private VBox buildLogo() {
        VBox box = new VBox(0);
        box.setPadding(new Insets(26, 22, 20, 22));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        // Icône croix pharmacie
        StackPane icon = new StackPane();
        Rectangle iconBg = new Rectangle(46, 46);
        iconBg.setArcWidth(14); iconBg.setArcHeight(14);
        iconBg.setFill(Color.web(ACCENT));

        // Croix médicale
        VBox cross = new VBox(0);
        cross.setAlignment(Pos.CENTER);
        Label crossH = new Label("✚");
        crossH.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        crossH.setTextFill(Color.WHITE);
        cross.getChildren().add(crossH);
        icon.getChildren().addAll(iconBg, cross);

        // Animation pulse sur logo
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.5), icon);
        pulse.setFromX(1.0); pulse.setToX(1.06);
        pulse.setFromY(1.0); pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        VBox texts = new VBox(4);
        Label name = new Label("PharmFlow");
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        name.setTextFill(Color.web(TEXT));

        Label tagline = new Label("Gestion de Stocks Pharmacie");
        tagline.setFont(Font.font("Segoe UI", 10));
        tagline.setTextFill(Color.web(ACCENT));

        clockLabel = new Label("--:--:--");
        clockLabel.setFont(Font.font("Courier New", 10));
        clockLabel.setTextFill(Color.web(TEXT_SUB));

        texts.getChildren().addAll(name, tagline, clockLabel);
        row.getChildren().addAll(icon, texts);
        box.getChildren().add(row);
        return box;
    }

    private VBox buildSideStats() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(18, 16, 18, 16));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        box.getChildren().add(sectionLabel("TABLEAU DE BORD"));

        // Grand compteur
        VBox countCard = new VBox(4);
        countCard.setPadding(new Insets(14, 16, 14, 16));
        countCard.setStyle(
            "-fx-background-color:#0a0a20;" +
            "-fx-background-radius:12;" +
            "-fx-border-color:" + ACCENT + "44;" +
            "-fx-border-radius:12;-fx-border-width:1;"
        );

        HBox countRow = new HBox(10);
        countRow.setAlignment(Pos.CENTER_LEFT);
        countLabel = new Label("0");
        countLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        countLabel.setTextFill(Color.web(ACCENT));

        VBox countTexts = new VBox(2);
        Label countTitle = new Label("médicaments");
        countTitle.setFont(Font.font("Segoe UI", 11));
        countTitle.setTextFill(Color.web(TEXT_SUB));
        Label countSub = new Label("en stock");
        countSub.setFont(Font.font("Segoe UI", 11));
        countSub.setTextFill(Color.web(TEXT_SUB));
        countTexts.getChildren().addAll(countTitle, countSub);
        countRow.getChildren().addAll(countLabel, countTexts);
        countCard.getChildren().add(countRow);

        // Mini stats 2x2
        HBox row1 = new HBox(8);
        HBox row2 = new HBox(8);

        VBox sVal   = miniStatCard("Valeur stock",   "0 DA",  BLUE);
        VBox sAlert = miniStatCard("Stock critique",  "0",     RED);
        VBox sMax   = miniStatCard("Prix max",        "—",     GREEN);
        VBox sMin   = miniStatCard("Prix min",        "—",     ORANGE);

        valStatVal   = (Label) sVal.getUserData();
        alertStatVal = (Label) sAlert.getUserData();
        maxStatVal   = (Label) sMax.getUserData();
        minStatVal   = (Label) sMin.getUserData();

        row1.getChildren().addAll(sVal, sAlert);
        row2.getChildren().addAll(sMax, sMin);

        box.getChildren().addAll(countCard, row1, row2);
        return box;
    }

    private VBox buildNavSection() {
        VBox box = new VBox(3);
        box.setPadding(new Insets(16, 14, 16, 14));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        VBox.setVgrow(box, Priority.ALWAYS);
        box.getChildren().add(sectionLabel("ACTIONS RAPIDES"));
        box.getChildren().addAll(
            navBtn("＋   Ajouter un médicament",    ACCENT,  this::ouvrirFormulaireAjout),
            navBtn("✎    Modifier la sélection",     ACCENT2, this::ouvrirFormulaireModification),
            navBtn("✕    Supprimer la sélection",    RED,     this::supprimerSelectionne),
            navBtn("↻    Actualiser le stock",       GREEN,   this::rafraichirListe),
            navBtn("📤   Exporter en CSV",            ORANGE,  this::exporterCSV)
        );
        return box;
    }

    private VBox buildAlertSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        box.getChildren().add(sectionLabel("ALERTES STOCK CRITIQUE"));

        alertLabel = new Label("✔  Aucune alerte");
        alertLabel.setFont(Font.font("Segoe UI", 11));
        alertLabel.setTextFill(Color.web(GREEN));
        alertLabel.setWrapText(true);
        box.getChildren().add(alertLabel);
        return box;
    }

    private VBox buildConnStatus() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 16, 20, 16));
        box.getChildren().add(sectionLabel("CONNEXION SERVEUR"));

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        statusDot = new Circle(5, Color.web(RED));

        // Animation pulse sur le dot de statut
        FadeTransition dotPulse = new FadeTransition(Duration.seconds(1), statusDot);
        dotPulse.setFromValue(1.0); dotPulse.setToValue(0.2);
        dotPulse.setAutoReverse(true);
        dotPulse.setCycleCount(Animation.INDEFINITE);
        dotPulse.play();

        statusLabel = new Label("Non connecté");
        statusLabel.setFont(Font.font("Segoe UI", 11));
        statusLabel.setTextFill(Color.web(TEXT_SUB));
        statusLabel.setWrapText(true);
        row.getChildren().addAll(statusDot, statusLabel);
        box.getChildren().add(row);
        return box;
    }
    // ══════════════════════════════════════════════════════════
    // CENTRE PRINCIPAL
    // ══════════════════════════════════════════════════════════
    private VBox buildCenter() {
        VBox center = new VBox(0);
        center.setStyle("-fx-background-color:" + BG + ";");
        center.getChildren().addAll(buildTopBar(), buildContent());
        VBox.setVgrow(center, Priority.ALWAYS);
        return center;
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(14);
        bar.setPadding(new Insets(22, 28, 18, 28));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

        VBox titleBox = new VBox(4);
        Label title = new Label("💊  Stock des Médicaments");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.web(TEXT));
        Label subtitle = new Label("Visualisez, filtrez, analysez et exportez votre inventaire pharmaceutique");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(TEXT_SUB));
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Recherche
        searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher un médicament...");
        searchField.setPrefWidth(230);
        searchField.setFont(Font.font("Segoe UI", 13));
        applyFieldStyle(searchField);
        searchField.textProperty().addListener((o, old, v) -> appliquerFiltres());

        // Filtre catégorie
        filterCat = new ComboBox<>();
        filterCat.setPromptText("Catégorie");
        filterCat.setPrefWidth(150);
        applyComboStyle(filterCat);
        filterCat.setOnAction(e -> appliquerFiltres());

        // Tri
        sortBox = new ComboBox<>();
        sortBox.setItems(FXCollections.observableArrayList(
            "Tri par défaut", "Nom ↑", "Nom ↓",
            "Prix ↑", "Prix ↓", "Quantité ↑", "Quantité ↓"
        ));
        sortBox.setValue("Tri par défaut");
        sortBox.setPrefWidth(155);
        applyComboStyle(sortBox);
        sortBox.setOnAction(e -> appliquerTri());

        bar.getChildren().addAll(titleBox, searchField, filterCat, sortBox);
        return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 20, 28));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Graphique catégories
        chartBox = new VBox(10);
        chartBox.setPadding(new Insets(16, 20, 16, 20));
        chartBox.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );
        Label chartTitle = new Label("📊  Répartition par catégorie de médicaments");
        chartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        chartTitle.setTextFill(Color.web(TEXT));
        chartBox.getChildren().add(chartTitle);

        // Table card
        buildTable();
        VBox tableCard = new VBox();
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        tableCard.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );

        // Header table
        HBox tableHeader = new HBox(12);
        tableHeader.setPadding(new Insets(14, 20, 12, 20));
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        Label tableTitle = new Label("🗂  Inventaire complet");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        tableTitle.setTextFill(Color.web(TEXT));
        HBox.setHgrow(tableTitle, Priority.ALWAYS);

        // Légende couleurs quantité
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
            legendItem("Critique < 5",  RED),
            legendItem("Faible < 15",   ORANGE),
            legendItem("Normal ≥ 15",   GREEN)
        );
        tableHeader.getChildren().addAll(tableTitle, legend);

        // Footer table
        HBox footer = new HBox();
        footer.setPadding(new Insets(8, 18, 8, 18));
        footer.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:1 0 0 0;");
        Label footerLbl = new Label("💡  Double-cliquez sur une ligne pour modifier  •  Cliquez sur les en-têtes pour trier");
        footerLbl.setFont(Font.font("Segoe UI", 10));
        footerLbl.setTextFill(Color.web(TEXT_SUB));
        footer.getChildren().add(footerLbl);

        tableCard.getChildren().addAll(tableHeader, table, footer);
        content.getChildren().addAll(chartBox, tableCard);
        return content;
    }

    private HBox legendItem(String txt, String color) {
        HBox h = new HBox(5);
        h.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web(color));
        Label lbl = new Label(txt);
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setTextFill(Color.web(TEXT_SUB));
        h.getChildren().addAll(dot, lbl);
        return h;
    }

    // ══════════════════════════════════════════════════════════
    // TABLE
    // ══════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void buildTable() {
        table = new TableView<>(produitsFiltres);
        table.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-table-cell-border-color:" + BORDER + ";" +
            "-fx-control-inner-background:" + BG_CARD + ";" +
            "-fx-control-inner-background-alt:" + BG_ROW + ";" +
            "-fx-border-color:transparent;"
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label ph = new Label("💊  Aucun médicament trouvé\nCliquez sur Actualiser ou Ajouter un médicament");
        ph.setTextFill(Color.web(TEXT_SUB));
        ph.setFont(Font.font("Segoe UI", 13));
        ph.setAlignment(Pos.CENTER);
        table.setPlaceholder(ph);

        // Double-clic pour modifier
        table.setRowFactory(tv -> {
            TableRow<Produit> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    ouvrirFormulaireModification();
            });
            return row;
        });

        TableColumn<Produit,Integer> colId  = col("ID",            "id",        55);
        TableColumn<Produit,String>  colNom = col("Médicament",    "nom",       210);
        TableColumn<Produit,Double>  colPrix= col("Prix (DA)",     "prix",      130);
        TableColumn<Produit,Integer> colQte = col("Quantité",      "quantite",  110);
        TableColumn<Produit,String>  colCat = col("Catégorie",     "categorie", 150);

        // Quantité avec indicateur coloré + barre mini
        colQte.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                String color = v < 5 ? RED : v < 15 ? ORANGE : GREEN;
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER);
                Circle dot = new Circle(5, Color.web(color));
                Label lbl = new Label(String.valueOf(v));
                lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                lbl.setTextFill(Color.web(color));
                box.getChildren().addAll(dot, lbl);
                setGraphic(box);
            }
        });

        // Prix formaté
        colPrix.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("%,.2f DA", v));
                setAlignment(Pos.CENTER);
                setStyle("-fx-text-fill:" + BLUE + ";-fx-font-weight:bold;");
            }
        });

        // Catégorie avec badge
        colCat.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label badge = new Label(v);
                badge.setFont(Font.font("Segoe UI", 10));
                badge.setTextFill(Color.web(ACCENT));
                badge.setPadding(new Insets(2, 8, 2, 8));
                badge.setStyle(
                    "-fx-background-color:" + ACCENT + "22;" +
                    "-fx-background-radius:8;" +
                    "-fx-border-color:" + ACCENT + "44;" +
                    "-fx-border-radius:8;-fx-border-width:1;"
                );
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        // Colonne actions inline
        TableColumn<Produit,Void> colAct = new TableColumn<>("Actions");
        colAct.setPrefWidth(190);
        colAct.setCellFactory(c -> new TableCell<>() {
            final Button bMod = actionBtn("✎  Modifier",   ACCENT);
            final Button bSup = actionBtn("✕  Supprimer",  RED);
            final HBox   box  = new HBox(8, bMod, bSup);
            {
                box.setAlignment(Pos.CENTER);
                bMod.setOnAction(e -> { table.getSelectionModel().select(getIndex()); ouvrirFormulaireModification(); });
                bSup.setOnAction(e -> { table.getSelectionModel().select(getIndex()); supprimerSelectionne(); });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(colId, colNom, colPrix, colQte, colCat, colAct);
    }

    // ══════════════════════════════════════════════════════════
    // GRAPHIQUE PAR CATÉGORIE
    // ══════════════════════════════════════════════════════════
    private void mettreAJourGraphique() {
        Platform.runLater(() -> {
            while (chartBox.getChildren().size() > 1)
                chartBox.getChildren().remove(1);
            if (produits.isEmpty()) return;

            var parCat = produits.stream().collect(
                Collectors.groupingBy(Produit::getCategorie, Collectors.counting()));
            long max = parCat.values().stream().mapToLong(Long::longValue).max().orElse(1);

            String[] colors = {ACCENT, GREEN, ORANGE, BLUE, PURPLE, ACCENT2, RED};
            int ci = 0;

            for (var entry : parCat.entrySet()) {
                String color = colors[ci % colors.length]; ci++;
                double ratio = (double) entry.getValue() / max;

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);

                Label catLbl = new Label(entry.getKey());
                catLbl.setFont(Font.font("Segoe UI", 11));
                catLbl.setTextFill(Color.web(TEXT_SUB));
                catLbl.setMinWidth(130);

                StackPane barWrapper = new StackPane();
                barWrapper.setStyle("-fx-background-color:#0d0d22;-fx-background-radius:4;");
                barWrapper.setMinHeight(12); barWrapper.setMaxHeight(12);
                HBox.setHgrow(barWrapper, Priority.ALWAYS);
                barWrapper.setAlignment(Pos.CENTER_LEFT);

                Rectangle bar = new Rectangle(0, 12);
                bar.setFill(Color.web(color));
                bar.setArcWidth(4); bar.setArcHeight(4);
                barWrapper.getChildren().add(bar);

                Label cntLbl = new Label(entry.getValue() + " réf.");
                cntLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                cntLbl.setTextFill(Color.web(color));
                cntLbl.setMinWidth(65);

                row.getChildren().addAll(catLbl, barWrapper, cntLbl);
                chartBox.getChildren().add(row);

                // Animation barre
                barWrapper.widthProperty().addListener((obs, old, w) -> {
                    if (w.doubleValue() > 0) {
                        Timeline tl = new Timeline(
                            new KeyFrame(Duration.ZERO,        new KeyValue(bar.widthProperty(), 0)),
                            new KeyFrame(Duration.millis(700), new KeyValue(bar.widthProperty(), w.doubleValue() * ratio, Interpolator.EASE_OUT))
                        );
                        tl.play();
                    }
                });
            }
        });
    }
    // ══════════════════════════════════════════════════════════
    // STATISTIQUES
    // ══════════════════════════════════════════════════════════
    private void mettreAJourStats() {
        Platform.runLater(() -> {
            int    total    = produits.size();
            double valTotal = produits.stream().mapToDouble(p -> p.getPrix() * p.getQuantite()).sum();
            long   alerts   = produits.stream().filter(p -> p.getQuantite() < 5).count();
            double prixMax  = produits.stream().mapToDouble(Produit::getPrix).max().orElse(0);
            double prixMin  = produits.stream().mapToDouble(Produit::getPrix).min().orElse(0);

            animateCount(countLabel, total);
            valStatVal.setText(String.format("%,.0f DA", valTotal));
            alertStatVal.setText(String.valueOf(alerts));
            maxStatVal.setText(String.format("%,.0f DA", prixMax));
            minStatVal.setText(String.format("%,.0f DA", prixMin));

            if (alerts > 0) {
                alertLabel.setText("⚠  " + alerts + " médicament(s) en\nstock critique (< 5) !");
                alertLabel.setTextFill(Color.web(RED));
                FadeTransition ft = new FadeTransition(Duration.millis(500), alertLabel);
                ft.setFromValue(1.0); ft.setToValue(0.2);
                ft.setAutoReverse(true); ft.setCycleCount(8);
                ft.play();
            } else {
                alertLabel.setText("✔  Stock en bon état");
                alertLabel.setTextFill(Color.web(GREEN));
            }
        });
    }

    private void animateCount(Label label, int target) {
        int from;
        try { from = Integer.parseInt(label.getText()); }
        catch (Exception e) { from = 0; }
        int start = from;
        Timeline tl = new Timeline();
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            int step = i;
            int val  = start + (int) ((target - start) * step / (double) steps);
            tl.getKeyFrames().add(
                new KeyFrame(Duration.millis(35 * i), e -> label.setText(String.valueOf(val)))
            );
        }
        tl.play();
    }

    // ══════════════════════════════════════════════════════════
    // FILTRES & RECHERCHE
    // ══════════════════════════════════════════════════════════
    private void appliquerFiltres() {
        String search = searchField.getText().toLowerCase().trim();
        String cat    = filterCat.getValue();
        produitsFiltres.setPredicate(p -> {
            boolean ms = search.isEmpty()
                || p.getNom().toLowerCase().contains(search)
                || p.getCategorie().toLowerCase().contains(search)
                || String.valueOf(p.getId()).contains(search);
            boolean mc = cat == null || cat.equals("Toutes") || p.getCategorie().equals(cat);
            return ms && mc;
        });
    }

    private void appliquerTri() {
        if (sortBox.getValue() == null) return;
        List<Produit> liste = new ArrayList<>(produits);
        switch (sortBox.getValue()) {
            case "Nom ↑"      -> liste.sort(Comparator.comparing(Produit::getNom));
            case "Nom ↓"      -> liste.sort(Comparator.comparing(Produit::getNom).reversed());
            case "Prix ↑"     -> liste.sort(Comparator.comparingDouble(Produit::getPrix));
            case "Prix ↓"     -> liste.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
            case "Quantité ↑" -> liste.sort(Comparator.comparingInt(Produit::getQuantite));
            case "Quantité ↓" -> liste.sort(Comparator.comparingInt(Produit::getQuantite).reversed());
        }
        produits.setAll(liste);
    }

    private void mettreAJourFiltreCategories() {
        List<String> cats = produits.stream()
            .map(Produit::getCategorie).distinct().sorted()
            .collect(Collectors.toList());
        cats.add(0, "Toutes");
        filterCat.setItems(FXCollections.observableArrayList(cats));
    }

    // ══════════════════════════════════════════════════════════
    // EXPORT CSV
    // ══════════════════════════════════════════════════════════
    private void exporterCSV() {
        if (produits.isEmpty()) { alerte("Export impossible", "Aucun médicament à exporter."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le stock en CSV");
        fc.setInitialFileName("pharmflow_stock_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File file = fc.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("ID,Médicament,Prix (DA),Quantité,Catégorie,Valeur totale (DA),Statut stock");
            for (Produit p : produits) {
                String statut = p.getQuantite() < 5 ? "CRITIQUE" : p.getQuantite() < 15 ? "FAIBLE" : "NORMAL";
                pw.printf("%d,%s,%.2f,%d,%s,%.2f,%s%n",
                    p.getId(), p.getNom(), p.getPrix(),
                    p.getQuantite(), p.getCategorie(),
                    p.getPrix() * p.getQuantite(), statut);
            }
            statut("✔ Exporté : " + file.getName());
            info("Export réussi !", "Fichier sauvegardé :\n" + file.getAbsolutePath());
        } catch (IOException e) { alerte("Erreur export", e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    // CONNEXION SERVEUR
    // ══════════════════════════════════════════════════════════
    private void connecterServeur() {
        try {
            socket = new Socket(HOST, PORT);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Platform.runLater(() -> {
                statusDot.setFill(Color.web(GREEN));
                statusLabel.setText("Connecté — localhost:" + PORT);
                statusLabel.setTextFill(Color.web(GREEN));
            });
        } catch (ConnectException e) {
            statut("✘ Serveur non démarré");
            alerte("Connexion impossible", "Démarrez Server.java d'abord !");
        } catch (IOException e) { statut("✘ " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════
    private void rafraichirListe() {
        if (!connecte()) return;
        try {
            out.println(Protocol.LIST);
            String rep = in.readLine();
            produits.clear();
            if (rep != null && !rep.equals(Protocol.EMPTY))
                for (String json : rep.split(";")) {
                    Produit p = Produit.fromJson(json);
                    if (p != null) produits.add(p);
                }
            mettreAJourStats();
            mettreAJourGraphique();
            mettreAJourFiltreCategories();
            statut("✔ " + produits.size() + " médicament(s) chargé(s)");
        } catch (IOException e) { statut("✘ " + e.getMessage()); }
    }

    private void ouvrirFormulaireAjout() {
        if (!connecte()) return;
        new FormulaireProduit(null, p -> {
            try {
                out.println(Protocol.ADD + ":" + p.toJson());
                String rep = in.readLine();
                if (rep != null && rep.startsWith(Protocol.OK + ":")) {
                    Produit c = Produit.fromJson(rep.substring(3));
                    if (c != null) produits.add(c);
                    mettreAJourStats();
                    mettreAJourGraphique();
                    mettreAJourFiltreCategories();
                    statut("✔ Médicament ajouté avec succès");
                } else statut("✘ " + rep);
            } catch (IOException e) { statut("✘ " + e.getMessage()); }
        }).show();
    }

    private void ouvrirFormulaireModification() {
        Produit sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alerte("Aucune sélection", "Sélectionnez un médicament à modifier."); return; }
        if (!connecte()) return;
        new FormulaireProduit(sel, p -> {
            p.setId(sel.getId());
            try {
                out.println(Protocol.UPDATE + ":" + p.toJson());
                String rep = in.readLine();
                if (rep != null && rep.startsWith(Protocol.OK + ":")) {
                    Produit maj = Produit.fromJson(rep.substring(3));
                    int idx = produits.indexOf(sel);
                    if (idx >= 0 && maj != null) produits.set(idx, maj);
                    mettreAJourStats();
                    mettreAJourGraphique();
                    statut("✔ Médicament modifié avec succès");
                } else statut("✘ " + rep);
            } catch (IOException e) { statut("✘ " + e.getMessage()); }
        }).show();
    }

    private void supprimerSelectionne() {
        Produit sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alerte("Aucune sélection", "Sélectionnez un médicament à supprimer."); return; }
        if (!connecte()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer \"" + sel.getNom() + "\" ?");
        confirm.setContentText(
            "Catégorie : " + sel.getCategorie() + "\n" +
            "Stock actuel : " + sel.getQuantite() + " unité(s)\n" +
            "Cette action est irréversible."
        );
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    out.println(Protocol.DELETE + ":" + sel.getId());
                    String rep = in.readLine();
                    if (rep != null && rep.startsWith(Protocol.OK)) {
                        produits.remove(sel);
                        mettreAJourStats();
                        mettreAJourGraphique();
                        mettreAJourFiltreCategories();
                        statut("✔ Médicament supprimé");
                    } else statut("✘ " + rep);
                } catch (IOException e) { statut("✘ " + e.getMessage()); }
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (out != null) out.println(Protocol.EXIT);
        if (socket != null && !socket.isClosed()) socket.close();
    }

    public static void main(String[] args) { launch(args); }
    // ══════════════════════════════════════════════════════════
    // FORMULAIRE POPUP
    // ══════════════════════════════════════════════════════════
    static class FormulaireProduit extends Stage {
        interface Callback { void onValider(Produit p); }

        FormulaireProduit(Produit existant, Callback cb) {
            boolean isEdit = existant != null;
            setTitle(isEdit ? "✎  Modifier le médicament" : "＋  Nouveau médicament");
            initModality(Modality.APPLICATION_MODAL);
            setResizable(false);

            VBox root = new VBox(0);
            root.setStyle("-fx-background-color:" + BG_CARD + ";");

            // ── Header ─────────────────────────────────────────
            HBox header = new HBox(14);
            header.setPadding(new Insets(22, 26, 18, 26));
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
            htitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            htitle.setTextFill(Color.web(TEXT));
            Label hsub = new Label(isEdit ? "Modifiez les champs et validez" : "Remplissez tous les champs obligatoires");
            hsub.setFont(Font.font("Segoe UI", 11));
            hsub.setTextFill(Color.web(TEXT_SUB));
            hText.getChildren().addAll(htitle, hsub);
            header.getChildren().addAll(ico, hText);

            // ── Champs ─────────────────────────────────────────
            GridPane grid = new GridPane();
            grid.setHgap(14); grid.setVgap(14);
            grid.setPadding(new Insets(22, 26, 10, 26));
            grid.setStyle("-fx-background-color:" + BG_CARD + ";");

            TextField tfNom  = fChamp(
                existant != null ? existant.getNom()                     : "",
                "Ex: Paracétamol 500mg");
            TextField tfPrix = fChamp(
                existant != null ? String.valueOf(existant.getPrix())     : "",
                "Ex: 250.00");
            TextField tfQte  = fChamp(
                existant != null ? String.valueOf(existant.getQuantite()) : "",
                "Ex: 100");
            TextField tfCat  = fChamp(
                existant != null ? existant.getCategorie()                : "",
                "Ex: Analgésique");

            // Indicateur stock en temps réel
            Label stockIndicator = new Label("");
            stockIndicator.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            tfQte.textProperty().addListener((o, old, v) -> {
                try {
                    int q = Integer.parseInt(v.trim());
                    if (q < 5) {
                        stockIndicator.setText("⚠  Stock critique !");
                        stockIndicator.setTextFill(Color.web(RED));
                    } else if (q < 15) {
                        stockIndicator.setText("⚡  Stock faible");
                        stockIndicator.setTextFill(Color.web(ORANGE));
                    } else {
                        stockIndicator.setText("✔  Stock normal");
                        stockIndicator.setTextFill(Color.web(GREEN));
                    }
                } catch (Exception ex) {
                    stockIndicator.setText("");
                }
            });

            grid.add(fLabel("💊  Médicament *"), 0, 0); grid.add(tfNom,  1, 0);
            grid.add(fLabel("💰  Prix (DA) *"),   0, 1); grid.add(tfPrix, 1, 1);
            grid.add(fLabel("📦  Quantité *"),    0, 2);

            // Quantité + indicateur inline
            VBox qteBox = new VBox(4);
            qteBox.getChildren().addAll(tfQte, stockIndicator);
            grid.add(qteBox, 1, 2);

            grid.add(fLabel("🏷  Catégorie *"),   0, 3); grid.add(tfCat,  1, 3);

            // ── Label erreur ───────────────────────────────────
            Label errLbl = new Label("");
            errLbl.setFont(Font.font("Segoe UI", 11));
            errLbl.setTextFill(Color.web(RED));
            errLbl.setPadding(new Insets(0, 26, 4, 26));
            errLbl.setWrapText(true);

            // ── Boutons ────────────────────────────────────────
            HBox actions = new HBox(10);
            actions.setPadding(new Insets(8, 26, 22, 26));
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button btnAnnuler = new Button("Annuler");
            btnAnnuler.setFont(Font.font("Segoe UI", 13));
            btnAnnuler.setPadding(new Insets(9, 22, 9, 22));
            btnAnnuler.setCursor(javafx.scene.Cursor.HAND);
            btnAnnuler.setStyle(
                "-fx-background-color:#12122a;" +
                "-fx-text-fill:" + TEXT_SUB + ";" +
                "-fx-background-radius:8;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-radius:8;-fx-border-width:1;"
            );
            btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
                "-fx-background-color:#1a1a3a;" +
                "-fx-text-fill:" + TEXT + ";" +
                "-fx-background-radius:8;" +
                "-fx-border-color:" + TEXT_SUB + ";" +
                "-fx-border-radius:8;-fx-border-width:1;"
            ));
            btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(
                "-fx-background-color:#12122a;" +
                "-fx-text-fill:" + TEXT_SUB + ";" +
                "-fx-background-radius:8;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-radius:8;-fx-border-width:1;"
            ));
            btnAnnuler.setOnAction(e -> close());

            String btnColor = isEdit ? ACCENT2 : ACCENT;
            Button btnOk = new Button(isEdit ? "✔  Enregistrer" : "✔  Ajouter");
            btnOk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            btnOk.setPadding(new Insets(9, 24, 9, 24));
            btnOk.setCursor(javafx.scene.Cursor.HAND);
            btnOk.setDefaultButton(true);
            btnOk.setStyle(
                "-fx-background-color:" + btnColor + ";" +
                "-fx-text-fill:white;" +
                "-fx-background-radius:8;"
            );
            btnOk.setOnMouseEntered(e -> btnOk.setStyle(
                "-fx-background-color:" + btnColor + "dd;" +
                "-fx-text-fill:white;" +
                "-fx-background-radius:8;"
            ));
            btnOk.setOnMouseExited(e -> btnOk.setStyle(
                "-fx-background-color:" + btnColor + ";" +
                "-fx-text-fill:white;" +
                "-fx-background-radius:8;"
            ));

            // ── Validation ─────────────────────────────────────
            btnOk.setOnAction(e -> {
                try {
                    errLbl.setText("");
                    String nom = tfNom.getText().trim();
                    if (nom.isEmpty()) throw new Exception("Le nom du médicament est obligatoire");

                    double prix;
                    try { prix = Double.parseDouble(tfPrix.getText().trim()); }
                    catch (NumberFormatException ex) { throw new Exception("Le prix doit être un nombre valide (ex: 250.00)"); }
                    if (prix < 0) throw new Exception("Le prix doit être supérieur ou égal à 0");

                    int qte;
                    try { qte = Integer.parseInt(tfQte.getText().trim()); }
                    catch (NumberFormatException ex) { throw new Exception("La quantité doit être un entier valide (ex: 100)"); }
                    if (qte < 0) throw new Exception("La quantité doit être supérieure ou égale à 0");

                    String cat = tfCat.getText().trim();
                    if (cat.isEmpty()) throw new Exception("La catégorie est obligatoire");

                    cb.onValider(new Produit(0, nom, prix, qte, cat));
                    close();

                } catch (Exception ex) {
                    errLbl.setText("⚠  " + ex.getMessage());
                    // Animation shake sur erreur
                    TranslateTransition shake = new TranslateTransition(Duration.millis(60), root);
                    shake.setFromX(0); shake.setByX(8);
                    shake.setAutoReverse(true); shake.setCycleCount(4);
                    shake.play();
                }
            });

            actions.getChildren().addAll(btnAnnuler, btnOk);

            // ── Assemblage final ───────────────────────────────
            root.getChildren().addAll(header, grid, errLbl, actions);
            setScene(new Scene(root, 450, 340));
        }

        private static Label fLabel(String txt) {
            Label l = new Label(txt);
            l.setFont(Font.font("Segoe UI", 12));
            l.setTextFill(Color.web(TEXT_SUB));
            l.setMinWidth(120);
            return l;
        }

        private static TextField fChamp(String val, String prompt) {
            TextField tf = new TextField(val);
            tf.setPromptText(prompt);
            tf.setPrefWidth(235);
            tf.setFont(Font.font("Segoe UI", 13));
            String base =
                "-fx-background-color:#0d0d20;" +
                "-fx-text-fill:" + TEXT + ";" +
                "-fx-prompt-text-fill:" + TEXT_SUB + ";" +
                "-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-padding:8 12;";
            tf.setStyle(base + "-fx-border-color:" + BORDER + ";");
            tf.focusedProperty().addListener((o, old, f) ->
                tf.setStyle(base + (f
                    ? "-fx-border-color:" + ACCENT + ";"
                    : "-fx-border-color:" + BORDER + ";")));
            return tf;
        }
    }
    // ══════════════════════════════════════════════════════════
    // FIN DE CLASSE
    // ══════════════════════════════════════════════════════════
}