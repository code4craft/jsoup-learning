package org.jsoup.parser;

/**
 * @author code4crafter@gmail.com
 */
interface ITokeniserState {

    abstract void read(Tokeniser t, CharacterReader r);
}
