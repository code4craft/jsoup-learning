package org.jsoup.parser;

/**
 * 词法分析状态机。
 * States and transition activations for the Tokeniser.
 */
enum MiniSoupTokeniserState implements ITokeniserState {
    /**
     * 什么层级都没有的状态
     * ⬇
     * <div>test</div>
     *      ⬇
     * <div>test</div>
     */
    Data {
        // in data state, gather characters until a character reference or tag is found
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '<':
                    t.advanceTransition(TagOpen);
                    break;
                case eof:
                    t.emit(new Token.EOF());
                    break;
                default:
                    String data = r.consumeToAny('&', '<', nullChar);
                    t.emit(data);
                    break;
            }
        }
    },
    /**
     * ⬇
     * <div>test</div>
     */
    TagOpen {
        // from < in data
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '/':
                    t.advanceTransition(EndTagOpen);
                    break;
                default:
                    if (r.matchesLetter()) {
                        t.createTagPending(true);
                        t.transition(TagName);
                    } else {
                        t.error(this);
                        t.emit('<'); // char that got us here
                        t.transition(Data);
                    }
                    break;
            }
        }
    },
    /**
     *           ⬇
     * <div>test</div>
     */
    EndTagOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.emit("</");
                t.transition(Data);
            } else if (r.matches('>')) {
                t.error(this);
                t.advanceTransition(Data);
            }
        }
    },
    /**
     *  ⬇
     * <div>test</div>
     */
    TagName {
        // from < or </ in data, will have start or end tag pending
        public void read(Tokeniser t, CharacterReader r) {
            // previous TagOpen state did NOT consume, will have a letter char in current
            String tagName = r.consumeToAny('\t', '\n', '\r', '\f', ' ', '/', '>', nullChar).toLowerCase();
            t.tagPending.appendTagName(tagName);

            switch (r.consume()) {
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
            }
        }
    };


    public abstract void read(Tokeniser t, CharacterReader r);

    private static final char nullChar = '\u0000';
    private static final char eof = CharacterReader.EOF;

}