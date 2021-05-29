import java.util.Timer;
import java.util.TimerTask;

public class TimeManager {

    private boolean isActive;
    private Timer timer;
    private boolean isTimeout;

    public TimeManager() {
        this.isActive = false;
        this.isTimeout = false;
        this.timer = new Timer();
    }

    public boolean isTimeout() {
        return isTimeout;
    }

    public void resetTimeout() {
        isTimeout = false;
    }

    public void startTimer() {
        if (isActive) {
            return;
        }

        isActive = true;
        isTimeout = false;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isActive = false;
                isTimeout = true;
            }
        }, 1000);
    }

    public void interruptTimer() {
        timer.cancel();
        isActive = false;
    }
}
