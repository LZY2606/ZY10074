package ru.vyarus.java.generics.resolver.contract.support

/**
 * Interface diamond: Leg<T> is reached through both LeftLeg and RightLeg with the same variable.
 */
interface Leg<T> {
    T leg()
}

interface LeftLeg<T> extends Leg<T> {
}

interface RightLeg<T> extends Leg<T> {
}

class DiamondWalker<T> implements LeftLeg<T>, RightLeg<T> {
    @Override
    T leg() {
        null
    }
}

class StringWalker extends DiamondWalker<String> {
}

class NumberWalker extends DiamondWalker<Integer> {
}
