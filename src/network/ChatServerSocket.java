package network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ChatServerSocket {
    private final int port;
    private final Consumer<String> onLog;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public ChatServerSocket(int port, Consumer<String> onLog) {
        this.port = port;
        this.onLog = onLog;
    }

    public void start() {
        if (running) return;
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Host Server đang lắng nghe tại cổng " + port + "...");
                while (running) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                if (running) log("Lỗi Server Socket: " + e.getMessage());
            } finally {
                running = false;
            }
        }, "Chat-Host-Thread").start();
    }

    public void stop() {
        running = false;
        for (ClientHandler ch : clients) {
            ch.closeQuietly();
        }
        clients.clear();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            log("Host Server đã dừng.");
        } catch (IOException e) {
            log("Lỗi khi dừng Server: " + e.getMessage());
        }
    }

    public void broadcast(String rawLine) {
        for (ClientHandler ch : clients) {
            ch.sendLine(rawLine);
        }
    }

    public int getConnectedCount() {
        return clients.size();
    }

    private void log(String msg) {
        if (onLog != null) onLog.accept(msg);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                log("Client kết nối: " + socket.getRemoteSocketAddress());
                String line;
                while (running && (line = in.readLine()) != null) {
                    log("Event: " + line);
                    broadcast(line);
                }
            } catch (IOException ignored) {
            } finally {
                clients.remove(this);
                closeQuietly();
                log("Client ngắt kết nối: " + socket.getRemoteSocketAddress());
            }
        }

        void sendLine(String line) {
            if (out != null) out.println(line);
        }

        void closeQuietly() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}