package fr.onishintan.ui.lan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import javafx.application.Platform;

/**
 * The link between two games on the same network: lines of text both ways. A background thread reads,
 * and every line reaches the JavaFX thread in order. Lines that arrive while nobody listens wait in a queue.
 */
public final class LanLink {
    /** The port the host listens on. */
    public static final int PORT = 47321;
    /** How long joining may take before giving up. */
    private static final int CONNECT_MS = 5000;

    private final Socket socket;
    private final PrintWriter out;
    private final Queue<String> waiting = new ArrayDeque<>();
    private Consumer<String> onMessage;
    private Runnable onClosed = () -> {};
    private boolean closed;

    private LanLink(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        Thread reader = new Thread(this::read, "lan-reader");
        reader.setDaemon(true);   // never keeps the game open
        reader.start();
    }

    /**
     * Hosts a game: waits on a background thread for one player to join, then gives the link to onJoined.
     * Returns the listening socket, so waiting can be cancelled (close it); null if the port is taken.
     */
    public static ServerSocket host(Consumer<LanLink> onJoined, Consumer<String> onError) {
        ServerSocket server;
        try {
            server = new ServerSocket(PORT);
        } catch (IOException e) {
            onError.accept("Port " + PORT + " is already in use");
            return null;
        }
        Thread waiter = new Thread(() -> {
            try (server) {
                LanLink link = new LanLink(server.accept());   // one player only: the server closes after him
                Platform.runLater(() -> onJoined.accept(link));
            } catch (IOException e) {
                if (!server.isClosed()) {
                    Platform.runLater(() -> onError.accept("Hosting failed: " + e.getMessage()));
                }
            }
        }, "lan-host");
        waiter.setDaemon(true);
        waiter.start();
        return server;
    }

    /** Joins the game hosted at this address, on a background thread; the link goes to onJoined. */
    public static void join(String address, Consumer<LanLink> onJoined, Consumer<String> onError) {
        Thread joiner = new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(address, PORT), CONNECT_MS);
                LanLink link = new LanLink(socket);
                Platform.runLater(() -> onJoined.accept(link));
            } catch (IOException | IllegalArgumentException e) {
                Platform.runLater(() -> onError.accept("No game found at " + address));
            }
        }, "lan-join");
        joiner.setDaemon(true);
        joiner.start();
    }

    /** This computer's addresses on the local network (what the other player types to join). */
    public static List<String> localAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface net : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!net.isUp() || net.isLoopback() || net.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(net.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // no network: the list stays empty
        }
        return addresses;
    }

    /** Sends one line to the other game. */
    public void send(String line) {
        out.println(line);
    }

    /** Who hears the lines from now on; null keeps them waiting until someone listens again. */
    public void setOnMessage(Consumer<String> listener) {
        onMessage = listener;
        while (onMessage != null && !waiting.isEmpty()) {
            onMessage.accept(waiting.poll());
        }
    }

    /** What happens when the other game leaves or the network drops. */
    public void setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
    }

    /** Ends the link on purpose (the match is over, or the player went back). */
    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            // already closed
        }
    }

    /** Background thread: every line read goes to the JavaFX thread; the end of the stream means the other game left. */
    private void read() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line = in.readLine();
            while (line != null) {
                String message = line;
                Platform.runLater(() -> deliver(message));
                line = in.readLine();
            }
        } catch (IOException e) {
            // the socket closed: handled below
        }
        Platform.runLater(() -> {
            if (!closed) {
                onClosed.run();
            }
        });
    }

    /** One line on the JavaFX thread: to the listener, or into the queue while there is none. */
    private void deliver(String line) {
        if (onMessage == null) {
            waiting.add(line);
            return;
        }
        onMessage.accept(line);
    }
}
