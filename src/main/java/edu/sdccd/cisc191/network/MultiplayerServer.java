package edu.sdccd.cisc191.network;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

    /*
     * Features added:
     * Multiplayer server for handling multiple player connections
     * Creates a server that listens for connections
     * Accepts multiple players
     * Broadcasts messages to all connected client
     */

public class MultiplayerServer {
    /** Port for client/server communication. */
    private static final int PORT = 5000; // Port for communication
    /** Stores the output writers for all connected clients for broadcasting. */
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    /**
     * Starts the multiplayer server, accepting connections and launching handlers for each client.
     * @param args not used
     */
    public static void main(String[] args) {
        System.out.println("Multiplayer Server Started on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket).start();
                } catch (IOException e) {
                    System.err.println("Error accepting new connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    /**
     * Handles a single client connection, reading input and broadcasting to all clients.
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientInfo; // <--- Declare here

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort(); // Or do this in run()
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                System.out.println("New Player Connected: " + clientInfo);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from " + clientInfo + ": " + message);
                    broadcastMessage("[" + clientInfo + "]: " + message);
                }
            } catch (IOException e) {
                System.err.println("Connection Lost: " + e.getMessage());
            } finally {
                // resource cleanup
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                    }
                }
                try {
                    if (in != null) in.close();
                } catch (IOException e) {
                    System.err.println("Error closing input stream: " + e.getMessage());
                }
                try {
                    if (out != null) out.close();
                } catch (Exception e) {
                    System.err.println("Error closing output stream: " + e.getMessage());
                }
                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     * @param message the message to broadcast
     */
    private static void broadcastMessage(String message) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                try {
                    writer.println(message);
                } catch (Exception e) {
                    System.err.println("Failed to send message to a client: " + e.getMessage());
                }
            }
        }
    }
}
