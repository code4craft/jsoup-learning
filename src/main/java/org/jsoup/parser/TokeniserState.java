package org.jsoup.parser;

/**
 * 词法分析状态机。
 * States and transition activations for the Tokeniser.
 */
enum TokeniserState implements ITokeniserState{
    /**
     * 初始状态,什么层级都没有的状态
     * ⬇
     *   <xxx></xxx>
     */
    Data {
        // in data state, gather characters until a character reference or tag is found
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '&':
                    t.advanceTransition(CharacterReferenceInData);
                    break;
                case '<':
                    t.advanceTransition(TagOpen);
                    break;
                case nullChar:
                    t.error(this); // NOT replacement character (oddly?)
                    t.emit(r.consume());
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
     * HTML实体转义的状态
     * ⬇
     * &amp;
     */
    CharacterReferenceInData {
        // from & in data
        public void read(Tokeniser t, CharacterReader r) {
            char[] c = t.consumeCharacterReference(null, false);
            if (c == null)
                t.emit('&');
            else
                t.emit(c);
            t.transition(Data);
        }
    },
    Rcdata {
        /// handles data in title, textarea etc
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '&':
                    t.advanceTransition(CharacterReferenceInRcdata);
                    break;
                case '<':
                    t.advanceTransition(RcdataLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.emit(replacementChar);
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
    CharacterReferenceInRcdata {
        public void read(Tokeniser t, CharacterReader r) {
            char[] c = t.consumeCharacterReference(null, false);
            if (c == null)
                t.emit('&');
            else
                t.emit(c);
            t.transition(Rcdata);
        }
    },
    Rawtext {
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '<':
                    t.advanceTransition(RawtextLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.emit(replacementChar);
                    break;
                case eof:
                    t.emit(new Token.EOF());
                    break;
                default:
                    String data = r.consumeToAny('<', nullChar);
                    t.emit(data);
                    break;
            }
        }
    },
    ScriptData {
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '<':
                    t.advanceTransition(ScriptDataLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.emit(replacementChar);
                    break;
                case eof:
                    t.emit(new Token.EOF());
                    break;
                default:
                    String data = r.consumeToAny('<', nullChar);
                    t.emit(data);
                    break;
            }
        }
    },
    PLAINTEXT {
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.emit(replacementChar);
                    break;
                case eof:
                    t.emit(new Token.EOF());
                    break;
                default:
                    String data = r.consumeTo(nullChar);
                    t.emit(data);
                    break;
            }
        }
    },
    /**
     * 打开的tag
     * ⬇
     * <div id="me"></div>
     */
    TagOpen {
        // from < in data
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.current()) {
                case '!':
                    t.advanceTransition(MarkupDeclarationOpen);
                    break;
                case '/':
                    t.advanceTransition(EndTagOpen);
                    break;
                case '?':
                    t.advanceTransition(BogusComment);
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
    EndTagOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.emit("</");
                t.transition(Data);
            } else if (r.matchesLetter()) {
                t.createTagPending(false);
                t.transition(TagName);
            } else if (r.matches('>')) {
                t.error(this);
                t.advanceTransition(Data);
            } else {
                t.error(this);
                t.advanceTransition(BogusComment);
            }
        }
    },
    TagName {
        // from < or </ in data, will have start or end tag pending
        public void read(Tokeniser t, CharacterReader r) {
            // previous TagOpen state did NOT consume, will have a letter char in current
            String tagName = r.consumeToAny('\t', '\n', '\r', '\f', ' ', '/', '>', nullChar).toLowerCase();
            t.tagPending.appendTagName(tagName);

            switch (r.consume()) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeAttributeName);
                    break;
                case '/':
                    t.transition(SelfClosingStartTag);
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case nullChar: // replacement
                    t.tagPending.appendTagName(replacementStr);
                    break;
                case eof: // should emit pending tag?
                    t.eofError(this);
                    t.transition(Data);
                // no default, as covered with above consumeToAny
            }
        }
    },
    RcdataLessthanSign {
        // from < in rcdata
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matches('/')) {
                t.createTempBuffer();
                t.advanceTransition(RCDATAEndTagOpen);
            } else if (r.matchesLetter() && !r.containsIgnoreCase("</" + t.appropriateEndTagName())) {
                // diverge from spec: got a start tag, but there's no appropriate end tag (</title>), so rather than
                // consuming to EOF; break out here
                t.tagPending = new Token.EndTag(t.appropriateEndTagName());
                t.emitTagPending();
                r.unconsume(); // undo "<"
                t.transition(Data);
            } else {
                t.emit("<");
                t.transition(Rcdata);
            }
        }
    },
    RCDATAEndTagOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.tagPending.appendTagName(Character.toLowerCase(r.current()));
                t.dataBuffer.append(Character.toLowerCase(r.current()));
                t.advanceTransition(RCDATAEndTagName);
            } else {
                t.emit("</");
                t.transition(Rcdata);
            }
        }
    },
    RCDATAEndTagName {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                String name = r.consumeLetterSequence();
                t.tagPending.appendTagName(name.toLowerCase());
                t.dataBuffer.append(name);
                return;
            }

            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    if (t.isAppropriateEndTagToken())
                        t.transition(BeforeAttributeName);
                    else
                        anythingElse(t, r);
                    break;
                case '/':
                    if (t.isAppropriateEndTagToken())
                        t.transition(SelfClosingStartTag);
                    else
                        anythingElse(t, r);
                    break;
                case '>':
                    if (t.isAppropriateEndTagToken()) {
                        t.emitTagPending();
                        t.transition(Data);
                    }
                    else
                        anythingElse(t, r);
                    break;
                default:
                    anythingElse(t, r);
            }
        }

        private void anythingElse(Tokeniser t, CharacterReader r) {
            t.emit("</" + t.dataBuffer.toString());
            r.unconsume();
            t.transition(Rcdata);
        }
    },
    RawtextLessthanSign {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matches('/')) {
                t.createTempBuffer();
                t.advanceTransition(RawtextEndTagOpen);
            } else {
                t.emit('<');
                t.transition(Rawtext);
            }
        }
    },
    RawtextEndTagOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.transition(RawtextEndTagName);
            } else {
                t.emit("</");
                t.transition(Rawtext);
            }
        }
    },
    RawtextEndTagName {
        public void read(Tokeniser t, CharacterReader r) {
            handleDataEndTag(t, r, Rawtext);
        }
    },
    ScriptDataLessthanSign {
        public void read(Tokeniser t, CharacterReader r) {
            switch (r.consume()) {
                case '/':
                    t.createTempBuffer();
                    t.transition(ScriptDataEndTagOpen);
                    break;
                case '!':
                    t.emit("<!");
                    t.transition(ScriptDataEscapeStart);
                    break;
                default:
                    t.emit("<");
                    r.unconsume();
                    t.transition(ScriptData);
            }
        }
    },
    ScriptDataEndTagOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.transition(ScriptDataEndTagName);
            } else {
                t.emit("</");
                t.transition(ScriptData);
            }

        }
    },
    ScriptDataEndTagName {
        public void read(Tokeniser t, CharacterReader r) {
            handleDataEndTag(t, r, ScriptData);
        }
    },
    ScriptDataEscapeStart {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matches('-')) {
                t.emit('-');
                t.advanceTransition(ScriptDataEscapeStartDash);
            } else {
                t.transition(ScriptData);
            }
        }
    },
    ScriptDataEscapeStartDash {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matches('-')) {
                t.emit('-');
                t.advanceTransition(ScriptDataEscapedDashDash);
            } else {
                t.transition(ScriptData);
            }
        }
    },
    ScriptDataEscaped {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.transition(Data);
                return;
            }

            switch (r.current()) {
                case '-':
                    t.emit('-');
                    t.advanceTransition(ScriptDataEscapedDash);
                    break;
                case '<':
                    t.advanceTransition(ScriptDataEscapedLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.emit(replacementChar);
                    break;
                default:
                    String data = r.consumeToAny('-', '<', nullChar);
                    t.emit(data);
            }
        }
    },
    ScriptDataEscapedDash {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.transition(Data);
                return;
            }

            char c = r.consume();
            switch (c) {
                case '-':
                    t.emit(c);
                    t.transition(ScriptDataEscapedDashDash);
                    break;
                case '<':
                    t.transition(ScriptDataEscapedLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    t.emit(replacementChar);
                    t.transition(ScriptDataEscaped);
                    break;
                default:
                    t.emit(c);
                    t.transition(ScriptDataEscaped);
            }
        }
    },
    ScriptDataEscapedDashDash {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.transition(Data);
                return;
            }

            char c = r.consume();
            switch (c) {
                case '-':
                    t.emit(c);
                    break;
                case '<':
                    t.transition(ScriptDataEscapedLessthanSign);
                    break;
                case '>':
                    t.emit(c);
                    t.transition(ScriptData);
                    break;
                case nullChar:
                    t.error(this);
                    t.emit(replacementChar);
                    t.transition(ScriptDataEscaped);
                    break;
                default:
                    t.emit(c);
                    t.transition(ScriptDataEscaped);
            }
        }
    },
    ScriptDataEscapedLessthanSign {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTempBuffer();
                t.dataBuffer.append(Character.toLowerCase(r.current()));
                t.emit("<" + r.current());
                t.advanceTransition(ScriptDataDoubleEscapeStart);
            } else if (r.matches('/')) {
                t.createTempBuffer();
                t.advanceTransition(ScriptDataEscapedEndTagOpen);
            } else {
                t.emit('<');
                t.transition(ScriptDataEscaped);
            }
        }
    },
    ScriptDataEscapedEndTagOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createTagPending(false);
                t.tagPending.appendTagName(Character.toLowerCase(r.current()));
                t.dataBuffer.append(r.current());
                t.advanceTransition(ScriptDataEscapedEndTagName);
            } else {
                t.emit("</");
                t.transition(ScriptDataEscaped);
            }
        }
    },
    ScriptDataEscapedEndTagName {
        public void read(Tokeniser t, CharacterReader r) {
            handleDataEndTag(t, r, ScriptDataEscaped);
        }
    },
    ScriptDataDoubleEscapeStart {
        public void read(Tokeniser t, CharacterReader r) {
            handleDataDoubleEscapeTag(t, r, ScriptDataDoubleEscaped, ScriptDataEscaped);
        }
    },
    ScriptDataDoubleEscaped {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.current();
            switch (c) {
                case '-':
                    t.emit(c);
                    t.advanceTransition(ScriptDataDoubleEscapedDash);
                    break;
                case '<':
                    t.emit(c);
                    t.advanceTransition(ScriptDataDoubleEscapedLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.emit(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                default:
                    String data = r.consumeToAny('-', '<', nullChar);
                    t.emit(data);
            }
        }
    },
    ScriptDataDoubleEscapedDash {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-':
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscapedDashDash);
                    break;
                case '<':
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscapedLessthanSign);
                    break;
                case nullChar:
                    t.error(this);
                    t.emit(replacementChar);
                    t.transition(ScriptDataDoubleEscaped);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                default:
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscaped);
            }
        }
    },
    ScriptDataDoubleEscapedDashDash {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-':
                    t.emit(c);
                    break;
                case '<':
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscapedLessthanSign);
                    break;
                case '>':
                    t.emit(c);
                    t.transition(ScriptData);
                    break;
                case nullChar:
                    t.error(this);
                    t.emit(replacementChar);
                    t.transition(ScriptDataDoubleEscaped);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                default:
                    t.emit(c);
                    t.transition(ScriptDataDoubleEscaped);
            }
        }
    },
    ScriptDataDoubleEscapedLessthanSign {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matches('/')) {
                t.emit('/');
                t.createTempBuffer();
                t.advanceTransition(ScriptDataDoubleEscapeEnd);
            } else {
                t.transition(ScriptDataDoubleEscaped);
            }
        }
    },
    ScriptDataDoubleEscapeEnd {
        public void read(Tokeniser t, CharacterReader r) {
            handleDataDoubleEscapeTag(t,r, ScriptDataEscaped, ScriptDataDoubleEscaped);
        }
    },
    BeforeAttributeName {
        // from tagname <xxx
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    break; // ignore whitespace
                case '/':
                    t.transition(SelfClosingStartTag);
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.newAttribute();
                    r.unconsume();
                    t.transition(AttributeName);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                case '"':
                case '\'':
                case '<':
                case '=':
                    t.error(this);
                    t.tagPending.newAttribute();
                    t.tagPending.appendAttributeName(c);
                    t.transition(AttributeName);
                    break;
                default: // A-Z, anything else
                    t.tagPending.newAttribute();
                    r.unconsume();
                    t.transition(AttributeName);
            }
        }
    },
    AttributeName {
        // from before attribute name
        public void read(Tokeniser t, CharacterReader r) {
            String name = r.consumeToAny('\t', '\n', '\r', '\f', ' ', '/', '=', '>', nullChar, '"', '\'', '<');
            t.tagPending.appendAttributeName(name.toLowerCase());

            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(AfterAttributeName);
                    break;
                case '/':
                    t.transition(SelfClosingStartTag);
                    break;
                case '=':
                    t.transition(BeforeAttributeValue);
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.appendAttributeName(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                case '"':
                case '\'':
                case '<':
                    t.error(this);
                    t.tagPending.appendAttributeName(c);
                // no default, as covered in consumeToAny
            }
        }
    },
    AfterAttributeName {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    // ignore
                    break;
                case '/':
                    t.transition(SelfClosingStartTag);
                    break;
                case '=':
                    t.transition(BeforeAttributeValue);
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.appendAttributeName(replacementChar);
                    t.transition(AttributeName);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                case '"':
                case '\'':
                case '<':
                    t.error(this);
                    t.tagPending.newAttribute();
                    t.tagPending.appendAttributeName(c);
                    t.transition(AttributeName);
                    break;
                default: // A-Z, anything else
                    t.tagPending.newAttribute();
                    r.unconsume();
                    t.transition(AttributeName);
            }
        }
    },
    BeforeAttributeValue {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    // ignore
                    break;
                case '"':
                    t.transition(AttributeValue_doubleQuoted);
                    break;
                case '&':
                    r.unconsume();
                    t.transition(AttributeValue_unquoted);
                    break;
                case '\'':
                    t.transition(AttributeValue_singleQuoted);
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.appendAttributeValue(replacementChar);
                    t.transition(AttributeValue_unquoted);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                case '>':
                    t.error(this);
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case '<':
                case '=':
                case '`':
                    t.error(this);
                    t.tagPending.appendAttributeValue(c);
                    t.transition(AttributeValue_unquoted);
                    break;
                default:
                    r.unconsume();
                    t.transition(AttributeValue_unquoted);
            }
        }
    },
    AttributeValue_doubleQuoted {
        public void read(Tokeniser t, CharacterReader r) {
            String value = r.consumeToAny('"', '&', nullChar);
            if (value.length() > 0)
                t.tagPending.appendAttributeValue(value);

            char c = r.consume();
            switch (c) {
                case '"':
                    t.transition(AfterAttributeValue_quoted);
                    break;
                case '&':
                    char[] ref = t.consumeCharacterReference('"', true);
                    if (ref != null)
                        t.tagPending.appendAttributeValue(ref);
                    else
                        t.tagPending.appendAttributeValue('&');
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.appendAttributeValue(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                // no default, handled in consume to any above
            }
        }
    },
    AttributeValue_singleQuoted {
        public void read(Tokeniser t, CharacterReader r) {
            String value = r.consumeToAny('\'', '&', nullChar);
            if (value.length() > 0)
                t.tagPending.appendAttributeValue(value);

            char c = r.consume();
            switch (c) {
                case '\'':
                    t.transition(AfterAttributeValue_quoted);
                    break;
                case '&':
                    char[] ref = t.consumeCharacterReference('\'', true);
                    if (ref != null)
                        t.tagPending.appendAttributeValue(ref);
                    else
                        t.tagPending.appendAttributeValue('&');
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.appendAttributeValue(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                // no default, handled in consume to any above
            }
        }
    },
    AttributeValue_unquoted {
        public void read(Tokeniser t, CharacterReader r) {
            String value = r.consumeToAny('\t', '\n', '\r', '\f', ' ', '&', '>', nullChar, '"', '\'', '<', '=', '`');
            if (value.length() > 0)
                t.tagPending.appendAttributeValue(value);

            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeAttributeName);
                    break;
                case '&':
                    char[] ref = t.consumeCharacterReference('>', true);
                    if (ref != null)
                        t.tagPending.appendAttributeValue(ref);
                    else
                        t.tagPending.appendAttributeValue('&');
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case nullChar:
                    t.error(this);
                    t.tagPending.appendAttributeValue(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                case '"':
                case '\'':
                case '<':
                case '=':
                case '`':
                    t.error(this);
                    t.tagPending.appendAttributeValue(c);
                    break;
                // no default, handled in consume to any above
            }

        }
    },
    // CharacterReferenceInAttributeValue state handled inline
    AfterAttributeValue_quoted {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeAttributeName);
                    break;
                case '/':
                    t.transition(SelfClosingStartTag);
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    r.unconsume();
                    t.transition(BeforeAttributeName);
            }

        }
    },
    SelfClosingStartTag {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '>':
                    t.tagPending.selfClosing = true;
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.transition(BeforeAttributeName);
            }
        }
    },
    BogusComment {
        public void read(Tokeniser t, CharacterReader r) {
            // todo: handle bogus comment starting from eof. when does that trigger?
            // rewind to capture character that lead us here
            r.unconsume();
            Token.Comment comment = new Token.Comment();
            comment.bogus = true;
            comment.data.append(r.consumeTo('>'));
            // todo: replace nullChar with replaceChar
            t.emit(comment);
            t.advanceTransition(Data);
        }
    },
    /**
     * HTML定义或者注释的开始
     *  ⬇
     * <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     *  ⬇
     * <!-- comments -->
     *
     */
    MarkupDeclarationOpen {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchConsume("--")) {
                t.createCommentPending();
                t.transition(CommentStart);
            } else if (r.matchConsumeIgnoreCase("DOCTYPE")) {
                t.transition(Doctype);
            } else if (r.matchConsume("[CDATA[")) {
                // todo: should actually check current namepspace, and only non-html allows cdata. until namespace
                // is implemented properly, keep handling as cdata
                //} else if (!t.currentNodeInHtmlNS() && r.matchConsume("[CDATA[")) {
                t.transition(CdataSection);
            } else {
                t.error(this);
                t.advanceTransition(BogusComment); // advance so this character gets in bogus comment data's rewind
            }
        }
    },
    /**
     * 注释开始
     *  ⬇
     * <!-- comments -->
     */
    CommentStart {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-':
                    t.transition(CommentStartDash);
                    break;
                case nullChar:
                    t.error(this);
                    t.commentPending.data.append(replacementChar);
                    t.transition(Comment);
                    break;
                case '>':
                    t.error(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                default:
                    t.commentPending.data.append(c);
                    t.transition(Comment);
            }
        }
    },
    /**
     * 注释开始的横线
     *   ⬇
     * <!-- comments -->
     */
    CommentStartDash {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-':
                    t.transition(CommentStartDash);
                    break;
                case nullChar:
                    t.error(this);
                    t.commentPending.data.append(replacementChar);
                    t.transition(Comment);
                    break;
                case '>':
                    t.error(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                default:
                    t.commentPending.data.append(c);
                    t.transition(Comment);
            }
        }
    },
    /**
     * 注释内容
     *      ⬇
     * <!-- comments -->
     */
    Comment {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.current();
            switch (c) {
                case '-':
                    t.advanceTransition(CommentEndDash);
                    break;
                case nullChar:
                    t.error(this);
                    r.advance();
                    t.commentPending.data.append(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                default:
                    t.commentPending.data.append(r.consumeToAny('-', nullChar));
            }
        }
    },
    /**
     * 注释结束的横线
     *               ⬇
     * <!-- comments -->
     */
    CommentEndDash {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-':
                    t.transition(CommentEnd);
                    break;
                case nullChar:
                    t.error(this);
                    t.commentPending.data.append('-').append(replacementChar);
                    t.transition(Comment);
                    break;
                case eof:
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                default:
                    t.commentPending.data.append('-').append(c);
                    t.transition(Comment);
            }
        }
    },
    /**
     * 注释结束
     *                 ⬇
     * <!-- comments -->
     */
    CommentEnd {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '>':
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                case nullChar:
                    t.error(this);
                    t.commentPending.data.append("--").append(replacementChar);
                    t.transition(Comment);
                    break;
                case '!':
                    t.error(this);
                    t.transition(CommentEndBang);
                    break;
                case '-':
                    t.error(this);
                    t.commentPending.data.append('-');
                    break;
                case eof:
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.commentPending.data.append("--").append(c);
                    t.transition(Comment);
            }
        }
    },
    /**
     * 非标准注释？
     *                 ⬇
     * <!-- comments --!>
     */
    CommentEndBang {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '-':
                    t.commentPending.data.append("--!");
                    t.transition(CommentEndDash);
                    break;
                case '>':
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                case nullChar:
                    t.error(this);
                    t.commentPending.data.append("--!").append(replacementChar);
                    t.transition(Comment);
                    break;
                case eof:
                    t.eofError(this);
                    t.emitCommentPending();
                    t.transition(Data);
                    break;
                default:
                    t.commentPending.data.append("--!").append(c);
                    t.transition(Comment);
            }
        }
    },
    /**
     *
     *           ⬇
     * <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    Doctype {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeDoctypeName);
                    break;
                case eof:
                    t.eofError(this);
                    t.createDoctypePending();
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.transition(BeforeDoctypeName);
            }
        }
    },
    /**
     *
     *           ⬇
     * <!DOCTYPE  html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    BeforeDoctypeName {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                t.createDoctypePending();
                t.transition(DoctypeName);
                return;
            }
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    break; // ignore whitespace
                case nullChar:
                    t.error(this);
                    t.doctypePending.name.append(replacementChar);
                    t.transition(DoctypeName);
                    break;
                case eof:
                    t.eofError(this);
                    t.createDoctypePending();
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.createDoctypePending();
                    t.doctypePending.name.append(c);
                    t.transition(DoctypeName);
            }
        }
    },
    /**
     *
     *            ⬇
     * <!DOCTYPE  html  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    DoctypeName {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.matchesLetter()) {
                String name = r.consumeLetterSequence();
                t.doctypePending.name.append(name.toLowerCase());
                return;
            }
            char c = r.consume();
            switch (c) {
                case '>':
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(AfterDoctypeName);
                    break;
                case nullChar:
                    t.error(this);
                    t.doctypePending.name.append(replacementChar);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.doctypePending.name.append(c);
            }
        }
    },
    /**
     *
     *                ⬇
     * <!DOCTYPE  html  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    AfterDoctypeName {
        public void read(Tokeniser t, CharacterReader r) {
            if (r.isEmpty()) {
                t.eofError(this);
                t.doctypePending.forceQuirks = true;
                t.emitDoctypePending();
                t.transition(Data);
                return;
            }
            if (r.matchesAny('\t', '\n', '\r', '\f', ' '))
                r.advance(); // ignore whitespace
            else if (r.matches('>')) {
                t.emitDoctypePending();
                t.advanceTransition(Data);
            } else if (r.matchConsumeIgnoreCase("PUBLIC")) {
                t.transition(AfterDoctypePublicKeyword);
            } else if (r.matchConsumeIgnoreCase("SYSTEM")) {
                t.transition(AfterDoctypeSystemKeyword);
            } else {
                t.error(this);
                t.doctypePending.forceQuirks = true;
                t.advanceTransition(BogusDoctype);
            }

        }
    },
    /**
     *
     *                        ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    AfterDoctypePublicKeyword {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeDoctypePublicIdentifier);
                    break;
                case '"':
                    t.error(this);
                    // set public id to empty string
                    t.transition(DoctypePublicIdentifier_doubleQuoted);
                    break;
                case '\'':
                    t.error(this);
                    // set public id to empty string
                    t.transition(DoctypePublicIdentifier_singleQuoted);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
            }
        }
    },
    /**
     *
     *                         ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    BeforeDoctypePublicIdentifier {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    break;
                case '"':
                    // set public id to empty string
                    t.transition(DoctypePublicIdentifier_doubleQuoted);
                    break;
                case '\'':
                    // set public id to empty string
                    t.transition(DoctypePublicIdentifier_singleQuoted);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
            }
        }
    },
    /**
     *
     *                         ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    DoctypePublicIdentifier_doubleQuoted {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '"':
                    t.transition(AfterDoctypePublicIdentifier);
                    break;
                case nullChar:
                    t.error(this);
                    t.doctypePending.publicIdentifier.append(replacementChar);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.doctypePending.publicIdentifier.append(c);
            }
        }
    },
    DoctypePublicIdentifier_singleQuoted {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\'':
                    t.transition(AfterDoctypePublicIdentifier);
                    break;
                case nullChar:
                    t.error(this);
                    t.doctypePending.publicIdentifier.append(replacementChar);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.doctypePending.publicIdentifier.append(c);
            }
        }
    },
    /**
     *
     *                                                                 ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    AfterDoctypePublicIdentifier {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BetweenDoctypePublicAndSystemIdentifiers);
                    break;
                case '>':
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case '"':
                    t.error(this);
                    // system id empty
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                case '\'':
                    t.error(this);
                    // system id empty
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
            }
        }
    },
    /**
     *
     *                                                                  ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    BetweenDoctypePublicAndSystemIdentifiers {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    break;
                case '>':
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case '"':
                    t.error(this);
                    // system id empty
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                case '\'':
                    t.error(this);
                    // system id empty
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
            }
        }
    },
    AfterDoctypeSystemKeyword {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeDoctypeSystemIdentifier);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case '"':
                    t.error(this);
                    // system id empty
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                case '\'':
                    t.error(this);
                    // system id empty
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
            }
        }
    },
    BeforeDoctypeSystemIdentifier {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    break;
                case '"':
                    // set system id to empty string
                    t.transition(DoctypeSystemIdentifier_doubleQuoted);
                    break;
                case '\'':
                    // set public id to empty string
                    t.transition(DoctypeSystemIdentifier_singleQuoted);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.transition(BogusDoctype);
            }
        }
    },
    /**
     *
     *                                                                   ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    DoctypeSystemIdentifier_doubleQuoted {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '"':
                    t.transition(AfterDoctypeSystemIdentifier);
                    break;
                case nullChar:
                    t.error(this);
                    t.doctypePending.systemIdentifier.append(replacementChar);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.doctypePending.systemIdentifier.append(c);
            }
        }
    },
    DoctypeSystemIdentifier_singleQuoted {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\'':
                    t.transition(AfterDoctypeSystemIdentifier);
                    break;
                case nullChar:
                    t.error(this);
                    t.doctypePending.systemIdentifier.append(replacementChar);
                    break;
                case '>':
                    t.error(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.doctypePending.systemIdentifier.append(c);
            }
        }
    },
    /**
     *
     *                                                                                                                          ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
     */
    AfterDoctypeSystemIdentifier {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    break;
                case '>':
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.eofError(this);
                    t.doctypePending.forceQuirks = true;
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    t.error(this);
                    t.transition(BogusDoctype);
                    // NOT force quirks
            }
        }
    },
    /**
     *
     *                                                                                                                             ⬇
     * <!DOCTYPE  html  PUBLIC  "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" blabla>
     */
    BogusDoctype {
        public void read(Tokeniser t, CharacterReader r) {
            char c = r.consume();
            switch (c) {
                case '>':
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                case eof:
                    t.emitDoctypePending();
                    t.transition(Data);
                    break;
                default:
                    // ignore char
                    break;
            }
        }
    },
    CdataSection {
        public void read(Tokeniser t, CharacterReader r) {
            String data = r.consumeTo("]]>");
            t.emit(data);
            r.matchConsume("]]>");
            t.transition(Data);
        }
    };


    public abstract void read(Tokeniser t, CharacterReader r);

    private static final char nullChar = '\u0000';
    private static final char replacementChar = Tokeniser.replacementChar;
    private static final String replacementStr = String.valueOf(Tokeniser.replacementChar);
    private static final char eof = CharacterReader.EOF;

    /**
     * Handles RawtextEndTagName, ScriptDataEndTagName, and ScriptDataEscapedEndTagName. Same body impl, just
     * different else exit transitions.
     */
    private static final void handleDataEndTag(Tokeniser t, CharacterReader r, TokeniserState elseTransition) {
        if (r.matchesLetter()) {
            String name = r.consumeLetterSequence();
            t.tagPending.appendTagName(name.toLowerCase());
            t.dataBuffer.append(name);
            return;
        }

        boolean needsExitTransition = false;
        if (t.isAppropriateEndTagToken() && !r.isEmpty()) {
            char c = r.consume();
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                    t.transition(BeforeAttributeName);
                    break;
                case '/':
                    t.transition(SelfClosingStartTag);
                    break;
                case '>':
                    t.emitTagPending();
                    t.transition(Data);
                    break;
                default:
                    t.dataBuffer.append(c);
                    needsExitTransition = true;
            }
        } else {
            needsExitTransition = true;
        }

        if (needsExitTransition) {
            t.emit("</" + t.dataBuffer.toString());
            t.transition(elseTransition);
        }
    }

    private static final void handleDataDoubleEscapeTag(Tokeniser t, CharacterReader r, TokeniserState primary, TokeniserState fallback) {
        if (r.matchesLetter()) {
            String name = r.consumeLetterSequence();
            t.dataBuffer.append(name.toLowerCase());
            t.emit(name);
            return;
        }

        char c = r.consume();
        switch (c) {
            case '\t':
            case '\n':
            case '\r':
            case '\f':
            case ' ':
            case '/':
            case '>':
                if (t.dataBuffer.toString().equals("script"))
                    t.transition(primary);
                else
                    t.transition(fallback);
                t.emit(c);
                break;
            default:
                r.unconsume();
                t.transition(fallback);
        }
    }
}