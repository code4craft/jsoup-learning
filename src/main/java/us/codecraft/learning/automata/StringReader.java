package us.codecraft.learning.automata;

/**
 * @author code4crafter@gmail.com
 */
public class StringReader {

    class EOFException extends Exception {

    }

    private String string;

    private int index;

    public StringReader(String string) {
        this.string = string;
    }

    public char read() throws EOFException {
        if (index < string.length() - 1) {
            return string.charAt(index++);
        } else {
            throw new EOFException();
        }
    }

    public void unread() {
        index--;
        if (index < 0) {
            index = 0;
        }
    }
}
