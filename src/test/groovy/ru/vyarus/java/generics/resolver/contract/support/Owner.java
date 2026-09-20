package ru.vyarus.java.generics.resolver.contract.support;

import java.util.List;

/**
 * Owner class with three inner class flavors: non-static inner (sees owner generics),
 * inner shadowing owner generic and static inner (owner generics not visible).
 * Written in java to guarantee exact inner class semantics.
 *
 * @author Vyacheslav Rusakov
 */
public class Owner<T> {

    public T ownerField;

    public class Inner {
        public T innerField;
        public List<T> innerList;
    }

    public class ShadowingInner<T> {
        public T ownField;
    }

    public static class StaticInner<T> {
        public T staticField;
    }
}
