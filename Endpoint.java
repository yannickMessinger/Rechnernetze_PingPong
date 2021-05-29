import java.net.InetAddress;

public class Endpoint {

    private final InetAddress address;
    private final int port;
    private int curSeqnum;

    public Endpoint(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.curSeqnum = 0;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getCurSeqnum() {
        return curSeqnum;
    }

    public void nextSeqnum() {
        this.curSeqnum = this.curSeqnum + 1;
    }
}
