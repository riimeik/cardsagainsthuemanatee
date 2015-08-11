import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Dealer {
    private Server server;
    private Thread serverThread;
    private Thread newRoundThread;

    private List<String> questionCards, answerCards;
    private List<ServerPlayer> playersList;
    private int lastPlayerId = 0;
    private List<Integer> askersQueue; // Mis järjekorras küsijad valitakse
    private int askersQueueIndex; // Preagune küsija

    private ArrayList<Integer> hasAnswered; // Kõik mängijad, kes on preagusele küsimusele vastanud
    private boolean gameStarted = false;
    private ServerPlayer lastPickedBestAnswer;

    private int optimalPlayerCount; // Et kõik küsimused saaks ära küsitud
    private int maxPlayerCount; // Et vähemalt sama palju küsimusi saaks ära küsida kui on mängijate arv + 1
    private int minPlayerCount;
    private int roundCounter = 0;

    private Random randInt;

    Dealer() {
        server = new Server(this);
        serverThread = new Thread(server);
        serverThread.start();

        playersList = new ArrayList<>();
        askersQueue = new ArrayList<>();
        hasAnswered = new ArrayList<>();

        questionCards = CAH.getQuestionCards();
        answerCards = CAH.getAnswerCards();

        // VastusteKaardid >= 10 * mängijateArv + mängijateArv * (küsimusteKaardid - 1)
        optimalPlayerCount = answerCards.size() / (10 + questionCards.size() - 1);
        maxPlayerCount = optimalPlayerCount + 5;
        minPlayerCount = CAH.minPlayerCount;

        randInt = new Random();

        new Thread(() -> {
            try {
                synchronized (server) {
                    while (!server.isServerRunning()) {
                        server.wait();
                    }

                    CAH.parseCommand("join " + getHostAddress());
                    CAH.getWindow().setServerAddress(getHostAddress());
                }
            } catch (Exception e) {
                CAH.logError(e, false, "Viga iseenda mängu lisamisel");
            }
        }).start();
    }

    // Tagastab aadressi, mille kaudu saavad teised mängijad ühenduda
    public String getHostAddress() {
        String hostName = "";
        String portNum = "";

        try {
            hostName = InetAddress.getLocalHost().toString().split("/")[0];
            portNum = server.getLocalSocketAddress().split("/")[1].split(":")[1];
        } catch (UnknownHostException e) {
            CAH.logError(e, false, "Ühenduse aadress ei ole kättesaadav");
        }

        return hostName + ":" + portNum;
    }

    public boolean gameCanBeStarted() {
        return (!gameStarted && playersList.size() >= minPlayerCount);
    }

    public void addPlayer(ServerPlayer serverPlayer) {
        playersList.add(serverPlayer);

        for (ServerPlayer sp : playersList) {
            sp.sendMessage(
                    "newplayer://" + serverPlayer.getPlayerId() + "//" + serverPlayer.getPlayerName()
            );

            if (sp != serverPlayer) {
                serverPlayer.sendMessage(
                        "newplayer://" + sp.getPlayerId() + "//" + sp.getPlayerName()
                );
            }
        }

        if (playersList.size() >= maxPlayerCount) {
            for (ServerPlayer sp : playersList) {
                sp.sendMessage("print://Mängijate arv on täis, mäng algab automaatselt!");
            }

            startGame();
        }
    }

    public void removePlayer(int playerId) {
        boolean isAsker = false;
        ServerPlayer player = findPlayerById(playerId);

        if (player == getAsker()) {
            isAsker = true;
        }

        playersList.remove(player);
        removeFromAskerQueue(player.getPlayerId());

        for (ServerPlayer sp : playersList) {
            sp.sendMessage("removeplayer://" + player.getPlayerId());

            if (playersList.size() < minPlayerCount) {
                sp.sendMessage("print://Mängijaid ei ole enam piisavalt, mäng lõppeb automaatselt");
            }
        }

        if (playersList.size() < minPlayerCount) {
            endGame();
        }

        if (isAsker && lastPickedBestAnswer != player) {
            startNewRound();
        }
    }

    private void removeFromAskerQueue(int playerId) {
        for (int i = 0; i < askersQueue.size(); i++) {
            if (askersQueue.get(i) == playerId) {
                askersQueue.remove(i);
                break;
            }
        }

        if (askersQueueIndex == askersQueue.size()) {
            askersQueueIndex -= 1;
        }
    }

    public void startGame() {
        gameStarted = true;
        serverThread.interrupt(); // Enam ühendusi vastu ei võta

        for(ServerPlayer player : playersList) {
            askersQueue.add(player.getPlayerId());

            String[] newAnswerCards = new String[10];

            for(int j = 0; j < 10; j++) {
                String answer = getNewAnswerCard();
                newAnswerCards[j] = answer;
                player.getGameData().addAnswer(answer);
            }

            player.sendMessage("newanswer://" + String.join("//", newAnswerCards));
        }

        Collections.shuffle(askersQueue); // Siin on mängijate järjestus - segame ära

        startNewRound();
    }

    public void endGame() {
        try {
            newRoundThread.interrupt();
        } catch (Exception e) {
            // ..
        }

        ArrayList<String> winnerIds = new ArrayList<>();

        int maxPoints = 0;

        for (ServerPlayer sp : playersList) {
            if (sp.getGameData().getWins() > maxPoints) {
                maxPoints = sp.getGameData().getWins();
                winnerIds.clear();
                winnerIds.add(Integer.toString(sp.getPlayerId()));
            } else if (sp.getGameData().getWins() == maxPoints) {
                winnerIds.add(Integer.toString(sp.getPlayerId()));
            }
        }

        for (ServerPlayer sp : playersList) {
            sp.sendMessage("winners://" + Integer.toString(maxPoints) + "//" + String.join("//", winnerIds));
            sp.sendMessage("gameend");

            if (sp.getPlayerId() == CAH.getPlayer().getPlayerId()) {
                CAH.getPlayer().getClient().setForceSocketClose(true);
            }

            sp.getServerClient().closeSocket(); // Ei taha enam sõnumeid
        }

        server.closeServerSocket();
    }

    private void setNewAsker() {
        if (roundCounter > 0) {
            ServerPlayer asker = getAsker();

            if (asker != null) {
                getAsker().getGameData().setAskerStatus(false);
            }
        }

        askersQueueIndex = (askersQueueIndex + 1) % askersQueue.size();
        getAsker().getGameData().setAskerStatus(true);
    }

    private ServerPlayer getAsker() {
        return findPlayerById(askersQueue.get(askersQueueIndex));
    }

    private void startNewRound() {
        if (Thread.interrupted()) {
            return;
        }

        if (questionCards.size() == 0 || answerCards.size() < playersList.size()) {
            endGame();
        }

        // Salvestame eelmise küsija - talle uut vastusekaarti ei anna
        ServerPlayer prevAsker = roundCounter > 0 ? getAsker() : null;
        setNewAsker();
        hasAnswered.clear();

        String question = getNewQuestionCard();

        for (ServerPlayer sp : playersList) {
            if (roundCounter > 0 && prevAsker != sp) {
                String answer = getNewAnswerCard();
                sp.sendMessage("newanswer://" + answer);
                sp.getGameData().addAnswer(answer);
            }

            sp.sendMessage("newquestion://" + question);
            sp.getGameData().setCurrentQuestion(question);

            sp.sendMessage("asker://" + getAsker().getPlayerId());
        }

        roundCounter += 1;
    }

    private String getNewQuestionCard() {
        int cardId = randInt.nextInt(questionCards.size() - 1);
        String question = questionCards.get(cardId);
        questionCards.remove(cardId);

        return question;
    }

    public String getNewAnswerCard() {
        int cardId = randInt.nextInt(answerCards.size() - 1);
        String answer = answerCards.get(cardId);
        answerCards.remove(cardId);

        return answer;
    }

    private void sendToAll(String message) {
        for (ServerPlayer sp : playersList) {
            sp.sendMessage(message);
        }
    }

    private ServerPlayer findPlayerById(int playerId) {
        for (ServerPlayer sp : playersList) {
            if (sp.getPlayerId() == playerId) {
                return sp;
            }
        }

        return null;
    }

    public void parseMessage(String message, ServerPlayer serverPlayer) {
        System.out.println(message);

        String[] msg = message.split("://");

        String cmd = msg[0];
        String[] param = msg.length > 1 ? msg[1].split("//") : new String[]{};

        if (cmd.equals("name")) {
            boolean breakOut;

            while (true) {
                breakOut = true;

                for (ServerPlayer sp : playersList) {
                    if (sp.getPlayerName().equals(param[0])) {
                        breakOut = false;
                        param[0] = param[0] + randInt.nextInt(10);
                    }
                }

                if (breakOut) {
                    break;
                }
            }

            serverPlayer.setPlayerName(param[0]);
            serverPlayer.setPlayerId(++lastPlayerId);
            addPlayer(serverPlayer);

            serverPlayer.sendMessage("playerid://" + serverPlayer.getPlayerId());
        } else if (cmd.equals("answer")) {
            int cardId = Integer.parseInt(param[0]);

            if (cardId < 0 || cardId >= 10) {
                return;
            }

            serverPlayer.getGameData().setCurrentAnswer(cardId);
            hasAnswered.add(serverPlayer.getPlayerId());

            sendToAll("hasanswered://" + serverPlayer.getPlayerId());

            // Piisavalt inimesi on vastanud
            if (hasAnswered.size() >= playersList.size() - 1) {
                Collections.shuffle(hasAnswered); // Et küsija aru mängijat ja vastust kokku ei viiks

                String[] answerTexts = new String[hasAnswered.size()];

                for (int i = 0; i < hasAnswered.size(); i++) {
                    int id = hasAnswered.get(i);
                    answerTexts[i] = findPlayerById(id).getGameData().getCurrentAnswer();
                }

                getAsker().sendMessage("allanswers://" + String.join("//", answerTexts));
            }
        } else if (cmd.equals("pickbestanswer")) {
            lastPickedBestAnswer = getAsker();
            int answerId = Integer.parseInt(param[0]);

            // Saadetud vastuse id ei sobi
            if (answerId < 0 || answerId >= hasAnswered.size()) {
                return;
            }

            ServerPlayer bestAnswerPlayer = findPlayerById(hasAnswered.get(answerId));
            String bestAnswer = bestAnswerPlayer.getGameData().getCurrentAnswer();

            // Anname kõigile teada, mis parim vastus oli
            for (ServerPlayer sp : playersList) {
                sp.sendMessage(
                        "bestanswer://" + Integer.toString(bestAnswerPlayer.getPlayerId()) + "//" +
                                bestAnswer
                );

                if (sp == bestAnswerPlayer) {
                    sp.getGameData().addWin();
                } else if (sp != getAsker()) {
                    sp.getGameData().removeCurrentAnswer();
                }
            }

            newRoundThread = new Thread(() -> {
                try {
                    Thread.sleep(6000);
                    startNewRound();
                } catch (InterruptedException e) {
                    // Ei huvita
                }
            });

            newRoundThread.start();
        }
    }
}
