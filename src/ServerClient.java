import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerClient extends Thread {
    private Socket socket;
    private BufferedReader istream;
    private PrintWriter ostream;

    private Dealer dealer;
    private ServerPlayer serverPlayer;

    private boolean forceSocketClose = false;

    ServerClient(Socket socket, Dealer dealer, ServerPlayer serverPlayer) throws IOException {
        this.socket = socket;
        this.dealer = dealer;
        this.serverPlayer = serverPlayer;

        istream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ostream = new PrintWriter(socket.getOutputStream(), true);

        start();
    }

    public void sendMessage(String message) {
        ostream.println(message);
    }

    public void closeSocket() {
        forceSocketClose = true;

        try {
            socket.close();
            istream.close();
            ostream.close();
        } catch (IOException e) {
            // .
        }
    }

    public void run() {
        try {
            String message;

            while (true) {
                if ((message = istream.readLine()) != null) {
                    dealer.parseMessage(message, serverPlayer);
                } else {
                    throw new Exception();
                }
            }
        } catch (Exception e) {
            if (!forceSocketClose) {
                dealer.removePlayer(serverPlayer.getPlayerId());
            }
        }
    }
}