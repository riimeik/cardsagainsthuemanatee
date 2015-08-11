public class ServerPlayer {
    private ServerClient serverClient;
    private GameData gameData;

    private int playerId;
    private String playerName;

    ServerPlayer() {
        gameData = new GameData();
    }

    public GameData getGameData() {
        return gameData;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public ServerClient getServerClient() {
        return serverClient;
    }

    public void setServerClient(ServerClient serverClient) {
        this.serverClient = serverClient;
    }

    public void sendMessage(String message) {
        serverClient.sendMessage(message);
    }
}
