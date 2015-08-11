import javafx.application.Platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Player {
    private Client client;
    private Thread clientThread;
    private GameData gameData;

    private int playerId;

    private boolean canSendAnswer = false;
    private boolean canPickBest = false;

    Player(String host, int port, String playerName) {
        client = new Client(host, port, this);
        clientThread = new Thread(client);
        clientThread.start();

        gameData = new GameData();

        new Thread(() -> {
            try {
                synchronized (client) {
                    while (!client.isSocketBound()) {
                        client.wait();
                    }

                    sendMessage("name://" + playerName);
                }
            } catch (InterruptedException e) {
                CAH.logError(e, false, "Serveriga ühendumisel tekkis viga");
            }
        }).start();
    }

    public void resetGame() {
        client.closeSocket();
        clientThread.interrupt();
    }

    public Client getClient() {
        return client;
    }

    public GameData getGameData() {
        return gameData;
    }

    public void sendMessage(String message) {
        client.sendMessage(message);
    }

    public void parseMessage(String message) throws IOException {
        System.out.println(message);

        String[] msg = message.split("://");

        String cmd = msg[0];
        String[] param = msg.length > 1 ? msg[1].split("//") : new String[]{};

        if (cmd.equals("playerid")) {
            playerId = Integer.parseInt(param[0]);

            if (!CAH.isDealer()) {
                Platform.runLater(() -> CAH.getWindow().setPrimaryText("Serveriga ühendumine õnnestus"));
            }
        } else if (cmd.equals("print")) {
            Platform.runLater(() -> CAH.getWindow().setSecondaryText((param[0])));
        } else if (cmd.equals("newanswer")) {
            for (int i = 0; i < param.length; i++) {
                gameData.addAnswer(param[i]);
            }

            Platform.runLater(() -> CAH.getWindow().updateAnswers());
        } else if (cmd.equals("newquestion")) {
            gameData.setCurrentQuestion(param[0]);
            Platform.runLater(() -> CAH.getWindow().updateQuestion());
        } else if (cmd.equals("asker")) {
            int askerPlayerId = Integer.parseInt(param[0]);

            if (askerPlayerId == playerId) {
                gameData.setAskerStatus(true);
            } else {
                gameData.setAskerStatus(false);
                canSendAnswer = true;
            }

            Platform.runLater(() -> {
                CAH.getWindow().updateAskerStatus();
                CAH.getWindow().setAsker(askerPlayerId);
            });
        } else if (cmd.equals("allanswers")) {
            canPickBest = true;
            List<String> answersList = new ArrayList<>();

            for (int i = 0; i < param.length; i++) {
                answersList.add(param[i]);
            }

            Platform.runLater(() -> {
                CAH.getWindow().updateAskerStatus();
                CAH.getWindow().updateAnswers(answersList);
            });
        } else if (cmd.equals("gameend")) {
            Platform.runLater(() -> CAH.getWindow().setSecondaryText("Mäng on lõppenud"));
            endGame();
        } else if (cmd.equals("newplayer")) {
            Platform.runLater(() -> {
                CAH.getWindow().newPlayer(Integer.parseInt(param[0]), param[1]);
            });
        } else if (cmd.equals("removeplayer")) {
            Platform.runLater(() -> {
                CAH.getWindow().removePlayer(Integer.parseInt(param[0]));
            });
        } else if (cmd.equals("hasanswered")) {
            Platform.runLater(() -> {
                CAH.getWindow().hasAnswered(Integer.parseInt(param[0]));
            });
        } else if (cmd.equals("bestanswer")) {
            Platform.runLater(() -> {
                CAH.getWindow().bestAnswer(Integer.parseInt(param[0]), param[1]);
            });
        } else if (cmd.equals("winners")) {
            int points = Integer.parseInt(param[0]);
            List<Integer> winnerIds = new ArrayList<>();

            for (int i = 1; i < param.length; i++) {
                winnerIds.add(Integer.parseInt(param[i]));
            }

            Platform.runLater(() -> {
                CAH.getWindow().setWinners(points, winnerIds);
            });
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public void endGame() {
        client.closeSocket();
        Platform.runLater(() -> CAH.getWindow().setSecondaryText("Mäng on lõppenud"));
    }

    public void chooseAnswer(int id) {
        if (gameData.isAsker() && canPickBest) {
            sendMessage("pickbestanswer://" + id);
        } else if (canSendAnswer) {
            gameData.setCurrentAnswer(id);
            gameData.removeCurrentAnswer();
            sendMessage("answer://" + id);
        }

        Platform.runLater(() -> CAH.getWindow().disableAnswerButton());

        canSendAnswer = false;
        canPickBest = false;
    }
}
