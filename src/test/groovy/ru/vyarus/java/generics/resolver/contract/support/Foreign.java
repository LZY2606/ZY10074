package ru.vyarus.java.generics.resolver.contract.support;

/**
 * Type outside of every contract hierarchy: used to verify wrong context errors.
 *
 * @author Vyacheslav Rusakov
 */
public class Foreign<T> {

    public T foreignField;

    public T foreignMethod() {
        return null;
    }
}
