package ru.vyarus.java.generics.resolver.contract.support

/**
 * Hierarchy for generics tracking: root generic Q must be tracked from known Trackable<String>.
 */
interface Trackable<T> {
}

class TrackMiddle<P> implements Trackable<P> {
}

class TrackRoot<Q> extends TrackMiddle<Q> {
}

class TrackHost {
    Trackable<String> known
}
