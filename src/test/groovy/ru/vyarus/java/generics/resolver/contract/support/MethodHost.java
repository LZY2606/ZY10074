package ru.vyarus.java.generics.resolver.contract.support;

/**
 * Method level variables: variable hiding class variable, recursive method bound
 * and method variable inside generic array. Written in java to allow method variable
 * hiding the class one.
 *
 * @author Vyacheslav Rusakov
 */
public class MethodHost<T> {

    public T classVar() {
        return null;
    }

    public <T> T hidden(T input) {
        return null;
    }

    public <M extends Comparable<M>> M max(M left, M right) {
        return null;
    }

    public <M> M[] arrayOf(M seed) {
        return null;
    }
}
