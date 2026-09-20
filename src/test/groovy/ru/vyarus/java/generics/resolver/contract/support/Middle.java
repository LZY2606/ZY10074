package ru.vyarus.java.generics.resolver.contract.support;

/**
 * Middle hierarchy level: forwards owner generic unchanged. Used to verify that the same class
 * resolved under different roots shares structure but not substituted values.
 *
 * @author Vyacheslav Rusakov
 */
public class Middle<T> extends Owner<T> {

    public T middleField;
}
