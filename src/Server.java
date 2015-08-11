import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Klass on eraldi lõimes, et me ühendusi vastu saaks võtta
public class Server implements Runnable {
    private ServerSocket serverSocket;
    private Dealer dealer;

    private boolean forceServerSocketClose = false;

    Server(Dealer dealer) {
        this.dealer = dealer;
    }

    public String getLocalSocketAddress() {
        return serverSocket.getLocalSocketAddress().toString();
    }

    public boolean isServerRunning() throws InterruptedException {
        return serverSocket != null && serverSocket.isBound();
    }

    public void closeServerSocket() {
        forceServerSocketClose = true;

        try {
            serverSocket.close();
        } catch (IOException e) {
            // ..
        }
    }

    public void run() {
        try {
            synchronized (this) {
                serverSocket = new ServerSocket(0); // Suvalise pordiga socket
                serverSocket.setSoTimeout(0);
                notifyAll();
            }

            while (true) {
                Socket socket = serverSocket.accept(); // Võtame ühenduse vastu..

                ServerPlayer serverPlayer = new ServerPlayer(); // Uus objekt, mis hoiab selle mängija kohta infot
                ServerClient serverClient = new ServerClient(socket, dealer, serverPlayer); // Uus lõim, mis sellelt ühenduselt sõmumeid vastu võtab
                serverPlayer.setServerClient(serverClient); // Viide serverClient-ile, et mängijale sõnumeid vastu saata
            }
        } catch (Exception e) {
            if (!forceServerSocketClose) {
                CAH.logError(e, false, "Serveri ülesseadmisel tekkis viga");
            }
        }
    }
}

