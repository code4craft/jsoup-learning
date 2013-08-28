package us.codecraft.learning.automata;

/**
 * @author code4crafter@gmail.com
 */
public class SwitchABStateMachine implements ABStateMachine {

    enum State {
        Init, Accept, AfterA, AfterB;
    }

    private StringBuilder accum = new StringBuilder();

    private State state = State.Init;

    public void process(StringReader reader) throws StringReader.EOFException {
        char ch;
        switch (state) {
            case Init:
                ch = reader.read();
                if (ch == 'a') {
                    state = State.AfterA;
                    accum.append(ch);
                }
                break;
            case AfterA:
                ch = reader.read();
                if (ch == 'b') {
                    accum.append(ch);
                    state = State.AfterB;
                } else {
                    state = State.Accept;
                }
                break;
            case AfterB:
                ch = reader.read();
                if (ch == 'b') {
                    accum.append(ch);
                    state = State.AfterB;
                } else {
                    state = State.Accept;
                }
                break;
            case Accept:
                System.out.println("find " + accum.toString());
                accum = new StringBuilder();
                state = State.Init;
                reader.unread();
                break;
        }
    }

    public static void main(String[] args) {
        ABStateMachine abStateMachine = new SwitchABStateMachine();
        String text = "abbbababbbaa";
        StringReader reader = new StringReader(text);
        try {
            while (true){
                abStateMachine.process(reader);
            }
        } catch (StringReader.EOFException e) {
        }
    }
}
