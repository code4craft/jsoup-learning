package us.codecraft.learning.automata;

/**
 * @author code4crafter@gmail.com
 */
public class StateModelABStateMachine implements ABStateMachine {

    State state;

    StringBuilder accum;

    enum State {
        Init {
            @Override
            public void process(StateModelABStateMachine stateModelABStateMachine, StringReader reader) throws StringReader.EOFException {
                char ch = reader.read();
                if (ch == 'a') {
                    stateModelABStateMachine.state = AfterA;
                    stateModelABStateMachine.accum.append(ch);
                }
            }
        },
        Accept {
            @Override
            public void process(StateModelABStateMachine stateModelABStateMachine, StringReader reader) throws StringReader.EOFException {
                System.out.println("find " + stateModelABStateMachine.accum.toString());
                stateModelABStateMachine.accum = new StringBuilder();
                stateModelABStateMachine.state = Init;
                reader.unread();
            }
        },
        AfterA {
            @Override
            public void process(StateModelABStateMachine stateModelABStateMachine, StringReader reader) throws StringReader.EOFException {
                char ch = reader.read();
                if (ch == 'b') {
                    stateModelABStateMachine.accum.append(ch);
                    stateModelABStateMachine.state = AfterB;
                } else {
                    stateModelABStateMachine.state = Accept;
                }
            }
        },
        AfterB {
            @Override
            public void process(StateModelABStateMachine stateModelABStateMachine, StringReader reader) throws StringReader.EOFException {
                char ch = reader.read();
                if (ch == 'b') {
                    stateModelABStateMachine.accum.append(ch);
                    stateModelABStateMachine.state = AfterB;
                } else {
                    stateModelABStateMachine.state = Accept;
                }
            }
        };

        public void process(StateModelABStateMachine stateModelABStateMachine, StringReader reader) throws StringReader.EOFException {
        }
    }

    public void process(StringReader reader) throws StringReader.EOFException {
        state.process(this, reader);
    }

    public static void main(String[] args) {
        ABStateMachine abStateMachine = new StateModelABStateMachine();
        String text = "abbbababbbaa";
        StringReader reader = new StringReader(text);
        try {
            while (true) {
                abStateMachine.process(reader);
            }
        } catch (StringReader.EOFException e) {
        }
    }
}
