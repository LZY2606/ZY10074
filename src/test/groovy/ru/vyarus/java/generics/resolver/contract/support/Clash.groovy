package ru.vyarus.java.generics.resolver.contract.support

/**
 * Two interfaces declaring a variable with the same name "T" but different declaration identity.
 * A correct resolver must distinguish them by declaration, not by name.
 */
interface Named<T> {
    T named()
}

interface Tagged<T> {
    T tagged()
}

class BothNamed implements Named<String>, Tagged<Integer> {
    @Override
    String named() {
        null
    }

    @Override
    Integer tagged() {
        null
    }
}
