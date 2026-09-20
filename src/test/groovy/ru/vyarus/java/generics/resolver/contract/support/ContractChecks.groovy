package ru.vyarus.java.generics.resolver.contract.support

import ru.vyarus.java.generics.resolver.util.type.TypeFactory

import java.lang.reflect.Type

/**
 * Shared contract assertions. The very same checks are applied to the real library
 * (must pass) and to mutated resolver test doubles (must fail), proving that the
 * contract actually detects erroneous implementations.
 */
class ContractChecks {

    /**
     * Non-static inner class must see substituted owner generic.
     */
    static void checkOwnerPropagation(Type resolvedInnerValue) {
        assert resolvedInnerValue == String:
                "Inner class field must be resolved through owner type to String but was $resolvedInnerValue"
    }

    /**
     * Variables with the same name declared on different classes must be resolved
     * by declaration identity, not by name.
     */
    static void checkShadowedDeclarations(Type baseValue, Type subValue) {
        assert baseValue == String:
                "ShadowBase.T must resolve to String but was $baseValue"
        assert subValue == Integer:
                "ShadowSub.T must resolve to Integer but was $subValue"
    }

    /**
     * Wildcard lower bound must remain a lower bound after resolution.
     */
    static void checkLowerBoundPreserved(Type resolvedLowerField) {
        assert resolvedLowerField == TypeFactory.param(List, TypeFactory.lower(Integer)):
                "Lower wildcard bound must stay '? super Integer' but was $resolvedLowerField"
    }
}
