import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class GUIPlayer {
    private int playerId;
    private String playerName;
    private int wins = 0;
    private String nodeMessage = "";

    private Pane playerNode;
    private ContextMenu popupTooltip;

    public GUIPlayer(int playerId, String playerName, Pane playerNode, ContextMenu popupTooltip) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerNode = playerNode;
        this.popupTooltip = popupTooltip;

        getLetterText().setText(playerName.substring(0, 1).toUpperCase());

        playerNode.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            popupTooltip.show(getBottomCircle(), Side.RIGHT, 10, 0);
        });

        playerNode.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            popupTooltip.hide();
        });

        if (playerId == CAH.getPlayer().getPlayerId()) {
            getLetterText().setStyle("-fx-font-weight: bold");
        }

        setNodeTooltip();
    }

    public void addWin() {
        wins += 1;
        setNodeTooltip();
    }

    public void removePlayer() {
        setNodeMessage("Mängija lahkus");
        playerNode.setOpacity(0.3);
    }

    public void setAskerStatus(boolean askerStatus) {
        if (askerStatus) {
            if (playerId == CAH.getPlayer().getPlayerId()) {
                setNodeMessage("Sina oled küsija");
            } else {
                setNodeMessage("Tema on küsija");
            }

            getBottomCircle().setFill(Color.valueOf("#54a6eb"));
        } else {
            setNodeMessage("Pole veel vastanud");
            getBottomCircle().setFill(Color.valueOf("#f5f5f5"));
        }
    }

    public void setHasAnswered() {
        if (playerId == CAH.getPlayer().getPlayerId()) {
            setNodeMessage("Oled vastanud");
        } else {
            setNodeMessage("On vastanud");
        }

        getBottomCircle().setFill(Color.valueOf("#eb7b54"));
    }

    public void setNodeMessage(String nodeMessage) {
        this.nodeMessage = nodeMessage;
        setNodeTooltip();
    }

    private void setNodeTooltip() {
        String nodeString = "Nimi: " + playerName + "\nPunkte: " + wins;

        if (!nodeMessage.equals("")) {
            nodeString = nodeString + "\n" + nodeMessage;
        }

        getTooltipText().setText(nodeString);
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Pane getPlayerNode() {
        return playerNode;
    }

    public Circle getBottomCircle() {
        return (Circle) playerNode.lookup("#bottomCircle");
    }

    private Text getLetterText() {
        return (Text) playerNode.lookup("#letterText");
    }

    private Text getTooltipText() {
        return (Text) ((CustomMenuItem) popupTooltip.getItems().get(0)).getContent().lookup("#tooltipText");
    }
}
