package ru.vyarus.java.generics.resolver.contract.support

/**
 * Recursive bound: R bounded by Comparable<R>, root fixes R to itself.
 */
class RecHolder<R extends Comparable<R>> {
    R value
}

class RecRoot extends RecHolder<RecRoot> implements Comparable<RecRoot> {
    @Override
    int compareTo(RecRoot other) {
        0
    }
}
