package core.util;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Martin
 */
public class Progressable {
    
    private double start = 0.0;     // progress at which current subroutine started
    private double goal = 1.0;      // progress at which current subroutine is done
    private double progress = 0.0;  // total progress: between 0 and 1 
    private boolean stopped = false;
    private final ArrayList<Double> starts;
    private final ArrayList<Double> goals;
    public int messages_up_to_level = Integer.MAX_VALUE;
    HashSet<MessageListener> messageListeners = new HashSet<>();
    HashSet<ProgressListener> progressListeners = new HashSet<>();

    private final MessageListener MESSAGE_LISTENER_SYSTEM_OUT = (message) -> {
        System.out.println(formateMessage(message, this.getCurrentLevel(), this.getProgress()));
    };

    public Progressable() {
        starts = new ArrayList<>();
        goals = new ArrayList<>();
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
    }
    
    public double getProgress() {
        return progress;
    }

    public double getProgressOfCurrentSubroutine() {
        if (goal == start) return 0.0;
        return (progress - start) / (goal - start);
    }

    public void progressSubroutineBy(double additionalProgress) {
        progressSubroutineTo(getProgressOfCurrentSubroutine() + additionalProgress);
    }

    public void progressSubroutineTo(double newProgress) {
        if (newProgress > 1) newProgress = 1;
        double next_progress = start + (goal - start) * newProgress;
        if (next_progress > goal) next_progress = goal;
        if (progress < newProgress) {
            progress = next_progress;
            handleProgressUpdate();
        }
    }

    void handleProgressUpdate() {
        for (ProgressListener l : progressListeners) {
//            System.out.println(getCurrentLevel() + ": " + progress + " " + getProgressOfCurrentSubroutine());
            l.progressUpdated(getProgress());
        }
    }

    public int getCurrentLevel() {
        return starts.size();
    }

    public boolean needsMessage() {
        return starts.size() <= messages_up_to_level && !messageListeners.isEmpty();
    }

    public void updateMessage(String message) {
        if (needsMessage()) {
            handleMessageUpdate(message);
        }
    }

    void handleMessageUpdate(String message) {
        for (MessageListener l : messageListeners) {
            l.messageUpdated(message);
        }
    }

    public void enableSystemOutput() {
        addMessageListener(MESSAGE_LISTENER_SYSTEM_OUT);
    }

    public void disableSystemOutput() {
        messageListeners.remove(MESSAGE_LISTENER_SYSTEM_OUT);
    }

    public void addMessageListener(MessageListener l) {
        messageListeners.add(l);
    }

    public boolean removeMessageListener(MessageListener l) {
        return messageListeners.remove(l);
    }

    public void addProgressListener(ProgressListener l) {
        progressListeners.add(l);
    }

    public boolean removeProgressListener(ProgressListener l) {
        return progressListeners.remove(l);
    }

    public Progressable start_subroutine_with_unknown_percentage() {
        return start_subroutine(0.0);
    }

    public Progressable start_subroutine(double fraction_of_current_goal) {
//        System.out.println("befor start: level=" + getCurrentLevel() + " start=" + IO.significantFigures(start) + " end=" + IO.significantFigures(goal) + " progress=" + IO.significantFigures(progress));
        double next_goal = progress + (goal - start) * fraction_of_current_goal;
        if (next_goal > goal) next_goal = goal;
        starts.add(start);
        start = progress;
        goals.add(goal);
        goal = next_goal;
//        System.out.println("after start: level=" + getCurrentLevel() + " start=" + IO.significantFigures(start) + " end=" + IO.significantFigures(goal) + " progress=" + IO.significantFigures(progress));
        return this;
    }

    public void end_subroutine() {
//        System.out.println("befor end:   level=" + getCurrentLevel() + " start=" + IO.significantFigures(start) + " end=" + IO.significantFigures(goal) + " progress=" + IO.significantFigures(progress));
        progressSubroutineTo(1.0);
        start = starts.remove(starts.size() - 1);
        progress = goal;
        goal = goals.remove(goals.size() - 1);
//        System.out.println("after end:   level=" + getCurrentLevel() + " start=" + IO.significantFigures(start) + " end=" + IO.significantFigures(goal) + " progress=" + IO.significantFigures(progress));
    }

    public String formateMessage(String message, boolean withProgress) {
        return formateMessage(message, getCurrentLevel(), withProgress ? getProgress() : null);
    }
    
    public static String formateMessage(String message, int level, Double totolProgress) {
        return IO.getCurTimeString() 
                + (totolProgress == null ? "" : (" " + String.format("%6s", String.format("%.2f", Math.round(totolProgress * 10000) / 100.0)) + "%")) 
                + "  ".repeat(level + 1) 
                + message;
    }

    public static void main(String[] args) {
        Progressable progressable = new Progressable();
        progressable.enableSystemOutput();
        progressable.updateMessage("Starting Progress Level 1...");
        progressable.progressSubroutineBy(0.312);
        progressable.updateMessage("Did some Level 1 work...");
        progressable.updateMessage("Starting subroutine of 10%");
        progressable.start_subroutine(0.1);
        progressable.progressSubroutineBy(0.56);
        progressable.updateMessage("Did some Level 2 work...");
//        progressable.updateProgress(1);
//        progressable.updateMssage("Did rest of Level 2 work");
        progressable.end_subroutine();
        progressable.updateMessage("Ended subroutine of 10%");
        progressable.progressSubroutineBy(0.312);
        progressable.updateMessage("Did some Level 1 work...");
        progressable.updateMessage("Starting subroutine of unknown length");
        progressable.start_subroutine_with_unknown_percentage();
        progressable.progressSubroutineBy(0.5);
        progressable.updateMessage("Did some Level 2 work...");
        progressable.progressSubroutineTo(1);
        progressable.updateMessage("Did rest of Level 2 work");
        progressable.end_subroutine();
        progressable.progressSubroutineBy(0.1);
        progressable.updateMessage("Ended subroutine of unknown length");
    }

    public interface MessageListener {

        public void messageUpdated(String message);
    }

    public interface ProgressListener {

        public void progressUpdated(double progress);
    }
}
