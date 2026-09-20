package ru.vyarus.java.generics.resolver.contract.support;

/**
 * Root of the main contract hierarchy: fixes T as String and declares fields of all inner flavors.
 *
 * @author Vyacheslav Rusakov
 */
public class OwnerRoot extends Middle<String> {

    public Inner inner;
    public ShadowingInner<Long> shadowed;
    public StaticInner<Double> stat;
}
