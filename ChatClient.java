import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Console chat client. 
 * - Connects to server
 * - Sends username as first message
 * - Reads user input and sends to server
 * - Prints incoming messages on separate thread
 */
public class ChatClient {
    private final String serverHost;
    private final int serverPort;

    public ChatClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    public void start() {
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            // Reader thread: prints messages from server
            Thread readerThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    // server closed connection
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Server initially asks for username — read that prompt and then send username
            // But because server may print many lines, we let user see prompt and then type name.
            // We'll wait for the server's "Please enter your username:" then prompt locally.
            // Simpler: read a short sleep to allow welcome messages to arrive, then prompt.
            Thread.sleep(200); // small delay so server messages appear first (not required)

            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) username = "Anonymous";
            out.println(username);

            // Input loop - read user's messages and send to server
            while (true) {
                String input = scanner.nextLine();
                if (input == null) break;
                input = input.trim();
                if (input.equalsIgnoreCase("/quit")) {
                    out.println("/quit");
                    break;
                }
                if (!input.isEmpty()) {
                    out.println(input);
                }
            }

            System.out.println("Disconnected from chat.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        new ChatClient(host, port).start();
    }
}
