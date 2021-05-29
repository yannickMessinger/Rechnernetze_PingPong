public class Message {
    private final String content;
    private final int seqnum;

    public Message(String content, int seqnum) {
        this.content = content;
        this.seqnum = seqnum;
    }

    public static Message parse(String raw) {
        String content = raw.replace("\n", "");
        String[] comps = content.split(";");
        if (comps.length > 1) {
            String newContent = comps[0];
            int newSeqnum = Integer.parseInt(comps[1]);
            return new Message(newContent, newSeqnum);
        } else {
            System.out.println("Fehler");
            return null;
        }
    }

    public String serialize() {
        return content + ";" + seqnum + "\n";
    }

    @Override
    public String toString() {
        return content + ";" + seqnum;
    }

    public String getContent() {
        return content;
    }

    public int getSeqnum() {
        return seqnum;
    }
}
