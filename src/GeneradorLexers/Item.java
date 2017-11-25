package GeneradorLexers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Item  implements Serializable {
    // Instance variables
    private String head, next;
    private ArrayList<String> body;
    private boolean end;
    private int dot;

    public Item(String head, String[] body, int dot) {
        this.head = head;
        this.dot = dot;
        this.body = new ArrayList<String>();
        this.body.addAll(Arrays.asList(body));
        end = dot > this.body.size() - 1;
        if (end){
            this.next = "";
        } else {
            this.next = this.body.get(dot);
        }

    }

    public Item(String head, ArrayList<String> body, int dot) {
        this.head = head;
        this.dot = dot;
        this.body = new ArrayList<String>();
        this.body.addAll(body);
        end = dot > this.body.size() - 1;
        if (end){
            this.next = "";
        } else {
            this.next = this.body.get(dot);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        if (dot != item.dot) return false;
        if (!head.equals(item.head)) return false;
        if (!next.equals(item.next)) return false;
        return body.equals(item.body);
    }


    @Override
    public int hashCode() {
        int result = head.hashCode();
        result = 31 * result + next.hashCode();
        result = 31 * result + body.hashCode();
        result = 31 * result + (end ? 1 : 0);
        result = 31 * result + dot;
        return result;
    }

    @Override
    public String toString() {
        String result = head + " -> ";
        for (int i = 0; i < body.size(); i++) {
            if (i == dot){
                result += ". ";
            }
            result += body.get(i) + " ";
        }

        if (end) result += ". ";

        return result;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public ArrayList<String> getBody() {
        return body;
    }

    public void setBody(ArrayList<String> body) {
        this.body = body;
    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public int getDot() {
        return dot;
    }

    public void setDot(int dot) {
        this.dot = dot;
    }
}