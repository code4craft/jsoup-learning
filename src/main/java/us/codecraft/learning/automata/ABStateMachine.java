package us.codecraft.learning.automata;

/**
 * @author code4crafter@gmail.com
 */
public interface ABStateMachine {

    void process(StringReader reader) throws StringReader.EOFException;
}
