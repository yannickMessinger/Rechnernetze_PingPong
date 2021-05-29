import java.util.Random;

public class Help {

    public static final double NETWORK_ERROR_PROBABILITY = 0.2;
    public static final int WINDOW_SIZE = 4;
    public static final int NUMBER_PACKAGES = 10;
    public static final int SOCKET_TIMEOUT = 1000;


    public static boolean isFakeNetworkError() {
        Random random = new Random();
        return random.nextDouble() < NETWORK_ERROR_PROBABILITY;
    }
}
