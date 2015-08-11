import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GUI {
    EventHandler bigButtonPressed;
    EventHandler bigButtonReleased;

    private Stage primaryStage;
    private Scene primaryScene;
    private StackPane sceneRoot;
    private Pane currentFrame;
    private Pane nextFrame;
    private Pane startButton;
    private Pane answerButton;

    private int roundCounter = 0;
    private boolean isGameDealer = false;
    private int playerNodesAreaRadius = 120;

    private Text bestAnswerText;
    private List<GUIPlayer> playersList;
    private ListView<String> answersList;

    public GUI(Stage primaryStage) {
        resetGame();

        bigButtonPressed = event -> {
            Pane topLayer = (Pane) ((Pane) event.getSource()).lookup("#topLayer");
            topLayer.setTranslateY(topLayer.getTranslateY() + 1);
        };

        bigButtonReleased = event -> {
            Pane topLayer = (Pane) ((Pane) event.getSource()).lookup("#topLayer");
            topLayer.setTranslateY(topLayer.getTranslateY() - 1);
        };

        this.primaryStage = primaryStage;

        sceneRoot = loadFXML("MainScene.fxml");
        primaryScene = new Scene(sceneRoot);
        currentFrame = startScreenFrame();
        sceneRoot.getChildren().addAll(currentFrame);

        primaryStage.setScene(primaryScene);
        primaryStage.setTitle("Cards against hue-manatee");
        primaryStage.show();

        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());
    }

    public void resetGame() {
        roundCounter = 0;
        isGameDealer = false;
        playersList = new ArrayList<>();
    }

    private <T> T loadFXML(String fileName) {
        T node = null;

        try {
            node = FXMLLoader.load(CAH.getResource("gui/" + fileName));
        } catch (Exception e) {
            CAH.logError(e, true, "Graafika laadimine ebaõnnestus");
        }

        return node;
    }

    private Transition flipFrame(Pane newFrame) {
        nextFrame = newFrame;

        newFrame.setTranslateX(primaryScene.getWidth());
        newFrame.prefWidthProperty().bind(primaryScene.widthProperty());
        newFrame.prefHeightProperty().bind(primaryScene.heightProperty());

        sceneRoot.getChildren().addAll(newFrame);

        TranslateTransition flipIn = new TranslateTransition(Duration.millis(300), newFrame);
        TranslateTransition flipOut = new TranslateTransition(Duration.millis(300), currentFrame);
        flipIn.setByX(-1 * primaryScene.getWidth());
        flipOut.setByX(-1 * primaryScene.getWidth());

        ParallelTransition flipFrames = new ParallelTransition(flipIn, flipOut);
        flipFrames.setOnFinished(event -> flipFramesFinished());
        flipFrames.play();

        return flipFrames;
    }

    private void flipFramesFinished() {
        sceneRoot.getChildren().remove(0);
        currentFrame = nextFrame;
        nextFrame = null;
    }

    private URL getResource(String path) throws URISyntaxException, MalformedURLException {
        return getClass().getResource(path).toURI().toURL();
    }

    private Pane startScreenFrame() {
        VBox wrapperPane = loadFXML("StartScreenFrame.fxml");

        ImageView logoImage = null;
        ImageView titleImage = null;

        try {
            logoImage = new ImageView(CAH.getResource("gui/manatee.png").toString());
            titleImage = new ImageView(CAH.getResource("gui/header.png").toString());
        } catch (Exception e) {
            CAH.logError(e, true, "Graafika laadimine ebaõnnestus");
        }

        ((VBox) wrapperPane.lookup("#imageBox")).getChildren().addAll(logoImage, titleImage);

        Pane playerNameInput = bigTextField("Sisesta enda nimi");
        TextField playerNameField = (TextField) playerNameInput.lookup("#textField");
        Pane addressInput = bigTextField("Sisesta ühenduse aadress");
        TextField addressField = (TextField) addressInput.lookup("#textField");

        Pane newGameButton = bigButton("Uus mäng", "#bebebe");
        Pane joinGameButton = bigButton("Ühine mänguga", "#bebebe");

        StackPane inputFieldBox = (StackPane) wrapperPane.lookup("#inputFieldBox");
        inputFieldBox.getChildren().addAll(playerNameInput);

        ((VBox) wrapperPane.lookup("#actionArea")).getChildren().addAll(newGameButton, joinGameButton);

        newGameButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!playerNameField.getText().equals("")) {
                isGameDealer = true;
                CAH.setPlayerName(playerNameField.getText());

                flipFrame(gamePlayFrame()).setOnFinished(event1 -> {
                    flipFramesFinished();
                    setPrimaryText("Serverit käivitatakse..");
                    CAH.parseCommand("new");
                });
            } else {
                errorBigTextField(playerNameInput);
            }
        });

        joinGameButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!playerNameField.getText().equals("")) {
                if (!inputFieldBox.getChildren().contains(addressInput)) {
                    addressInput.setTranslateX(primaryScene.getWidth());
                    inputFieldBox.getChildren().addAll(addressInput);

                    TranslateTransition flipOut = new TranslateTransition(Duration.millis(150), playerNameInput);
                    TranslateTransition flipIn = new TranslateTransition(Duration.millis(150), addressInput);
                    flipIn.setByX(-1 * primaryScene.getWidth());
                    flipOut.setByX(-1 * primaryScene.getWidth());

                    ParallelTransition flipFrames = new ParallelTransition(flipIn, flipOut);
                    flipFrames.play();
                } else {
                    if (!addressField.getText().equals("")) {
                        CAH.setPlayerName(playerNameField.getText());

                        flipFrame(gamePlayFrame()).setOnFinished(event1 -> {
                            flipFramesFinished();

                            currentFrame.getChildren().remove(currentFrame.lookup("#serverAddressField"));
                            setPrimaryText("Serveriga ühendutakse..");
                            CAH.parseCommand("join " + addressField.getText());
                        });
                    } else {
                        errorBigTextField(addressInput);
                    }
                }
            } else {
                errorBigTextField(playerNameInput);
            }
        });

        return wrapperPane;
    }

    public void setPrimaryText(String message) {
        setGameText(message, currentFrame.lookup("#primaryText"));
    }

    public void setSecondaryText(String message) {
        setGameText(message, currentFrame.lookup("#secondaryText"));
    }

    public void setServerAddress(String address) {
        setPrimaryText("Serveriga saab ühenduda aadressil");
        setGameText(address, currentFrame.lookup("#serverAddressText"));
    }

    private void setGameText(String message, Node targetText) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), targetText);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            if (targetText instanceof Text) {
                ((Text) targetText).setText(message);
            } else {
                ((TextField) targetText).setText(message);
            }
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), targetText);
        fadeIn.setToValue(1);

        SequentialTransition fadeFlip = new SequentialTransition(fadeOut, fadeIn);
        fadeFlip.play();
    }

    public void updateQuestion() {
        if (roundCounter++ > 0) {
            setSecondaryText(((Text) currentFrame.lookup("#primaryText")).getText());
        }

        setPrimaryText(CAH.getPlayer().getGameData().getCurrentQuestion());
        switchPrimaryTextLabel(false);
    }

    public void bestAnswer(int playerId, String answer) {
        GUIPlayer bestPlayer = findPlayerById(playerId);
        bestPlayer.addWin();

        String fullText = CAH.replaceAnswer(CAH.getPlayer().getGameData().getCurrentQuestion(), answer);

        setPrimaryText(fullText + " (" + bestPlayer.getPlayerName() + ")");
        switchPrimaryTextLabel(true, "Parim vastus: ");
    }

    public void switchPrimaryTextLabel(boolean show, String labelText) {
        if (show) {
            bestAnswerText.setText(labelText);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bestAnswerText);
            fadeIn.setToValue(1);

            new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            fadeIn.play();
                        }
                    }, 300);
        } else {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), bestAnswerText);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(event -> bestAnswerText.setText(""));

            fadeOut.play();
        }
    }

    public void switchPrimaryTextLabel(boolean show) {
        switchPrimaryTextLabel(show, "");
    }

    public void updateAnswers(List<String> answerTexts) {
        ObservableList<String> answers = FXCollections.observableArrayList(answerTexts);

        answersList.setItems(answers);
        answersList.getSelectionModel().select(0);
        answersList.getFocusModel().focus(0);
        answersList.scrollTo(0);

        if (answerTexts.size() > 0) {
            activateBigButton(answerButton);
        } else {
            disableBigButton(answerButton);
        }
    }

    public void disableAnswerButton() {
        disableBigButton(answerButton);
    }

    public void updateAnswers() {
        updateAnswers(CAH.getPlayer().getGameData().getAllAnswers());
    }

    public void updateAskerStatus() {
        Text askerStatusText = (Text) currentFrame.lookup("#askerStatusText");

        if (CAH.getPlayer().getGameData().isAsker()) {
            if (answersList.getItems().size() == 0) {
                askerStatusText.setText("Teised mängijad on oma valiku teinud!\nVali välja parim vastus");
            } else {
                askerStatusText.setText("Sina seekord ei vasta\nOota, kuni teised mängijad on oma valiku teinud");
                updateAnswers(new ArrayList<>());
            }
        } else {
            askerStatusText.setText("Vali vastus, mis sinu arvates sobiks lünka kõige paremini");
            updateAnswers();
        }
    }

    public void setWinners(int points, List<Integer> winners) {
        List<String> winnersNames = new ArrayList<>();

        for (int plaeyrId : winners) {
            winnersNames.add(findPlayerById(plaeyrId).getPlayerName());
        }

        setPrimaryText(String.join(", ", winnersNames));
        switchPrimaryTextLabel(true, "Võitja" + (winners.size() == 1 ? "" : "d") + " (" + points + "p): ");
    }

    public void newPlayer(int playerId, String playerName) {
        GUIPlayer newPlayer = new GUIPlayer(
                playerId,
                playerName,
                loadFXML("PlayerNode.fxml"),
                loadFXML("PopupTooltip.fxml")
        );

        double[] newCoordinates;

        for (int i = 0; i < playersList.size(); i++) {
            Pane playerNode = playersList.get(i).getPlayerNode();

            newCoordinates = rotatePoint(
                    0.0,
                    playerNodesAreaRadius,
                    (double) (playersList.size() - i) * (2.0 * Math.PI / (double) (playersList.size() + 1))
            );


            Path rotationPath = new Path();
            MoveTo sourceLocation = new MoveTo();
            sourceLocation.setX(playerNode.getTranslateX());
            sourceLocation.setY(playerNode.getTranslateY());
            ArcTo moveAlong = new ArcTo();
            moveAlong.setX(newCoordinates[0]);
            moveAlong.setY(newCoordinates[1]);
            moveAlong.setRadiusX(playerNodesAreaRadius);
            moveAlong.setRadiusY(playerNodesAreaRadius);

            rotationPath.getElements().addAll(sourceLocation, moveAlong);

            PathTransition rotatePoint = new PathTransition(Duration.millis(300), rotationPath);
            rotatePoint.setNode(playerNode);
            rotatePoint.play();
        }

        newPlayer.getPlayerNode().setTranslateX(0);
        newPlayer.getPlayerNode().setTranslateY(playerNodesAreaRadius);

        playersList.add(newPlayer);
        ((Group) currentFrame.lookup("#playerNodesArea")).getChildren().addAll(newPlayer.getPlayerNode());

        if (CAH.isDealer() && CAH.getDealer().gameCanBeStarted()) {
            activateBigButton(startButton);
        }
    }

    public GUIPlayer findPlayerById(int playerId) {
        for (GUIPlayer gp : playersList) {
            if (gp.getPlayerId() == playerId) {
                return gp;
            }
        }

        return null;
    }

    public void removePlayer(int playerId) {
        GUIPlayer gp = findPlayerById(playerId);
        gp.removePlayer();
    }

    public void hasAnswered(int playerId) {
        findPlayerById(playerId).setHasAnswered();
    }

    public void setAsker(int playerId) {
        double rotateBy = 0;

        for (GUIPlayer gp : playersList) {
            if (gp.getPlayerId() == playerId) {
                rotateBy = (2.0 * Math.PI - rotationAngle(
                        gp.getPlayerNode().getTranslateX(),
                        gp.getPlayerNode().getTranslateY()
                )) % (2.0 * Math.PI);

                gp.setAskerStatus(true);
            } else {
                gp.setAskerStatus(false);
            }
        }

        // Ei ole vaja pöörata
        if (rotateBy < Math.pow(10, -5)) {
            return;
        }

        ParallelTransition rotateAllPoints = new ParallelTransition();
        double[] newCoordinates;

        for (GUIPlayer gp : playersList) {
            Pane playerNode = gp.getPlayerNode();
            newCoordinates = rotatePoint(
                    playerNode.getTranslateX(),
                    playerNode.getTranslateY(),
                    rotateBy
            );

            Path rotationPath = new Path();
            MoveTo sourceLocation = new MoveTo();
            sourceLocation.setX(playerNode.getTranslateX());
            sourceLocation.setY(playerNode.getTranslateY());
            ArcTo moveAlong = new ArcTo();
            moveAlong.setX(newCoordinates[0]);
            moveAlong.setY(newCoordinates[1]);
            moveAlong.setRadiusX(playerNodesAreaRadius);
            moveAlong.setRadiusY(playerNodesAreaRadius);

            rotationPath.getElements().addAll(sourceLocation, moveAlong);

            PathTransition rotatePoint = new PathTransition(Duration.millis(300), rotationPath);
            rotatePoint.setNode(playerNode);

            rotateAllPoints.getChildren().addAll(rotatePoint);
        }

        rotateAllPoints.play();
    }

    private Pane gamePlayFrame() {
        BorderPane wrapperPane = loadFXML("GamePlayFrame.fxml");

        answersList = (ListView) wrapperPane.lookup("#answersList");

        bestAnswerText = (Text) wrapperPane.lookup("#bestAnswerText");
        switchPrimaryTextLabel(false);

        Pane leaveButton = bigButton((isGameDealer ? "Lõpeta mäng" : "Lahku mängust"), "#d44343");
        Pane backButton = bigButton("Tagasi", "#d44343");
        startButton = bigButton("Alusta mängu", "#3dd32e");
        disableBigButton(startButton);

        HBox decisionButtonsBox = (HBox) wrapperPane.lookup("#decisionButtonsBox");

        if (isGameDealer) {
            decisionButtonsBox.getChildren().addAll(startButton);
        }

        decisionButtonsBox.getChildren().addAll(leaveButton);

        answerButton = bigButton("Saada vastus", "#bebebe");
        disableBigButton(answerButton);

        ((VBox) wrapperPane.lookup("#answerButtonBox")).getChildren().addAll(answerButton);

        startButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (startButton.getOpacity() < 1) {
                return;
            }

            CAH.parseCommand("start");

            FadeTransition buttonFadeOut = new FadeTransition(Duration.millis(100), startButton);
            buttonFadeOut.setToValue(0);
            buttonFadeOut.setOnFinished((event1) -> {
                decisionButtonsBox.getChildren().remove(startButton);
            });

            TextField serverAddressText = (TextField) currentFrame.lookup("#serverAddressText");

            FadeTransition addressFadeOut = new FadeTransition(Duration.millis(100), serverAddressText);
            addressFadeOut.setToValue(0);
            addressFadeOut.setOnFinished((event1) -> {
                decisionButtonsBox.getChildren().remove(serverAddressText);
            });

            buttonFadeOut.play();
            addressFadeOut.play();
        });

        leaveButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            CAH.parseCommand("end");

            if (!isGameDealer) {
                flipFrame(startScreenFrame()).setOnFinished(event1 -> {
                    flipFramesFinished();

                    CAH.resetGame();
                    resetGame();
                });
            } else {
                FadeTransition buttonFadeOut = new FadeTransition(Duration.millis(100), leaveButton);
                buttonFadeOut.setToValue(0);
                buttonFadeOut.setOnFinished((event1) -> {
                    decisionButtonsBox.getChildren().remove(leaveButton);
                });

                FadeTransition backButtonFadeIn = new FadeTransition(Duration.millis(100), backButton);
                backButtonFadeIn.setFromValue(0);
                backButtonFadeIn.setToValue(1);

                decisionButtonsBox.getChildren().add(0, backButton);

                buttonFadeOut.play();
                backButtonFadeIn.play();
            }
        });

        backButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            flipFrame(startScreenFrame()).setOnFinished(event1 -> {
                flipFramesFinished();

                CAH.resetGame();
                resetGame();
            });
        });

        answerButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (answerButton.getOpacity() < 1) {
                return;
            }

            CAH.getPlayer().chooseAnswer(answersList.getSelectionModel().getSelectedIndex());
        });

        return wrapperPane;
    }

    private Pane bigButton(String labelText, String backgroundColor) {
        Pane wrapperPane = loadFXML("BigButton.fxml");

        ((Text) wrapperPane.lookup("#buttonText")).setText(labelText);
        ((Rectangle) wrapperPane.lookup("#topRectangle")).setFill(Paint.valueOf(backgroundColor));
        ((Rectangle) wrapperPane.lookup("#bottomRectangle")).setFill(Paint.valueOf(backgroundColor));

        activateBigButton(wrapperPane);

        return wrapperPane;
    }

    private void activateBigButton(Pane bigButton) {
        bigButton.setOpacity(1);
        bigButton.addEventHandler(MouseEvent.MOUSE_PRESSED, bigButtonPressed);
        bigButton.addEventHandler(MouseEvent.MOUSE_RELEASED, bigButtonReleased);
    }

    private void disableBigButton(Pane bigButton) {
        bigButton.setOpacity(0.5);
        bigButton.removeEventHandler(MouseEvent.MOUSE_PRESSED, bigButtonPressed);
        bigButton.removeEventHandler(MouseEvent.MOUSE_RELEASED, bigButtonReleased);
    }

    private Pane bigTextField(String promptText) {
        StackPane wrapperPane = loadFXML("BigTextField.fxml");

        ((TextField) wrapperPane.lookup("#textField")).setPromptText(promptText);

        return wrapperPane;
    }

    private void errorBigTextField(Pane bigTextField) {
        Rectangle bottomRectangle = (Rectangle) bigTextField.lookup("#bottomRectangle");

        Paint prevColor = bottomRectangle.getStroke();
        Paint newColor = Color.valueOf("#e02323");
        double prevWidth = bottomRectangle.getStrokeWidth();
        double newWidth = 2;

        if (prevColor.equals(newColor) && prevWidth == newWidth) {
            return;
        }

        bottomRectangle.setStroke(newColor);
        bottomRectangle.setStrokeWidth(newWidth);

        ((TextField) bigTextField.lookup("#textField")).textProperty().addListener((observable, oldValue, newValue) -> {
            bottomRectangle.setStroke(prevColor);
            bottomRectangle.setStrokeWidth(prevWidth);
        });
    }

    private ContextMenu popupTooltip(String message) {
        ContextMenu wrapperMenu = loadFXML("PopupTooltip.fxml");
        CustomMenuItem menuItem = (CustomMenuItem) wrapperMenu.getItems().get(0);

        ((Text) menuItem.getContent().lookup("#tooltipText")).setText(message);

        return wrapperMenu;
    }

    private double[] rotatePoint(double x, double y, double phi) {
        return new double[]{
                x * Math.cos(phi) - y * Math.sin(phi),
                x * Math.sin(phi) + y * Math.cos(phi)
        };
    }

    private double rotationAngle(double x, double y) {
        double[] coordinates;
        int discreteRotations = 0;

        for (int i = 0; i < playersList.size(); i++) {
            coordinates = rotatePoint(
                    0,
                    playerNodesAreaRadius,
                    (double) i * 2.0 * Math.PI / (double) playersList.size()
            );

            if (Math.abs(coordinates[0] - x) < Math.pow(10, -5) && Math.abs(coordinates[1] - y) < Math.pow(10, -5)) {
                discreteRotations = i;
            }
        }

        return (double) discreteRotations * 2.0 * Math.PI / (double) playersList.size();
    }
}