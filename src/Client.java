import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {
    private Socket socket;
    private String host;
    private int port;

    private Player player;
    private BufferedReader istream;
    private PrintWriter ostream;

    private boolean forceSocketClose = false;

    Client(String host, int port, Player player) {
        this.player = player;
        this.host = host;
        this.port = port;
    }

    public void sendMessage(String message) {
        ostream.println(message);
    }

    public boolean isSocketBound() throws InterruptedException {
        return socket != null && socket.isBound() && ostream != null && istream != null;
    }

    public void setForceSocketClose(boolean forceSocketClose) {
        this.forceSocketClose = forceSocketClose;
    }

    public void closeSocket() {
        forceSocketClose = true;

        try {
            socket.close();
            istream.close();
            ostream.close();
        } catch (IOException e) {
            // ..
        }
    }

    public void run() {
        try {
            synchronized (this) {
                socket = new Socket(host, port);
                istream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ostream = new PrintWriter(socket.getOutputStream(), true);
                notifyAll();
            }

            String message;

            while (true) {
                if ((message = istream.readLine()) != null) {
                    player.parseMessage(message);
                } else {
                    throw new Exception();
                }
            }
        } catch (Exception e) {
            if (!forceSocketClose) {
                CAH.logError(e, false, "Serveriga suhtlemisel tekkis viga");
            }
        }
    }
}