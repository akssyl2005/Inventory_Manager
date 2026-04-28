package main.server;

import java.io.*;
import java.net.*;

import main.model.ProduitStore;

public class Server {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        ProduitStore store = new ProduitStore();
        System.out.println("Serveur démarré sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connecté : " + socket.getInetAddress());
                new Thread(new ClientHandler(socket, store)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }
}