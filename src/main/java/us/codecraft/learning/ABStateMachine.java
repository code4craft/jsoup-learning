package us.codecraft.learning;

/**
 * @author code4crafter@gmail.com
 */
public interface ABStateMachine {

    void process(StringReader reader) throws StringReader.EOFException;
}
