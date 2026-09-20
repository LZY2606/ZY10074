package ru.vyarus.java.generics.resolver.contract.support;

import java.util.List;
import java.util.Map;

/**
 * Wildcard flavors: upper bound, lower bound, both in one type, wildcard inside generic array
 * and a raw type. Written in java to guarantee exact declared types.
 *
 * @author Vyacheslav Rusakov
 */
public class WildcardHost {

    public List<? extends Number> upper;
    public List<? super Integer> lower;
    public Map<? extends Number, ? super Integer> mixed;
    public List<? extends Number>[] upperArray;
    @SuppressWarnings("rawtypes")
    public List raw;
}
