package network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClientSocket {
    private final String host;
    private final int port;
    private final Consumer<String> onMessageReceived;
    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;

    public ChatClientSocket(String host, int port, Consumer<String> onMessageReceived) {
        this.host = host;
        this.port = port;
        this.onMessageReceived = onMessageReceived;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            connected = true;
            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                    String line;
                    while (connected && (line = in.readLine()) != null) {
                        if (onMessageReceived != null) onMessageReceived.accept(line);
                    }
                } catch (IOException ignored) {
                } finally {
                    connected = false;
                }
            }, "Chat-Client-Listener").start();
            return true;
        } catch (IOException e) {
            connected = false;
            return false;
        }
    }

    public void sendEvent(String eventPayload) {
        if (connected && out != null) {
            out.println(eventPayload.replace("\n", "\\n").replace("\r", ""));
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}