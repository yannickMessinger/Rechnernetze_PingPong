public class Packetdata {

    private final Message message;
    private final long timeStart;

    public Packetdata(Message message, long timeStart) {
        this.message = message;
        this.timeStart = timeStart;
    }

    public Message getMessage() {
        return message;
    }

    public long getTimeStart() {
        return timeStart;
    }

}
