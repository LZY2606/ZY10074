package ru.vyarus.java.generics.resolver.contract.support;

import java.util.List;
import java.util.Map;

/**
 * Readable type family used by context api contract tests. Combines non-static and static inner classes,
 * interface diamond, method and constructor level variables, recursive bound, wildcard upper/lower bounds,
 * generic array and raw type.
 */
public final class ContractTypes {

    private ContractTypes() {
    }

    // ---------------------------------------------------------------- interface diamond

    public interface Seed<P> {
        P seed();
    }

    public interface Left<P> extends Seed<P> {
    }

    public interface Right<P> extends Seed<P> {
    }

    public interface Diamond<Q> extends Left<Q>, Right<Q> {
    }

    public static class DiamondRoot implements Diamond<String> {
        @Override
        public String seed() {
            return "";
        }
    }

    // ---------------------------------------------------------------- owner / inner classes

    public static class Owner<O> {

        public Owner() {
        }

        /**
         * Constructor level variable.
         */
        public <C> Owner(C marker) {
        }

        /**
         * Method level variable.
         */
        public <M> M pick(M in) {
            return in;
        }

        /**
         * Non-static inner class: sees owner generic O.
         */
        public class Inner {
            public O value;
            public O[] values;
        }

        /**
         * Static nested class: isolated from owner generics.
         */
        public static class StaticInner<S> {
            public S value;
            public List<? extends Number> upper;
            public List<? super Integer> lower;
            public Map<? extends Number, ? super Integer> mixed;
            @SuppressWarnings("rawtypes")
            public List raw;
        }
    }

    public static class RootA extends Owner<String> {
        // inherited member type: Owner<String>.Inner
        public Inner inner;
    }

    public static class RootB extends Owner<Integer> {
        // inherited member type: Owner<Integer>.Inner
        public Inner inner;
    }

    // ---------------------------------------------------------------- recursive bound

    public static class Rec<R extends Comparable<R>> {
        public R value;
    }

    public static class RecRoot extends Rec<RecRoot> implements Comparable<RecRoot> {
        @Override
        public int compareTo(RecRoot other) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- same-name variable shadowing

    public static class ShadowBase<T> {
        public T baseField;
    }

    public static class ShadowSub<T> extends ShadowBase<String> {
        public T subField;
    }

    public static class ShadowRoot extends ShadowSub<Integer> {
    }

    // ---------------------------------------------------------------- generics tracking

    public interface Tracker<T> {
    }

    public static class Tracked<U> implements Tracker<U> {
    }

    // ---------------------------------------------------------------- foreign type (for exceptions)

    public static class Foreign<E> {
        public E field;
    }
}
