import java.util.ArrayList;
import java.util.List;

public class GameData {
    private List<String> answerList;
    private boolean askerStatus = false;
    private int currentAnswer;
    private String currentQuestion;
    private int wins = 0;

    GameData() {
        answerList = new ArrayList<>();
    }

    public List<String> getAllAnswers() {
        return answerList;
    }

    public void addAnswer(String answer) {
        answerList.add(answer);
    }

    public void removeCurrentAnswer() {
        answerList.remove(currentAnswer);
    }

    public String getAnswer(int id) {
        return answerList.get(id);
    }

    public String getCurrentAnswer() {
        return getAnswer(currentAnswer);
    }

    public void setCurrentAnswer(int id) {
        currentAnswer = id;
    }

    public int getCurrentAnswerId() {
        return currentAnswer;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public void addWin() {
        wins += 1;
    }

    public int getWins() {
        return wins;
    }

    public void setAskerStatus(boolean asksQuestion) {
        this.askerStatus = asksQuestion;
    }

    public boolean isAsker() {
        return askerStatus;
    }
}
