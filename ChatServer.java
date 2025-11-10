import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Multithreaded chat server.
 * Accepts many clients and broadcasts messages to all connected clients.
 */
public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("Starting chat server on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Waiting for clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void broadcast(String message, ClientHandler from) {
        // Send to all clients except optionally the sender (we include sender here)
        System.out.println(message); // server console log
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcast("[Server] " + client.getUsername() + " left the chat.", client);
    }

    public void shutdown() {
        try {
            pool.shutdownNow();
            for (ClientHandler c : clients) {
                c.close();
            }
        } catch (Exception e) {
            // ignore
        }
        System.out.println("Server shut down.");
    }

    public static void main(String[] args) {
        int port = 12345;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        new ChatServer(port).start();
    }

    // ---------- ClientHandler inner class ----------
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private String username = "Anonymous";
        private BufferedReader in;
        private PrintWriter out;
        private volatile boolean running = true;

        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        public String getUsername() {
            return username;
        }

        public void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }

        public void close() {
            running = false;
            try { socket.close(); } catch (IOException ignored) {}
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First line from client should be username
                out.println("Welcome! Please enter your username:");
                String name = in.readLine();
                if (name != null && !name.trim().isEmpty()) {
                    username = name.trim();
                }
                server.broadcast("[Server] " + username + " joined the chat.", this);
                out.println("[Server] You are connected as: " + username);
                out.println("[Server] Type /quit to leave.");

                String line;
                while (running && (line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    // Broadcast message
                    String msg = username + ": " + line;
                    server.broadcast(msg, this);
                }
            } catch (IOException e) {
                // client disconnected unexpectedly
            } finally {
                try {
                    if (out != null) out.println("[Server] Goodbye!");
                } catch (Exception ignored) {}
                server.removeClient(this);
                close();
            }
        }
    }
}
