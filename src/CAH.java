import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CAH extends Application {
    public static final int minPlayerCount = 3;

    private static Dealer dealer;
    private static Player player;
    private static String playerName;
    private static GUI window;

    public static void main(String[] args) {
        dealer = null;
        player = null;
        playerName = null;

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        resetGame();
        window = new GUI(primaryStage);
    }

    public static void resetGame() {
        if (player != null) {
            player.resetGame();
        }

        dealer = null;
        player = null;
        playerName = null;
    }

    public static boolean parseCommand(String line) {
        if(line.equals("end")) {
            if (dealer != null) {
                dealer.endGame();
            }

            if (player != null) {
                player.endGame();
            }
        } else if(line.equals("new")) {
            // Uus mäng - praegune kasutaja on nii diiler kui mängija
            if (player == null && dealer == null) {
                dealer = new Dealer();
            }
        } else if (line.equals("start")) {
            // Alustame mängu
            if (dealer != null && dealer.gameCanBeStarted()) {
                dealer.startGame();
            }
        } else if (line.startsWith("join ")) {
            // Ühineme mänguga (aadress peab olema kujul host:port)
            if (player == null) {
                try {
                    String[] addressParts = line.split(" ")[1].split(":");
                    Integer.parseInt(addressParts[1]);
                    player = new Player(addressParts[0], Integer.parseInt(addressParts[1]), playerName);
                } catch (Exception e) {
                    logError(e, false, "Serveriga ühendumisel tekkis viga");
                }
            }
        } else if (player != null) {
            // Mängimisega seotud käsklused
            if (line.startsWith("answer ") && !player.getGameData().isAsker()) {
                player.chooseAnswer(Integer.parseInt(line.split(" ")[1]));
            } else if (line.equals("show question")) {
                //player.printQuestion();
            } else if (line.equals("show answers")) {
                //player.printAnswers();
            } else if (line.startsWith("best answer ") && player.getGameData().isAsker()) {
                player.chooseAnswer(Integer.parseInt(line.split(" ")[2]));
            } else if (line.equals("points") || line.equals("wins")) {
                System.out.println(player.getGameData().getWins());
            }
        } else {
            logError(new Exception(""), false, "Tundmatu käsklus: " + line);
        }

        return true;
    }

    public static void setPlayerName(String playerName) {
        CAH.playerName = playerName.replace("//,", "/");
    }

    public static Player getPlayer() {
        return player;
    }

    public static Dealer getDealer() {
        return dealer;
    }

    public static GUI getWindow() {
        return window;
    }

    public static boolean isDealer() {
        return dealer != null;
    }

    public static List<String> getQuestionCards() {
        // Küsimuste kaartide listi lisamine
        List<String> questions = new ArrayList<>();

        try (Scanner reader = new Scanner(CAH.class.getResourceAsStream("questions.txt"), "UTF-8")) {
            String line;

            while (reader.hasNext()) {
                line = reader.nextLine();
                questions.add(line);
            }
        }

        return questions;
    }

    public static List<String> getAnswerCards() {
        // Vastuste kaartide listi lisamine
        List<String> answers = new ArrayList<>();

        try (Scanner reader = new Scanner(CAH.class.getResourceAsStream("answers.txt"), "UTF-8")) {
            String line;

            while (reader.hasNext()) {
                line = reader.nextLine();
                answers.add(line);
            }
        }

        return answers;
    }

    public static String replaceAnswer(String question, String answer) {
        return question.replace("___", "[" + answer + "]");
    }

    public static void logError(Exception e, boolean isSevere, String message) {
        System.out.println("Viga: " + message);
        e.printStackTrace();

        if(isSevere) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Viga: " + message);
            alert.showAndWait().ifPresent(consumer -> System.exit(1));
        } else if(window != null) {
            window.setPrimaryText(message);
        }
    }

    public static URL getResource(String file) throws URISyntaxException, MalformedURLException {
        return Thread.currentThread().getContextClassLoader().getResource(file);
    }
}
