package ru.vyarus.java.generics.resolver.contract.support;

/**
 * Second root over the same {@link Middle} class with a different generic value.
 * Used for cache isolation and concurrency contracts.
 *
 * @author Vyacheslav Rusakov
 */
public class OtherOwnerRoot extends Middle<Integer> {

    public Inner inner;
}
