package ru.vyarus.java.generics.resolver.contract.support

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils

import java.lang.reflect.Type

/**
 * Shared contract assertions. Used directly by the main contract test against the real
 * implementation and by the mutation test against mutant doubles to prove the same
 * assertions catch wrong implementations.
 */
class ContractChecks {

    /**
     * Non-static inner class must see owner generic values (owner type of the field declaration
     * must be taken into account).
     */
    static void checkInnerSeesOwnerGenerics(Class resolvedInnerField, Map<String, Type> ownerGenerics) {
        assert resolvedInnerField == String:
                "inner field must see owner generic value <String> but resolved to <${resolvedInnerField}> " +
                "(owner type was likely ignored)"
        assert ownerGenerics == [T: String]:
                "owner generics must be [T: String] but were ${ownerGenerics}"
    }

    /**
     * Variables with the same name declared on different types must be resolved
     * by declaration identity, not by name.
     */
    static void checkSameNameVariableValues(Class namedValue, Class taggedValue) {
        assert namedValue == String:
                "Named#T must resolve to <String> but was <${namedValue}> " +
                "(variables with the same name were likely cached by name, not by declaration)"
        assert taggedValue == Integer:
                "Tagged#T must resolve to <Integer> but was <${taggedValue}>"
    }

    /**
     * Wildcard lower bound must stay a lower bound after resolution.
     */
    static void checkLowerBoundPreserved(Type resolvedLowerField) {
        String rendered = TypeToStringUtils.toStringType(resolvedLowerField)
        assert rendered == 'List<? super Integer>':
                "lower bound must be preserved as <List<? super Integer>> but was <${rendered}> " +
                "(lower bound was likely treated as upper bound)"
    }
}
