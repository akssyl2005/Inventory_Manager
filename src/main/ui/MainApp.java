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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.*;
import javafx.util.Duration;
import main.client.UDPNotificationClient;
import main.model.*;

import java.io.*;
import java.net.Socket;
import java.net.ConnectException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainApp extends Application {

    // ── Réseau ─────────────────────────────────────────────────
    private static String HOST = getServerHost();
    private static final int PORT = 5000;
    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;
    private ProduitService service;
    private UDPNotificationClient udpClient;
    private main.client.RMIClient rmiClient;

    // ── Données ────────────────────────────────────────────────
    private final ObservableList<Produit>   produits   = FXCollections.observableArrayList();
    private final ObservableList<Mouvement> mouvements = FXCollections.observableArrayList();
    private final ObservableList<Produit>   alertes    = FXCollections.observableArrayList();
    private FilteredList<Produit>           produitsFiltres;

    // ── UI refs ────────────────────────────────────────────────
   private Label   statusLabel, countLabel, alertLabel, clockLabel, udpNotifLabel;
    private Label   valStatVal, alertStatVal, maxStatVal, minStatVal;
    private Label   entreesStatVal, sortiesStatVal, mouvStatVal;
    private Circle  statusDot;
    private TabPane tabPane;
    private TableView<Produit>   tableProduits;
    private TableView<Mouvement> tableMouvements;
    private TableView<Produit>   tableAlertes;
    private TextField            searchField;
    private ComboBox<String>     filterCat, sortBox;
    private VBox                 chartBox, dashboardBox;

    // ── Palette ────────────────────────────────────────────────
    static final String BG       = "#060610";
    static final String BG_CARD  = "#0b0b1a";
    static final String BG_PANEL = "#101022";
    static final String BG_ROW   = "#0d0d1f";
    static final String BORDER   = "#1a1a35";
    static final String ACCENT   = "#00b4d8";
    static final String ACCENT2  = "#90e0ef";
    static final String GREEN    = "#00f5a0";
    static final String ORANGE   = "#ffb347";
    static final String RED      = "#ff4d6d";
    static final String BLUE     = "#48cae4";
    static final String PURPLE   = "#c77dff";
    static final String YELLOW   = "#ffd32a";
    static final String TEXT     = "#e8f4f8";
    static final String TEXT_SUB = "#5577aa";

    // ══════════════════════════════════════════════════════════
  @Override
public void start(Stage stage) {
    stage.setTitle("PharmFlow Pro — Gestion de Stocks Pharmacie");
    stage.setMinWidth(1200);
    stage.setMinHeight(720);

    // ── IMPORTANT : initialiser avant buildCenter() ────────
    produitsFiltres = new FilteredList<>(produits, p -> true);

    BorderPane root = new BorderPane();
    root.setStyle("-fx-background-color:" + BG + ";");
    root.setLeft(buildSidebar());
    root.setCenter(buildCenter());
    stage.setScene(new Scene(root, 1420, 860));
    stage.show();
    startClock();

    // ── Écran de chargement ────────────────────────────────
    Stage splash = new Stage();
    splash.initModality(Modality.APPLICATION_MODAL);
    splash.initStyle(StageStyle.UNDECORATED);

    VBox splashRoot = new VBox(20);
    splashRoot.setAlignment(Pos.CENTER);
    splashRoot.setPadding(new Insets(50, 60, 50, 60));
    splashRoot.setStyle(
        "-fx-background-color:#0b0b1a;" +
        "-fx-border-color:#00b4d8;" +
        "-fx-border-width:2;" +
        "-fx-border-radius:16;" +
        "-fx-background-radius:16;"
    );

    Label icon = new Label("⚕");
    icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
    icon.setTextFill(Color.web(ACCENT));

    ScaleTransition iconPulse = new ScaleTransition(
        Duration.seconds(0.8), icon);
    iconPulse.setFromX(1.0); iconPulse.setToX(1.15);
    iconPulse.setFromY(1.0); iconPulse.setToY(1.15);
    iconPulse.setAutoReverse(true);
    iconPulse.setCycleCount(Animation.INDEFINITE);
    iconPulse.play();

    Label title = new Label("PharmFlow Pro");
    title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
    title.setTextFill(Color.web(TEXT));

    Label scanMsg = new Label(
        "🔍  Recherche du serveur sur le réseau...");
    scanMsg.setFont(Font.font("Segoe UI", 13));
    scanMsg.setTextFill(Color.web(TEXT_SUB));

    Label ipLabel = new Label("Scan en cours...");
    ipLabel.setFont(Font.font("Courier New", 11));
    ipLabel.setTextFill(Color.web(ACCENT));

    ProgressBar bar = new ProgressBar(0);
    bar.setPrefWidth(320);
    bar.setStyle(
        "-fx-accent:" + ACCENT + ";" +
        "-fx-background-color:#1a1a35;" +
        "-fx-background-radius:8;" +
        "-fx-border-radius:8;"
    );

    Label pct = new Label("0 / 254");
    pct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
    pct.setTextFill(Color.web(ACCENT));

    // ── Bouton passer le scan ──────────────────────────────
    Button btnSkip = new Button("⏭  Entrer l'IP manuellement");
    btnSkip.setFont(Font.font("Segoe UI", 11));
    btnSkip.setPadding(new Insets(6, 16, 6, 16));
    btnSkip.setCursor(javafx.scene.Cursor.HAND);
    btnSkip.setStyle(
        "-fx-background-color:transparent;" +
        "-fx-text-fill:" + TEXT_SUB + ";" +
        "-fx-border-color:" + BORDER + ";" +
        "-fx-border-radius:8;-fx-border-width:1;"
    );

    splashRoot.getChildren().addAll(
        icon, title, scanMsg, ipLabel, bar, pct, btnSkip);
    splash.setScene(new Scene(splashRoot, 420, 340));
    splash.show();

    // ── Flag pour arrêter le scan si on skip ──────────────
    boolean[] scanRunning = {true};

    // ── Bouton skip → dialogue manuel ─────────────────────
    btnSkip.setOnAction(e -> {
        scanRunning[0] = false;
        splash.close();
        HOST = demanderIPManuellement();
        connecterServeur();
        if (socket != null) {
            rafraichirTout();
            demarrerUDP();
        }
    });

    // ── Scan en arrière-plan ───────────────────────────────
    Thread scanThread = new Thread(() -> {
        String found = null;
        try {
            String localIp = java.net.InetAddress
                .getLocalHost().getHostAddress();
            String base = localIp.substring(
                0, localIp.lastIndexOf('.') + 1);

            System.out.println("Scan réseau : " + base + "0/24");
            System.out.println("IP locale   : " + localIp);

            for (int i = 1; i <= 254; i++) {

                // Arrêter si on a cliqué sur Skip
                if (!scanRunning[0]) return;

                String ip = base + i;

                // Ne pas se scanner soi-même
                if (ip.equals(localIp)) continue;

                final int step = i;
                Platform.runLater(() -> {
                    ipLabel.setText("Test : " + base + step);
                    bar.setProgress(step / 254.0);
                    pct.setText(step + " / 254");
                });

                try {
                    Socket s = new Socket();
                    s.connect(
                        new java.net.InetSocketAddress(ip, PORT),
                        80 // timeout 80ms
                    );
                    s.close();
                    found = ip;
                    System.out.println("✔ Serveur trouvé : " + ip);

                    final String serverIp = ip;
                    Platform.runLater(() ->
                        ipLabel.setText(
                            "✔ Serveur trouvé : " + serverIp));
                    break;
                } catch (IOException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Scan échoué : " + e.getMessage());
        }

        // ── Résultat du scan ───────────────────────────────
        final String serverIp = found;
        Platform.runLater(() -> {

            // Si on a déjà cliqué sur Skip → ne rien faire
            if (!scanRunning[0]) return;
            scanRunning[0] = false;
            splash.close();

            if (serverIp != null) {
                // ✅ Serveur trouvé automatiquement
                HOST = serverIp;
                System.out.println("✔ Connexion à : " + HOST);
                connecterServeur();
                if (socket != null) {
                    rafraichirTout();
                    demarrerUDP();
                }
            } else {
                // ❌ Rien trouvé → dialogue manuel
                System.out.println(
                    "⚠ Aucun serveur trouvé → dialogue manuel");
                HOST = demanderIPManuellement();
                connecterServeur();
                if (socket != null) {
                    rafraichirTout();
                    demarrerUDP();
                }
            }
        });
    });
    scanThread.setDaemon(true);
    scanThread.start();
}

    // ── Dialogue de connexion ──────────────────────────────────
   

    // ── Horloge ────────────────────────────────────────────────
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

    // ══════════════════════════════════════════════════════════
    // SIDEBAR
    // ══════════════════════════════════════════════════════════
   private VBox buildSidebar() {
    VBox sb = new VBox(0);
    sb.setPrefWidth(275);
    sb.setStyle("-fx-background-color:" + BG_CARD +
        ";-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");

    // ── Sections normales ───────────────────────────────────
    sb.getChildren().addAll(
        buildLogo(),
        buildSideStats(),
        buildNavSection(),
        buildAlertSection(),
        buildConnStatus()
    );

    // ── Spacer qui pousse le bouton vers le bas ─────────────
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    sb.getChildren().add(spacer);

    // ── Séparateur ──────────────────────────────────────────
    Separator sep = new Separator();
    sep.setStyle("-fx-background-color:" + BORDER + ";");
    sb.getChildren().add(sep);

    // ── Bouton Quitter visible en bas ───────────────────────
    Button btnQuitter = new Button("⏻   Quitter l'application");
    btnQuitter.setMaxWidth(Double.MAX_VALUE);
    btnQuitter.setAlignment(Pos.CENTER_LEFT);
    btnQuitter.setFont(Font.font("Segoe UI", 13));
    btnQuitter.setPadding(new Insets(14, 18, 14, 18));
    btnQuitter.setCursor(javafx.scene.Cursor.HAND);

    String nQ = 
        "-fx-background-color:transparent;" +
        "-fx-text-fill:" + RED + ";" +
        "-fx-background-radius:0;";
    String hQ = 
        "-fx-background-color:" + RED + "15;" +
        "-fx-text-fill:" + RED + ";" +
        "-fx-background-radius:0;";

    btnQuitter.setStyle(nQ);
    btnQuitter.setOnMouseEntered(e -> btnQuitter.setStyle(hQ));
    btnQuitter.setOnMouseExited (e -> btnQuitter.setStyle(nQ));
    btnQuitter.setOnAction(e -> confirmerQuitter());

    sb.getChildren().add(btnQuitter);
    return sb;
}

    private VBox buildLogo() {
        VBox box = new VBox(0);
        box.setPadding(new Insets(26, 22, 20, 22));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        Rectangle iconBg = new Rectangle(46, 46);
        iconBg.setArcWidth(14); iconBg.setArcHeight(14);
        iconBg.setFill(Color.web(ACCENT));
        Label cross = new Label("✚");
        cross.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        cross.setTextFill(Color.WHITE);
        icon.getChildren().addAll(iconBg, cross);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.5), icon);
        pulse.setFromX(1.0); pulse.setToX(1.06);
        pulse.setFromY(1.0); pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        VBox texts = new VBox(3);
        Label name = new Label("PharmFlow Pro");
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 19));
        name.setTextFill(Color.web(TEXT));
        Label tag = new Label("Gestion Stocks Pharmacie");
        tag.setFont(Font.font("Segoe UI", 10));
        tag.setTextFill(Color.web(ACCENT));
        Label serverLbl = new Label("Serveur : " + HOST + ":" + PORT);
        serverLbl.setFont(Font.font("Segoe UI", 9));
        serverLbl.setTextFill(Color.web(TEXT_SUB));
        clockLabel = new Label("--:--:--");
        clockLabel.setFont(Font.font("Courier New", 10));
        clockLabel.setTextFill(Color.web(TEXT_SUB));
        texts.getChildren().addAll(name, tag, serverLbl, clockLabel);
        row.getChildren().addAll(icon, texts);
        box.getChildren().add(row);
        return box;
    }

    private VBox buildSideStats() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(18, 16, 18, 16));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        box.getChildren().add(sectionLabel("TABLEAU DE BORD"));

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
        VBox ct = new VBox(2);
        Label l1 = new Label("médicaments");
        l1.setFont(Font.font("Segoe UI", 11));
        l1.setTextFill(Color.web(TEXT_SUB));
        Label l2 = new Label("en stock");
        l2.setFont(Font.font("Segoe UI", 11));
        l2.setTextFill(Color.web(TEXT_SUB));
        ct.getChildren().addAll(l1, l2);
        countRow.getChildren().addAll(countLabel, ct);
        countCard.getChildren().add(countRow);

        HBox r1 = new HBox(8), r2 = new HBox(8), r3 = new HBox(8);
        VBox sVal   = miniStat("Valeur stock",   "0 DA",  BLUE);
        VBox sAlert = miniStat("Stock critique", "0",     RED);
        VBox sMax   = miniStat("Prix max",       "—",     GREEN);
        VBox sMin   = miniStat("Prix min",       "—",     ORANGE);
        VBox sEntr  = miniStat("Entrées",        "0",     GREEN);
        VBox sSort  = miniStat("Sorties",        "0",     ACCENT2);

        valStatVal     = (Label) sVal.getUserData();
        alertStatVal   = (Label) sAlert.getUserData();
        maxStatVal     = (Label) sMax.getUserData();
        minStatVal     = (Label) sMin.getUserData();
        entreesStatVal = (Label) sEntr.getUserData();
        sortiesStatVal = (Label) sSort.getUserData();

        r1.getChildren().addAll(sVal, sAlert);
        r2.getChildren().addAll(sMax, sMin);
        r3.getChildren().addAll(sEntr, sSort);
        box.getChildren().addAll(countCard, r1, r2, r3);
        return box;
    }

 private VBox buildNavSection() {
    // ── Conteneur principal sans VGrow ─────────────────────
    VBox box = new VBox(3);
    box.setPadding(new Insets(16, 14, 16, 14));
    box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

    // ── Navigation ──────────────────────────────────────────
    box.getChildren().add(sectionLabel("NAVIGATION"));
    box.getChildren().addAll(
        navBtn("📦   Stock & Inventaire",    ACCENT,  () -> tabPane.getSelectionModel().select(0)),
        navBtn("↕    Entrées / Sorties",     GREEN,   () -> tabPane.getSelectionModel().select(1)),
        navBtn("🚨   Alertes & Péremptions", RED,     () -> tabPane.getSelectionModel().select(2)),
        navBtn("📊   Tableau de bord",       PURPLE,  () -> tabPane.getSelectionModel().select(3))
    );

    // ── Actions rapides ─────────────────────────────────────
    box.getChildren().add(sectionLabel("ACTIONS RAPIDES"));
    box.getChildren().addAll(
        navBtn("＋   Ajouter médicament",    ACCENT,  this::ouvrirFormulaireAjout),
        navBtn("↻    Actualiser tout",       ACCENT2, this::rafraichirTout),
        navBtn("📤   Exporter CSV",          ORANGE,  this::exporterCSV)
    );

    return box;
}

    private VBox buildAlertSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        box.getChildren().add(sectionLabel("ALERTES CRITIQUES"));
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
        FadeTransition ft = new FadeTransition(Duration.seconds(1), statusDot);
        ft.setFromValue(1.0); ft.setToValue(0.2);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.play();
        statusLabel = new Label("Non connecté");
        statusLabel.setFont(Font.font("Segoe UI", 11));
        statusLabel.setTextFill(Color.web(TEXT_SUB));
        statusLabel.setWrapText(true);
        row.getChildren().addAll(statusDot, statusLabel);
        box.getChildren().add(row);
        return box;
    }
    // ══════════════════════════════════════════════════════════
    // CENTRE — MULTI-ONGLETS
    // ══════════════════════════════════════════════════════════
    private VBox buildCenter() {
        VBox center = new VBox(0);
        center.setStyle("-fx-background-color:" + BG + ";");

        // Header
        HBox header = new HBox(14);
        header.setPadding(new Insets(20, 28, 16, 28));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");

        VBox titleBox = new VBox(4);
        Label title = new Label("💊  PharmFlow — Gestion Complète");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(TEXT));
        Label subtitle = new Label("Stock • Mouvements • Alertes • Péremptions • Dashboard");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(TEXT_SUB));
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher...");
        searchField.setPrefWidth(220);
        searchField.setFont(Font.font("Segoe UI", 13));
        applyFieldStyle(searchField);
        searchField.textProperty().addListener((o, old, v) -> appliquerFiltres());

        filterCat = new ComboBox<>();
        filterCat.setPromptText("Catégorie");
        filterCat.setPrefWidth(140);
        applyComboStyle(filterCat);
        filterCat.setOnAction(e -> appliquerFiltres());

        sortBox = new ComboBox<>();
        sortBox.setItems(FXCollections.observableArrayList(
            "Tri par défaut","Nom ↑","Nom ↓",
            "Prix ↑","Prix ↓","Quantité ↑","Quantité ↓",
            "Expiration ↑","Expiration ↓"));
        sortBox.setValue("Tri par défaut");
        sortBox.setPrefWidth(150);
        applyComboStyle(sortBox);
        sortBox.setOnAction(e -> appliquerTri());

        header.getChildren().addAll(titleBox, searchField, filterCat, sortBox);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:" + BG + ";-fx-tab-min-height:40px;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(
            buildTabStock(),
            buildTabMouvements(),
            buildTabAlertes(),
            buildTabDashboard(),
            buildTabRMI()
        );

      // Barre notification UDP
udpNotifLabel = new Label("");
udpNotifLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
udpNotifLabel.setTextFill(Color.web(GREEN));
udpNotifLabel.setPadding(new Insets(6, 20, 6, 20));
udpNotifLabel.setMaxWidth(Double.MAX_VALUE);
udpNotifLabel.setVisible(false);
udpNotifLabel.setStyle(
    "-fx-background-color:#0a0a20;" +
    "-fx-border-color:" + BORDER + ";" +
    "-fx-border-width:1 0 0 0;"
);

center.getChildren().addAll(header, tabPane, udpNotifLabel);
        VBox.setVgrow(center, Priority.ALWAYS);
        return center;
    }

    // ══════════════════════════════════════════════════════════
    // ONGLET 1 — STOCK
    // ══════════════════════════════════════════════════════════
    private Tab buildTabStock() {
        Tab tab = new Tab("  📦  Stock & Inventaire  ");

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 24, 18, 24));
        content.setStyle("-fx-background-color:" + BG + ";");
        VBox.setVgrow(content, Priority.ALWAYS);

        chartBox = new VBox(10);
        chartBox.setPadding(new Insets(14, 18, 14, 18));
        chartBox.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );
        Label chartTitle = new Label("📊  Répartition par catégorie");
        chartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        chartTitle.setTextFill(Color.web(TEXT));
        chartBox.getChildren().add(chartTitle);

        buildTableStock();

        VBox tableCard = buildTableCard(
            tableProduits,
            "🗂  Inventaire complet des médicaments",
            "💡  Double-clic pour modifier  •  Boutons inline pour actions rapides"
        );
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        content.getChildren().addAll(chartBox, tableCard);
        VBox.setVgrow(content, Priority.ALWAYS);

        tab.setContent(content);
        return tab;
    }

    // ══════════════════════════════════════════════════════════
    // ONGLET 2 — MOUVEMENTS
    // ══════════════════════════════════════════════════════════
    private Tab buildTabMouvements() {
        Tab tab = new Tab("  ↕  Entrées / Sorties  ");

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 24, 18, 24));
        content.setStyle("-fx-background-color:" + BG + ";");
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button btnEntree = bigBtn("📥  Enregistrer une Entrée", GREEN);
        Button btnSortie = bigBtn("📤  Enregistrer une Sortie", ORANGE);
        btnEntree.setOnAction(e -> ouvrirFormulaireMouvement(Mouvement.Type.ENTREE));
        btnSortie.setOnAction(e -> ouvrirFormulaireMouvement(Mouvement.Type.SORTIE));
        actions.getChildren().addAll(btnEntree, btnSortie);

        HBox statsRow = new HBox(12);
        VBox sEntr = miniStat("Total Entrées", "0", GREEN);
        VBox sSort = miniStat("Total Sorties", "0", ORANGE);
        VBox sMouv = miniStat("Mouvements",    "0", BLUE);
        mouvStatVal = (Label) sMouv.getUserData();
        HBox.setHgrow(sEntr, Priority.ALWAYS);
        HBox.setHgrow(sSort, Priority.ALWAYS);
        HBox.setHgrow(sMouv, Priority.ALWAYS);
        statsRow.getChildren().addAll(sEntr, sSort, sMouv);

        buildTableMouvements();
        VBox tableCard = buildTableCard(
            tableMouvements,
            "📋  Historique des mouvements de stock",
            "💡  Toutes les entrées et sorties sont enregistrées automatiquement"
        );
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        content.getChildren().addAll(actions, statsRow, tableCard);
        tab.setContent(content);
        return tab;
    }

    // ══════════════════════════════════════════════════════════
    // ONGLET 3 — ALERTES
    // ══════════════════════════════════════════════════════════
    private Tab buildTabAlertes() {
        Tab tab = new Tab("  🚨  Alertes  ");

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 24, 18, 24));
        content.setStyle("-fx-background-color:" + BG + ";");

        HBox filtres = new HBox(10);
        filtres.setAlignment(Pos.CENTER_LEFT);
        Button bExp   = filterBtn("🔴  Expirés",           RED);
        Button bBient = filterBtn("🟠  Expirent bientôt",  ORANGE);
        Button bCrit  = filterBtn("⚡  Stock critique",    YELLOW);
        Button bReapp = filterBtn("📦  À réapprovisionner",BLUE);
        bExp.setOnAction(e   -> chargerAlertes("expires"));
        bBient.setOnAction(e -> chargerAlertes("bientot"));
        bCrit.setOnAction(e  -> chargerAlertes("critique"));
        bReapp.setOnAction(e -> chargerAlertes("reappro"));
        filtres.getChildren().addAll(bExp, bBient, bCrit, bReapp);

        buildTableAlertes();
        VBox tableCard = buildTableCard(
            tableAlertes,
            "🚨  Médicaments nécessitant une attention immédiate",
            "💡  Cliquez sur un filtre pour afficher les alertes"
        );
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        content.getChildren().addAll(filtres, tableCard);
        tab.setContent(content);
        return tab;
    }

    // ══════════════════════════════════════════════════════════
    // ONGLET 4 — DASHBOARD
    // ══════════════════════════════════════════════════════════
    private Tab buildTabDashboard() {
        Tab tab = new Tab("  📊  Dashboard  ");

        dashboardBox = new VBox(16);
        dashboardBox.setPadding(new Insets(18, 24, 18, 24));
        dashboardBox.setStyle("-fx-background-color:" + BG + ";");

        Label titre = new Label("📊  Tableau de bord — KPIs Pharmacie");
        titre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titre.setTextFill(Color.web(TEXT));
        Label sous = new Label("Statistiques en temps réel de votre stock");
        sous.setFont(Font.font("Segoe UI", 12));
        sous.setTextFill(Color.web(TEXT_SUB));

        dashboardBox.getChildren().addAll(titre, sous);

        ScrollPane sp = new ScrollPane(dashboardBox);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:" + BG + ";-fx-border-color:transparent;");
        tab.setContent(sp);
        return tab;
    }

    // ══════════════════════════════════════════════════════════
    // TABLES
    // ══════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void buildTableStock() {
        // ── CORRECTION PRINCIPALE ──────────────────────────────
        tableProduits = new TableView<>();
        tableProduits.setItems(produitsFiltres);
        applyTableStyle(tableProduits);

        tableProduits.setRowFactory(tv -> {
            TableRow<Produit> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    ouvrirFormulaireModification();
            });
            return row;
        });

        TableColumn<Produit,Integer> colId   = col("ID",          "id",             55);
        TableColumn<Produit,String>  colNom  = col("Médicament",  "nom",            180);
        TableColumn<Produit,String>  colDci  = col("DCI",         "dci",            130);
        TableColumn<Produit,Double>  colPrix = col("Prix (DA)",   "prix",           110);
        TableColumn<Produit,Integer> colQte  = col("Quantité",    "quantite",       90);
        TableColumn<Produit,String>  colCat  = col("Catégorie",   "categorie",      120);
        TableColumn<Produit,String>  colFourn= col("Fournisseur", "fournisseur",    120);
        TableColumn<Produit,String>  colLot  = col("N° Lot",      "numeroLot",      100);

        // Quantité colorée
        colQte.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                try {
                    Produit p = getTableView().getItems().get(getIndex());
                    String color = p.stockCritique() ? RED : v < 15 ? ORANGE : GREEN;
                    HBox box = new HBox(6); box.setAlignment(Pos.CENTER);
                    Circle dot = new Circle(4, Color.web(color));
                    Label lbl = new Label(String.valueOf(v));
                    lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                    lbl.setTextFill(Color.web(color));
                    box.getChildren().addAll(dot, lbl);
                    setGraphic(box);
                } catch (Exception ex) { setText(String.valueOf(v)); }
            }
        });

        // Prix coloré
        colPrix.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("%,.2f DA", v));
                setAlignment(Pos.CENTER);
                setStyle("-fx-text-fill:" + BLUE + ";-fx-font-weight:bold;");
            }
        });

        // Expiration colorée
        TableColumn<Produit,String> colExp = new TableColumn<>("Expiration");
        colExp.setPrefWidth(110);
        colExp.setStyle("-fx-alignment:CENTER;");
        colExp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); setStyle(""); return; }
                try {
                    Produit p = getTableView().getItems().get(getIndex());
                    if (p.getDateExpiration() == null) {
                        setText("N/A");
                        setStyle("-fx-text-fill:" + TEXT_SUB + ";-fx-alignment:CENTER;");
                        return;
                    }
                    String color = p.estExpire() ? RED : p.expireBientot() ? ORANGE : GREEN;
                    setText(p.getDateExpiration()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;-fx-alignment:CENTER;");
                } catch (Exception ex) { setText("—"); }
            }
        });

        // Catégorie badge
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

        // Actions inline
        TableColumn<Produit,Void> colAct = new TableColumn<>("Actions");
        colAct.setPrefWidth(230);
        colAct.setCellFactory(c -> new TableCell<>() {
            final Button bMod  = actionBtn("✎ Modifier",  ACCENT);
            final Button bSup  = actionBtn("✕ Supprimer", RED);
            final Button bEntr = actionBtn("↑ Entrée",    GREEN);
            final Button bSort = actionBtn("↓ Sortie",    ORANGE);
            final HBox   box   = new HBox(4, bMod, bSup, bEntr, bSort);
            {
                box.setAlignment(Pos.CENTER);
                bMod.setOnAction(e -> {
                    tableProduits.getSelectionModel().select(getIndex());
                    ouvrirFormulaireModification();
                });
                bSup.setOnAction(e -> {
                    tableProduits.getSelectionModel().select(getIndex());
                    supprimerSelectionne();
                });
                bEntr.setOnAction(e -> {
                    tableProduits.getSelectionModel().select(getIndex());
                    ouvrirFormulaireMouvement(Mouvement.Type.ENTREE);
                });
                bSort.setOnAction(e -> {
                    tableProduits.getSelectionModel().select(getIndex());
                    ouvrirFormulaireMouvement(Mouvement.Type.SORTIE);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableProduits.getColumns().addAll(
            colId, colNom, colDci, colPrix,
            colQte, colCat, colFourn, colLot, colExp, colAct
        );
    }

    @SuppressWarnings("unchecked")
    private void buildTableMouvements() {
        tableMouvements = new TableView<>();
        tableMouvements.setItems(mouvements);
        applyTableStyle(tableMouvements);

        TableColumn<Mouvement,Integer> colId   = colM("ID",          "id",           50);
        TableColumn<Mouvement,String>  colDate = colM("Date & Heure","dateHeureStr", 160);
        TableColumn<Mouvement,String>  colNom  = colM("Médicament",  "produitNom",   180);
        TableColumn<Mouvement,String>  colType = colM("Type",        "typeLabel",    90);
        TableColumn<Mouvement,Integer> colQte  = colM("Quantité",    "quantite",     90);
        TableColumn<Mouvement,String>  colMotif= colM("Motif",       "motif",        180);
        TableColumn<Mouvement,String>  colOper = colM("Opérateur",   "operateur",    120);

        colType.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                boolean isEntree = v.equals("Entrée");
                Label badge = new Label((isEntree ? "↑ " : "↓ ") + v);
                badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                String color = isEntree ? GREEN : ORANGE;
                badge.setTextFill(Color.web(color));
                badge.setPadding(new Insets(2, 8, 2, 8));
                badge.setStyle(
                    "-fx-background-color:" + color + "22;" +
                    "-fx-background-radius:8;" +
                    "-fx-border-color:" + color + "44;" +
                    "-fx-border-radius:8;-fx-border-width:1;"
                );
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colQte.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                try {
                    Mouvement m = getTableView().getItems().get(getIndex());
                    String color = m.getType() == Mouvement.Type.ENTREE ? GREEN : ORANGE;
                    setText((m.getType() == Mouvement.Type.ENTREE ? "+" : "-") + v);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
                } catch (Exception ex) { setText(String.valueOf(v)); }
            }
        });

        tableMouvements.getColumns().addAll(
            colId, colDate, colNom, colType, colQte, colMotif, colOper
        );
    }

    @SuppressWarnings("unchecked")
    private void buildTableAlertes() {
        tableAlertes = new TableView<>();
        tableAlertes.setItems(alertes);
        applyTableStyle(tableAlertes);

        TableColumn<Produit,Integer> colId   = col("ID",         "id",             55);
        TableColumn<Produit,String>  colNom  = col("Médicament", "nom",            180);
        TableColumn<Produit,String>  colDci  = col("DCI",        "dci",            130);
        TableColumn<Produit,Integer> colQte  = col("Quantité",   "quantite",       90);
        TableColumn<Produit,Integer> colSeuil= col("Seuil",      "seuilReappro",   80);
        TableColumn<Produit,String>  colFourn= col("Fournisseur","fournisseur",    130);

        TableColumn<Produit,String> colExp = new TableColumn<>("Expiration");
        colExp.setPrefWidth(120);
        colExp.setStyle("-fx-alignment:CENTER;");
        colExp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); return; }
                try {
                    Produit p = getTableView().getItems().get(getIndex());
                    if (p.getDateExpiration() == null) { setText("N/A"); return; }
                    String color = p.estExpire() ? RED : ORANGE;
                    setText(p.getDateExpiration()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
                } catch (Exception ex) { setText("—"); }
            }
        });

        TableColumn<Produit,Void> colStatut = new TableColumn<>("Statut");
        colStatut.setPrefWidth(160);
        colStatut.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                try {
                    Produit p = getTableView().getItems().get(getIndex());
                    String txt, color;
                    if (p.estExpire())          { txt = "🔴 EXPIRÉ";            color = RED;    }
                    else if (p.expireBientot()) { txt = "🟠 EXPIRE BIENTÔT";    color = ORANGE; }
                    else if (p.stockCritique()) { txt = "⚡ STOCK CRITIQUE";     color = YELLOW; }
                    else                        { txt = "📦 À RÉAPPROVISIONNER"; color = BLUE;   }
                    Label badge = new Label(txt);
                    badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                    badge.setTextFill(Color.web(color));
                    badge.setPadding(new Insets(3, 10, 3, 10));
                    badge.setStyle(
                        "-fx-background-color:" + color + "22;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + color + "55;" +
                        "-fx-border-radius:8;-fx-border-width:1;"
                    );
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                } catch (Exception ex) { setGraphic(null); }
            }
        });

        tableAlertes.getColumns().addAll(
            colId, colNom, colDci, colQte, colSeuil, colFourn, colExp, colStatut
        );
    }
    // ══════════════════════════════════════════════════════════
    // CRUD & RÉSEAU
    // ══════════════════════════════════════════════════════════
   private void connecterServeur() {
    try {
        socket  = new Socket(HOST, PORT);
        out     = new PrintWriter(socket.getOutputStream(), true);
        in      = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        service = new ProduitService(out, in);
        Platform.runLater(() -> {
            statusDot.setFill(Color.web(GREEN));
            statusLabel.setText("Connecté — " + HOST + ":" + PORT);
            statusLabel.setTextFill(Color.web(GREEN));
        });
    } catch (ConnectException e) {
        statut("✘ Connexion échouée sur " + HOST + ":" + PORT);
        alerte("Connexion impossible",
            "Impossible de joindre le serveur sur :\n" +
            HOST + ":" + PORT + "\n\n" +
            "Vérifiez que :\n" +
            "• Server.java est démarré\n" +
            "• Le pare-feu autorise le port " + PORT + "\n" +
            "• Les deux machines sont sur le même réseau");
    } catch (IOException e) {
        statut("✘ Erreur : " + e.getMessage());
        System.err.println("Erreur connexion : " + e.getMessage());
    }
}

    // ── CORRECTION PRINCIPALE rafraichirTout ──────────────────
    private void rafraichirTout() {
        if (!connecte()) return;
        new Thread(() -> {
            try {
                List<Produit>   lp = service.getListe();
                List<Mouvement> lm = service.getListeMouvements();

                Platform.runLater(() -> {
                    // Mise à jour propre des listes observables
                    produits.setAll(lp);
                    mouvements.setAll(lm);

                    // Mise à jour UI
                    mettreAJourStats();
                    mettreAJourGraphique();
                    mettreAJourFiltreCategories();
                    mettreAJourDashboard();
                    statut("✔ " + lp.size() + " médicament(s) chargé(s)");
                });
            } catch (IOException e) {
                Platform.runLater(() -> statut("✘ " + e.getMessage()));
            }
        }).start();
    }

    private void ouvrirFormulaireAjout() {
        if (!connecte()) return;
        new FormulaireProduit(null, p -> {
            new Thread(() -> {
                try {
                    Produit added = service.ajouter(p);
                    if (added != null) {
                        Platform.runLater(() -> {
                            produits.add(added);
                            mettreAJourStats();
                            mettreAJourGraphique();
                            mettreAJourFiltreCategories();
                            mettreAJourDashboard();
                            statut("✔ Médicament ajouté");
                        });
                        rafraichirMouvements();
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> statut("✘ " + e.getMessage()));
                }
            }).start();
        }).show();
    }

    private void ouvrirFormulaireModification() {
        Produit sel = tableProduits.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alerte("Aucune sélection", "Sélectionnez un médicament à modifier.");
            return;
        }
        if (!connecte()) return;
        new FormulaireProduit(sel, p -> {
            p.setId(sel.getId());
            new Thread(() -> {
                try {
                    Produit maj = service.modifier(p);
                    if (maj != null) {
                        Platform.runLater(() -> {
                            int idx = produits.indexOf(sel);
                            if (idx >= 0) produits.set(idx, maj);
                            mettreAJourStats();
                            mettreAJourGraphique();
                            mettreAJourDashboard();
                            statut("✔ Médicament modifié");
                        });
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> statut("✘ " + e.getMessage()));
                }
            }).start();
        }).show();
    }

    private void supprimerSelectionne() {
        Produit sel = tableProduits.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alerte("Aucune sélection", "Sélectionnez un médicament.");
            return;
        }
        if (!connecte()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer \"" + sel.getNom() + "\" ?");
        confirm.setContentText(
            "Stock actuel : " + sel.getQuantite() + " unité(s)\n" +
            "Cette action est irréversible."
        );
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        if (service.supprimer(sel.getId())) {
                            Platform.runLater(() -> {
                                produits.remove(sel);
                                mettreAJourStats();
                                mettreAJourGraphique();
                                mettreAJourFiltreCategories();
                                mettreAJourDashboard();
                                statut("✔ Médicament supprimé");
                            });
                            rafraichirMouvements();
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> statut("✘ " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void ouvrirFormulaireMouvement(Mouvement.Type type) {
        Produit sel = tableProduits.getSelectionModel().getSelectedItem();
        new FormulaireMouvement(type, sel, produits, (produitId, qte, motif, op) -> {
            if (!connecte()) return;
            new Thread(() -> {
                try {
                    Object[] res = type == Mouvement.Type.ENTREE
                        ? service.entreeStock(produitId, qte, motif, op)
                        : service.sortieStock(produitId, qte, motif, op);
                    if (res != null) {
                        Produit   updated = (Produit)   res[0];
                        Mouvement m       = (Mouvement) res[1];
                        Platform.runLater(() -> {
                            // Mettre à jour le produit dans la liste
                            for (int i = 0; i < produits.size(); i++) {
                                if (produits.get(i).getId() == updated.getId()) {
                                    produits.set(i, updated);
                                    break;
                                }
                            }
                            mouvements.add(m);
                            mettreAJourStats();
                            mettreAJourDashboard();
                            statut("✔ Mouvement enregistré");
                        });
                    } else {
                        Platform.runLater(() ->
                            statut("✘ Opération impossible (stock insuffisant ?)"));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> statut("✘ " + e.getMessage()));
                }
            }).start();
        }).show();
    }

    private void chargerAlertes(String type) {
        if (!connecte()) return;
        new Thread(() -> {
            try {
                List<Produit> liste = switch (type) {
                    case "expires"  -> service.getExpires();
                    case "bientot"  -> service.getExpireBientot();
                    case "critique" -> service.getStockCritique();
                    case "reappro"  -> service.getAReapprovisionner();
                    default         -> new ArrayList<>();
                };
                Platform.runLater(() -> {
                    alertes.setAll(liste);
                    statut("✔ " + liste.size() + " alerte(s) chargée(s)");
                });
            } catch (IOException e) {
                Platform.runLater(() -> statut("✘ " + e.getMessage()));
            }
        }).start();
    }

    private void rafraichirMouvements() throws IOException {
        List<Mouvement> lm = service.getListeMouvements();
        Platform.runLater(() -> mouvements.setAll(lm));
    }

    // ══════════════════════════════════════════════════════════
    // STATS
    // ══════════════════════════════════════════════════════════
    private void mettreAJourStats() {
        long   alerts   = produits.stream()
            .filter(p -> p.stockCritique() || p.estExpire() || p.expireBientot())
            .count();
        double valTotal = produits.stream()
            .mapToDouble(p -> p.getPrix() * p.getQuantite()).sum();
        double prixMax  = produits.stream()
            .mapToDouble(Produit::getPrix).max().orElse(0);
        double prixMin  = produits.stream()
            .mapToDouble(Produit::getPrix).min().orElse(0);
        int entrees = mouvements.stream()
            .filter(m -> m.getType() == Mouvement.Type.ENTREE)
            .mapToInt(Mouvement::getQuantite).sum();
        int sorties = mouvements.stream()
            .filter(m -> m.getType() == Mouvement.Type.SORTIE)
            .mapToInt(Mouvement::getQuantite).sum();

        animateCount(countLabel, produits.size());
        valStatVal.setText(String.format("%,.0f DA", valTotal));
        alertStatVal.setText(String.valueOf(alerts));
        maxStatVal.setText(String.format("%,.0f DA", prixMax));
        minStatVal.setText(String.format("%,.0f DA", prixMin));
        entreesStatVal.setText(String.valueOf(entrees));
        sortiesStatVal.setText(String.valueOf(sorties));
        if (mouvStatVal != null) mouvStatVal.setText(String.valueOf(mouvements.size()));

        if (alerts > 0) {
            alertLabel.setText("⚠  " + alerts + " alerte(s) active(s) !");
            alertLabel.setTextFill(Color.web(RED));
            FadeTransition ft = new FadeTransition(Duration.millis(500), alertLabel);
            ft.setFromValue(1.0); ft.setToValue(0.2);
            ft.setAutoReverse(true); ft.setCycleCount(6);
            ft.play();
        } else {
            alertLabel.setText("✔  Stock en bon état");
            alertLabel.setTextFill(Color.web(GREEN));
        }
    }

    private void animateCount(Label label, int target) {
        int from;
        try { from = Integer.parseInt(label.getText()); }
        catch (Exception e) { from = 0; }
        int start = from;
        Timeline tl = new Timeline();
        for (int i = 0; i <= 12; i++) {
            int step = i;
            int val  = start + (int)((target - start) * step / 12.0);
            tl.getKeyFrames().add(
                new KeyFrame(Duration.millis(35 * i),
                    e -> label.setText(String.valueOf(val)))
            );
        }
        tl.play();
    }

    // ══════════════════════════════════════════════════════════
    // FILTRES & TRI
    // ══════════════════════════════════════════════════════════
    private void appliquerFiltres() {
        String s = searchField.getText().toLowerCase().trim();
        String c = filterCat.getValue();
        produitsFiltres.setPredicate(p ->
            (s.isEmpty()
                || p.getNom().toLowerCase().contains(s)
                || p.getDci().toLowerCase().contains(s)
                || p.getCategorie().toLowerCase().contains(s)
                || p.getFournisseur().toLowerCase().contains(s)
                || p.getNumeroLot().toLowerCase().contains(s)
                || String.valueOf(p.getId()).contains(s))
            && (c == null || c.equals("Toutes") || p.getCategorie().equals(c))
        );
    }

    private void appliquerTri() {
        if (sortBox.getValue() == null) return;
        List<Produit> liste = new ArrayList<>(produits);
        switch (sortBox.getValue()) {
            case "Nom ↑"        -> liste.sort(Comparator.comparing(Produit::getNom));
            case "Nom ↓"        -> liste.sort(Comparator.comparing(Produit::getNom).reversed());
            case "Prix ↑"       -> liste.sort(Comparator.comparingDouble(Produit::getPrix));
            case "Prix ↓"       -> liste.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
            case "Quantité ↑"   -> liste.sort(Comparator.comparingInt(Produit::getQuantite));
            case "Quantité ↓"   -> liste.sort(Comparator.comparingInt(Produit::getQuantite).reversed());
            case "Expiration ↑" -> liste.sort(Comparator.comparing(
                p -> p.getDateExpiration() == null ? LocalDate.MAX : p.getDateExpiration()));
            case "Expiration ↓" -> liste.sort((a, b) -> {
                LocalDate da = a.getDateExpiration() == null ? LocalDate.MIN : a.getDateExpiration();
                LocalDate db = b.getDateExpiration() == null ? LocalDate.MIN : b.getDateExpiration();
                return db.compareTo(da);
            });
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
    // GRAPHIQUE
    // ══════════════════════════════════════════════════════════
    private void mettreAJourGraphique() {
        while (chartBox.getChildren().size() > 1)
            chartBox.getChildren().remove(1);
        if (produits.isEmpty()) return;

        var parCat = produits.stream().collect(
            Collectors.groupingBy(Produit::getCategorie, Collectors.counting()));
        long max = parCat.values().stream().mapToLong(Long::longValue).max().orElse(1);
        String[] colors = {ACCENT, GREEN, ORANGE, BLUE, PURPLE, ACCENT2, RED, YELLOW};
        int ci = 0;

        for (var entry : parCat.entrySet()) {
            String color = colors[ci++ % colors.length];
            double ratio = (double) entry.getValue() / max;

            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label(entry.getKey());
            lbl.setFont(Font.font("Segoe UI", 11));
            lbl.setTextFill(Color.web(TEXT_SUB));
            lbl.setMinWidth(120);

            StackPane wrap = new StackPane();
            wrap.setStyle("-fx-background-color:#0d0d22;-fx-background-radius:4;");
            wrap.setMinHeight(11); wrap.setMaxHeight(11);
            HBox.setHgrow(wrap, Priority.ALWAYS);
            wrap.setAlignment(Pos.CENTER_LEFT);

            Rectangle bar = new Rectangle(0, 11);
            bar.setFill(Color.web(color));
            bar.setArcWidth(4); bar.setArcHeight(4);
            wrap.getChildren().add(bar);

            Label cnt = new Label(entry.getValue() + " réf.");
            cnt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            cnt.setTextFill(Color.web(color));
            cnt.setMinWidth(65);

            row.getChildren().addAll(lbl, wrap, cnt);
            chartBox.getChildren().add(row);

            wrap.widthProperty().addListener((obs, old, w) -> {
                if (w.doubleValue() > 0) {
                    Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                            new KeyValue(bar.widthProperty(), 0)),
                        new KeyFrame(Duration.millis(700),
                            new KeyValue(bar.widthProperty(),
                                w.doubleValue() * ratio, Interpolator.EASE_OUT))
                    );
                    tl.play();
                }
            });
        }
    }

    // ══════════════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════════════
    private void mettreAJourDashboard() {
        while (dashboardBox.getChildren().size() > 2)
            dashboardBox.getChildren().remove(2);

        // KPIs
        HBox kpiRow = new HBox(14);
        kpiRow.getChildren().addAll(
            kpiCard("💊 Total médicaments",
                String.valueOf(produits.size()), ACCENT),
            kpiCard("💰 Valeur totale",
                String.format("%,.0f DA",
                    produits.stream().mapToDouble(p -> p.getPrix() * p.getQuantite()).sum()), BLUE),
            kpiCard("🔴 Expirés",
                String.valueOf(produits.stream().filter(Produit::estExpire).count()), RED),
            kpiCard("🟠 Expirent bientôt",
                String.valueOf(produits.stream().filter(Produit::expireBientot).count()), ORANGE),
            kpiCard("⚡ Stock critique",
                String.valueOf(produits.stream().filter(Produit::stockCritique).count()), YELLOW),
            kpiCard("↕ Mouvements",
                String.valueOf(mouvements.size()), GREEN)
        );

        // Graphique catégories dashboard
        VBox catChart = new VBox(10);
        catChart.setPadding(new Insets(16, 18, 16, 18));
        catChart.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );
        Label catTitle = new Label("📊  Répartition du stock par catégorie");
        catTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        catTitle.setTextFill(Color.web(TEXT));
        catChart.getChildren().add(catTitle);

        var parCat = produits.stream().collect(
            Collectors.groupingBy(Produit::getCategorie, Collectors.counting()));
        long max = parCat.values().stream().mapToLong(Long::longValue).max().orElse(1);
        String[] colors = {ACCENT, GREEN, ORANGE, BLUE, PURPLE, ACCENT2, RED, YELLOW};
        int ci = 0;
        for (var entry : parCat.entrySet()) {
            String color = colors[ci++ % colors.length];
            double ratio = (double) entry.getValue() / max;
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label(entry.getKey());
            lbl.setFont(Font.font("Segoe UI", 11));
            lbl.setTextFill(Color.web(TEXT_SUB));
            lbl.setMinWidth(130);
            StackPane wrap = new StackPane();
            wrap.setStyle("-fx-background-color:#0d0d22;-fx-background-radius:4;");
            wrap.setMinHeight(12); wrap.setMaxHeight(12);
            HBox.setHgrow(wrap, Priority.ALWAYS);
            wrap.setAlignment(Pos.CENTER_LEFT);
            Rectangle bar = new Rectangle(0, 12);
            bar.setFill(Color.web(color));
            bar.setArcWidth(4); bar.setArcHeight(4);
            wrap.getChildren().add(bar);
            Label cnt = new Label(entry.getValue() + " réf.");
            cnt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            cnt.setTextFill(Color.web(color));
            cnt.setMinWidth(70);
            row.getChildren().addAll(lbl, wrap, cnt);
            catChart.getChildren().add(row);
            wrap.widthProperty().addListener((obs, old, w) -> {
                if (w.doubleValue() > 0) {
                    new Timeline(
                        new KeyFrame(Duration.ZERO,
                            new KeyValue(bar.widthProperty(), 0)),
                        new KeyFrame(Duration.millis(700),
                            new KeyValue(bar.widthProperty(),
                                w.doubleValue() * ratio, Interpolator.EASE_OUT))
                    ).play();
                }
            });
        }

        // Top 5 plus chers
        VBox top5 = new VBox(10);
        top5.setPadding(new Insets(16, 18, 16, 18));
        top5.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );
        Label top5Title = new Label("🏆  Top 5 — Médicaments les plus chers");
        top5Title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        top5Title.setTextFill(Color.web(TEXT));
        top5.getChildren().add(top5Title);
        produits.stream()
            .sorted(Comparator.comparingDouble(Produit::getPrix).reversed())
            .limit(5)
            .forEach(p -> {
                HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));
                Label nom = new Label(p.getNom());
                nom.setFont(Font.font("Segoe UI", 12));
                nom.setTextFill(Color.web(TEXT));
                HBox.setHgrow(nom, Priority.ALWAYS);
                Label cat = new Label(p.getCategorie());
                cat.setFont(Font.font("Segoe UI", 10));
                cat.setTextFill(Color.web(TEXT_SUB));
                Label prix = new Label(String.format("%,.2f DA", p.getPrix()));
                prix.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                prix.setTextFill(Color.web(BLUE));
                row.getChildren().addAll(nom, cat, prix);
                top5.getChildren().add(row);
            });

        HBox charts = new HBox(14, catChart, top5);
        HBox.setHgrow(catChart, Priority.ALWAYS);
        HBox.setHgrow(top5,    Priority.ALWAYS);

        dashboardBox.getChildren().addAll(kpiRow, charts);
    }

    private VBox kpiCard(String label, String value, String color) {
        VBox v = new VBox(6);
        v.setPadding(new Insets(16, 18, 16, 18));
        v.setStyle(
            "-fx-background-color:" + color + "15;" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + color + "50;" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );
        HBox.setHgrow(v, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setTextFill(Color.web(color));
        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        val.setTextFill(Color.web(TEXT));
        v.getChildren().addAll(lbl, val);
        return v;
    }

    // ══════════════════════════════════════════════════════════
    // EXPORT CSV
    // ══════════════════════════════════════════════════════════
    private void exporterCSV() {
        if (produits.isEmpty()) {
            alerte("Export impossible", "Aucun médicament à exporter.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le stock");
        fc.setInitialFileName("pharmflow_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            + ".csv");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showSaveDialog(tableProduits.getScene().getWindow());
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("ID,Médicament,DCI,Prix(DA),Quantité,Catégorie," +
                       "Fournisseur,N°Lot,Expiration,Seuil,Valeur(DA),Statut");
            for (Produit p : produits) {
                String statut = p.estExpire()       ? "EXPIRÉ"
                              : p.expireBientot()   ? "EXPIRE BIENTÔT"
                              : p.stockCritique()   ? "CRITIQUE"
                              : "OK";
                pw.printf("%d,%s,%s,%.2f,%d,%s,%s,%s,%s,%d,%.2f,%s%n",
                    p.getId(), p.getNom(), p.getDci(),
                    p.getPrix(), p.getQuantite(),
                    p.getCategorie(), p.getFournisseur(),
                    p.getNumeroLot(),
                    p.getDateExpiration() != null
                        ? p.getDateExpiration().toString() : "",
                    p.getSeuilReappro(),
                    p.getPrix() * p.getQuantite(),
                    statut);
            }
            statut("✔ Exporté : " + file.getName());
            info("Export réussi !", "Fichier :\n" + file.getAbsolutePath());
        } catch (IOException e) {
            alerte("Erreur export", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    // UTILITAIRES UI
    // ══════════════════════════════════════════════════════════
    private boolean connecte() {
        if (socket == null || socket.isClosed()) {
            connecterServeur();
            return socket != null && !socket.isClosed();
        }
        return true;
    }

    private void statut(String msg) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setTextFill(Color.web(msg.startsWith("✔") ? GREEN : RED));
            statusDot.setFill(Color.web(msg.startsWith("✔") ? GREEN : RED));
        });
    }

    private void alerte(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING, m, ButtonType.OK);
        a.setTitle(t); a.showAndWait();
    }

    private void info(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK);
        a.setTitle(t); a.showAndWait();
    }

    private Label sectionLabel(String txt) {
        Label l = new Label(txt);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        l.setTextFill(Color.web(TEXT_SUB));
        l.setPadding(new Insets(4, 0, 4, 0));
        return l;
    }

    private VBox miniStat(String label, String val, String color) {
        VBox v = new VBox(3);
        v.setPadding(new Insets(10, 12, 10, 12));
        v.setStyle(
            "-fx-background-color:" + color + "15;" +
            "-fx-background-radius:10;" +
            "-fx-border-color:" + color + "40;" +
            "-fx-border-radius:10;-fx-border-width:1;"
        );
        HBox.setHgrow(v, Priority.ALWAYS);
        Label lb = new Label(label);
        lb.setFont(Font.font("Segoe UI", 9));
        lb.setTextFill(Color.web(color));
        Label vl = new Label(val);
        vl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        vl.setTextFill(Color.web(TEXT));
        v.getChildren().addAll(lb, vl);
        v.setUserData(vl);
        return v;
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

    private Button bigBtn(String txt, String color) {
        Button b = new Button(txt);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        b.setPadding(new Insets(11, 22, 11, 22));
        b.setCursor(javafx.scene.Cursor.HAND);
        String n = "-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";-fx-background-radius:10;-fx-border-color:" + color + "55;-fx-border-radius:10;";
        String h = "-fx-background-color:" + color + "44;-fx-text-fill:" + color + ";-fx-background-radius:10;-fx-border-color:" + color + ";-fx-border-radius:10;";
        b.setStyle(n);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited (e -> b.setStyle(n));
        return b;
    }

    private Button filterBtn(String txt, String color) {
        Button b = new Button(txt);
        b.setFont(Font.font("Segoe UI", 12));
        b.setPadding(new Insets(8, 16, 8, 16));
        b.setCursor(javafx.scene.Cursor.HAND);
        String n = "-fx-background-color:" + color + "20;-fx-text-fill:" + color + ";-fx-background-radius:8;-fx-border-color:" + color + "50;-fx-border-radius:8;";
        String h = "-fx-background-color:" + color + "50;-fx-text-fill:" + color + ";-fx-background-radius:8;-fx-border-color:" + color + ";-fx-border-radius:8;";
        b.setStyle(n);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited (e -> b.setStyle(n));
        return b;
    }

    private Button actionBtn(String txt, String color) {
        Button b = new Button(txt);
        b.setFont(Font.font("Segoe UI", 10));
        b.setPadding(new Insets(3, 8, 3, 8));
        b.setCursor(javafx.scene.Cursor.HAND);
        String n = "-fx-background-color:" + color + "20;-fx-text-fill:" + color + ";-fx-background-radius:5;-fx-border-color:" + color + "50;-fx-border-radius:5;";
        String h = "-fx-background-color:" + color + "50;-fx-text-fill:" + color + ";-fx-background-radius:5;-fx-border-color:" + color + ";-fx-border-radius:5;";
        b.setStyle(n);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited (e -> b.setStyle(n));
        return b;
    }

    private <T> TableColumn<Produit,T> col(String t, String p, double w) {
        TableColumn<Produit,T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(p));
        c.setPrefWidth(w);
        c.setStyle("-fx-alignment:CENTER;");
        return c;
    }

    private <T> TableColumn<Mouvement,T> colM(String t, String p, double w) {
        TableColumn<Mouvement,T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(p));
        c.setPrefWidth(w);
        c.setStyle("-fx-alignment:CENTER;");
        return c;
    }

    private void applyTableStyle(TableView<?> t) {
        t.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-table-cell-border-color:" + BORDER + ";" +
            "-fx-control-inner-background:" + BG_CARD + ";" +
            "-fx-control-inner-background-alt:" + BG_ROW + ";" +
            "-fx-border-color:transparent;"
        );
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label ph = new Label("Aucune donnée");
        ph.setTextFill(Color.web(TEXT_SUB));
        t.setPlaceholder(ph);
    }

    private VBox buildTableCard(Control table, String title, String footer) {
        VBox card = new VBox();
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card,  Priority.ALWAYS);
        card.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:14;-fx-border-width:1;"
        );
        HBox header = new HBox();
        header.setPadding(new Insets(12, 18, 10, 18));
        header.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        titleLbl.setTextFill(Color.web(TEXT));
        header.getChildren().add(titleLbl);
        HBox foot = new HBox();
        foot.setPadding(new Insets(6, 18, 6, 18));
        foot.setStyle("-fx-border-color:" + BORDER + ";-fx-border-width:1 0 0 0;");
        Label footLbl = new Label(footer);
        footLbl.setFont(Font.font("Segoe UI", 10));
        footLbl.setTextFill(Color.web(TEXT_SUB));
        foot.getChildren().add(footLbl);
        card.getChildren().addAll(header, table, foot);
        return card;
    }

    private void applyFieldStyle(TextField tf) {
        String base =
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-text-fill:" + TEXT + ";" +
            "-fx-prompt-text-fill:" + TEXT_SUB + ";" +
            "-fx-border-radius:10;-fx-background-radius:10;" +
            "-fx-padding:8 14;";
        tf.setStyle(base + "-fx-border-color:" + BORDER + ";");
        tf.focusedProperty().addListener((o, old, f) ->
            tf.setStyle(base + (f
                ? "-fx-border-color:" + ACCENT + ";"
                : "-fx-border-color:" + BORDER + ";")));
    }

    private void applyComboStyle(ComboBox<?> cb) {
        cb.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:10;-fx-background-radius:10;" +
            "-fx-text-fill:" + TEXT + ";"
        );
    }

  @Override
public void stop() throws Exception {
    if (udpClient != null) udpClient.arreter();
    if (service   != null) service.quitter();
    if (socket    != null && !socket.isClosed()) socket.close();
}

    public static void main(String[] args) { launch(args); }


private static String getServerHost() {
    return "localhost"; // remplacé dynamiquement au démarrage
}

private void confirmerQuitter() {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Quitter PharmFlow");
    confirm.setHeaderText("Voulez-vous vraiment quitter ?");
    confirm.setContentText("La connexion au serveur sera fermée.");

    // Style sombre
    confirm.getDialogPane().setStyle(
        "-fx-background-color:" + BG_CARD + ";"
    );

    confirm.showAndWait().ifPresent(bt -> {
        if (bt == ButtonType.OK) {
            try {
                if (service != null) service.quitter();
                if (socket  != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
            Platform.exit();
            System.exit(0);
        }
    });
}

private void demarrerUDP() {
    udpClient = new UDPNotificationClient(
        new UDPNotificationClient.NotificationCallback() {

            @Override
            public void onStockUpdate(String action,
                    String medicament, String details) {
                // Rafraîchir la liste automatiquement
                rafraichirTout();

                // Afficher notification
                String msg = switch (action) {
                    case "AJOUT"       -> "➕ Ajouté : " + medicament;
                    case "MODIF"       -> "✎ Modifié : " + medicament;
                    case "SUPPRESSION" -> "✕ Supprimé : " + medicament;
                    default            -> "🔄 Stock mis à jour";
                };
                afficherNotifUDP(msg, GREEN);
            }

            @Override
            public void onMouvement(String type,
                    String medicament, int quantite) {
                // Rafraîchir mouvements automatiquement
                new Thread(() -> {
                    try {
                        List<Mouvement> lm =
                            service.getListeMouvements();
                        Platform.runLater(() ->
                            mouvements.setAll(lm));
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }).start();

                String msg = type.equals("ENTREE")
                    ? "📥 Entrée : " + medicament
                        + " +" + quantite
                    : "📤 Sortie : " + medicament
                        + " -" + quantite;
                afficherNotifUDP(msg, ACCENT);
            }

            @Override
            public void onAlerte(String type,
                    String medicament, String details) {
                String msg = switch (type) {
                    case "CRITIQUE"   ->
                        "⚡ Stock critique : " + medicament;
                    case "EXPIRATION" ->
                        "🟠 Expire bientôt : " + medicament;
                    case "EXPIRÉ"     ->
                        "🔴 EXPIRÉ : " + medicament;
                    default ->
                        "⚠ Alerte : " + medicament;
                };
                afficherNotifUDP(msg, RED);

                // Aller sur l'onglet alertes automatiquement
                tabPane.getSelectionModel().select(2);
                chargerAlertes("critique");
            }
        }
    );

    Thread udpThread = new Thread(udpClient, "UDP-Client");
    udpThread.setDaemon(true);
    udpThread.start();
    System.out.println("✔ Client UDP démarré");
}

private void afficherNotifUDP(String message, String color) {
    Platform.runLater(() -> {
        // Créer une notification flottante en bas
        if (udpNotifLabel == null) return;
        udpNotifLabel.setText("📡  " + message);
        udpNotifLabel.setTextFill(Color.web(color));
        udpNotifLabel.setVisible(true);

        // Disparaît après 4 secondes
        Timeline hide = new Timeline(
            new KeyFrame(Duration.seconds(4),
                e -> udpNotifLabel.setVisible(false))
        );
        hide.play();
    });
}
private void demarrerRMI() {
    new Thread(() -> {
        rmiClient = new main.client.RMIClient(HOST);
        boolean ok = rmiClient.connecter();
        Platform.runLater(() -> {
            if (ok) statut("✔ RMI connecté — port 1099");
            else    statut("⚠ RMI non disponible");
        });
    }).start();
}

private Tab buildTabRMI() {
    Tab tab = new Tab("  📡  RMI  ");

    VBox content = new VBox(16);
    content.setPadding(new Insets(18, 24, 18, 24));
    content.setStyle("-fx-background-color:" + BG + ";");

    // ── Header ─────────────────────────────────────────
    Label titre = new Label("📡  Rapports via RMI");
    titre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
    titre.setTextFill(Color.web(TEXT));
    Label sous = new Label(
        "Statistiques avancées par appel de méthode distante");
    sous.setFont(Font.font("Segoe UI", 12));
    sous.setTextFill(Color.web(TEXT_SUB));

    // ── Statut RMI ─────────────────────────────────────
    Label rmiStatut = new Label("⏳ Connexion RMI en cours...");
    rmiStatut.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
    rmiStatut.setTextFill(Color.web(ACCENT));

    // ── Boutons ────────────────────────────────────────
    HBox btnRow = new HBox(12);
    btnRow.setAlignment(Pos.CENTER_LEFT);

    Button btnPing  = bigBtn("🔌  Ping RMI",        ACCENT);
    Button btnStats = bigBtn("📊  Statistiques",    BLUE);
    Button btnTop5  = bigBtn("🏆  Top 5 prix",      PURPLE);
    Button btnAlert = bigBtn("🚨  Alertes RMI",     RED);

    btnRow.getChildren().addAll(
        btnPing, btnStats, btnTop5, btnAlert);

    // ── Zone résultat ──────────────────────────────────
    VBox resultBox = new VBox(10);
    resultBox.setPadding(new Insets(16, 18, 16, 18));
    resultBox.setStyle(
        "-fx-background-color:" + BG_CARD + ";" +
        "-fx-background-radius:14;" +
        "-fx-border-color:" + BORDER + ";" +
        "-fx-border-radius:14;-fx-border-width:1;"
    );
    Label resultTitle = new Label("Résultats RMI");
    resultTitle.setFont(
        Font.font("Segoe UI", FontWeight.BOLD, 13));
    resultTitle.setTextFill(Color.web(TEXT));
    resultBox.getChildren().add(resultTitle);
    VBox.setVgrow(resultBox, Priority.ALWAYS);

    // ── Actions boutons ────────────────────────────────
    btnPing.setOnAction(e -> {
        if (rmiClient == null || !rmiClient.isConnecte()) {
            demarrerRMI();
            rmiStatut.setText("🔄 Reconnexion...");
            rmiStatut.setTextFill(Color.web(ORANGE));
            return;
        }
        new Thread(() -> {
            String rep = rmiClient.ping();
            Platform.runLater(() -> {
                resultBox.getChildren().clear();
                resultBox.getChildren().add(resultTitle);
                Label r = new Label(rep);
                r.setFont(Font.font("Segoe UI", 13));
                r.setTextFill(Color.web(GREEN));
                resultBox.getChildren().add(r);
                rmiStatut.setText("✔ RMI opérationnel");
                rmiStatut.setTextFill(Color.web(GREEN));
            });
        }).start();
    });

    btnStats.setOnAction(e -> {
        if (rmiClient == null || !rmiClient.isConnecte()) {
            alerte("RMI", "RMI non connecté."); return;
        }
        new Thread(() -> {
            Map<String, Object> stats =
                rmiClient.getStatistiques();
            Platform.runLater(() -> {
                resultBox.getChildren().clear();
                resultBox.getChildren().add(resultTitle);
                stats.forEach((k, v) -> {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(4, 0, 4, 0));
                    Label key = new Label(k + " :");
                    key.setFont(Font.font("Segoe UI", 12));
                    key.setTextFill(Color.web(TEXT_SUB));
                    key.setMinWidth(200);
                    Label val = new Label(
                        v instanceof Double
                        ? String.format("%,.2f", (Double) v)
                        : String.valueOf(v));
                    val.setFont(Font.font(
                        "Segoe UI", FontWeight.BOLD, 12));
                    val.setTextFill(Color.web(ACCENT));
                    row.getChildren().addAll(key, val);
                    resultBox.getChildren().add(row);
                });
            });
        }).start();
    });

    btnTop5.setOnAction(e -> {
        if (rmiClient == null || !rmiClient.isConnecte()) {
            alerte("RMI", "RMI non connecté."); return;
        }
        new Thread(() -> {
            List<main.model.Produit> top5 =
                rmiClient.getTop5PlusCher();
            Platform.runLater(() -> {
                resultBox.getChildren().clear();
                resultBox.getChildren().add(resultTitle);
                top5.forEach(p -> {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 0, 6, 0));
                    Label nom = new Label(p.getNom());
                    nom.setFont(Font.font("Segoe UI", 12));
                    nom.setTextFill(Color.web(TEXT));
                    HBox.setHgrow(nom, Priority.ALWAYS);
                    Label cat = new Label(p.getCategorie());
                    cat.setFont(Font.font("Segoe UI", 10));
                    cat.setTextFill(Color.web(TEXT_SUB));
                    Label prix = new Label(
                        String.format("%,.2f DA", p.getPrix()));
                    prix.setFont(Font.font(
                        "Segoe UI", FontWeight.BOLD, 13));
                    prix.setTextFill(Color.web(BLUE));
                    row.getChildren().addAll(nom, cat, prix);
                    resultBox.getChildren().add(row);
                });
            });
        }).start();
    });

    btnAlert.setOnAction(e -> {
        if (rmiClient == null || !rmiClient.isConnecte()) {
            alerte("RMI", "RMI non connecté."); return;
        }
        new Thread(() -> {
            List<main.model.Produit> alertes =
                rmiClient.getAlertesCritiques();
            Platform.runLater(() -> {
                resultBox.getChildren().clear();
                resultBox.getChildren().add(resultTitle);
                if (alertes.isEmpty()) {
                    Label ok = new Label(
                        "✔  Aucune alerte critique");
                    ok.setFont(Font.font("Segoe UI", 12));
                    ok.setTextFill(Color.web(GREEN));
                    resultBox.getChildren().add(ok);
                    return;
                }
                alertes.forEach(p -> {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 10, 6, 10));
                    String color =
                        p.estExpire()      ? RED    :
                        p.expireBientot()  ? ORANGE :
                        p.stockCritique()  ? YELLOW : BLUE;
                    row.setStyle(
                        "-fx-background-color:" + color + "15;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + color + "44;" +
                        "-fx-border-radius:8;" +
                        "-fx-border-width:1;"
                    );
                    Label nom = new Label(p.getNom());
                    nom.setFont(Font.font("Segoe UI", 12));
                    nom.setTextFill(Color.web(TEXT));
                    HBox.setHgrow(nom, Priority.ALWAYS);
                    Label qte = new Label(
                        "Qté: " + p.getQuantite());
                    qte.setFont(Font.font(
                        "Segoe UI", FontWeight.BOLD, 11));
                    qte.setTextFill(Color.web(color));
                    row.getChildren().addAll(nom, qte);
                    resultBox.getChildren().add(row);
                });
            });
        }).start();
    });

    ScrollPane sp = new ScrollPane(resultBox);
    sp.setFitToWidth(true);
    sp.setStyle(
        "-fx-background-color:transparent;" +
        "-fx-border-color:transparent;");
    VBox.setVgrow(sp, Priority.ALWAYS);

    content.getChildren().addAll(
        titre, sous, rmiStatut, btnRow, sp);
    VBox.setVgrow(content, Priority.ALWAYS);
    tab.setContent(content);
    return tab;
}
private String demanderIPManuellement() {
    TextInputDialog dialog = new TextInputDialog("192.168.1.");
    dialog.setTitle("PharmFlow — Serveur non trouvé");
    dialog.setHeaderText(
        "⚠  Aucun serveur détecté automatiquement");
    dialog.setContentText(
        "Entrez l'adresse IP du serveur manuellement :");

    dialog.getDialogPane().setStyle(
        "-fx-background-color:" + BG_CARD + ";");

    // Ajouter info
    Label info = new Label(
        "💡 Pour trouver l'IP du serveur :\n" +
        "   Ouvrez un terminal sur le PC serveur\n" +
        "   et tapez : ipconfig (Windows)\n" +
        "   ou : ip addr (Linux)");
    info.setFont(Font.font("Segoe UI", 11));
    info.setTextFill(Color.web(TEXT_SUB));
    info.setPadding(new Insets(10, 0, 0, 0));

    dialog.getDialogPane().setExpandableContent(info);
    dialog.getDialogPane().setExpanded(true);

    return dialog.showAndWait().orElse("localhost").trim();
}
}