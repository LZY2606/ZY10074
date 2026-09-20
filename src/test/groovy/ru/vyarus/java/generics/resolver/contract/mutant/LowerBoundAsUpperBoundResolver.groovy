package ru.vyarus.java.generics.resolver.contract.mutant

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.contract.support.WildcardHost
import ru.vyarus.java.generics.resolver.util.type.TypeFactory

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * MUTATION (test double only): resolves the field with the real implementation and then
 * rewrites wildcards, moving lower bounds into upper bounds. Simulates the regression
 * "lower bound treated as upper bound".
 */
class LowerBoundAsUpperBoundResolver {

    static Type resolveLowerField() {
        Type resolved = GenericsResolver.resolve(WildcardHost)
                .resolveFieldType(WildcardHost.getField('lower'))
        rewrite(resolved)
    }

    private static Type rewrite(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] args = type.actualTypeArguments.collect { rewrite(it) } as Type[]
            return TypeFactory.param(type.rawType as Class, args)
        }
        if (type instanceof WildcardType && type.lowerBounds.length > 0) {
            // bug: lower bound moved into upper bounds
            return TypeFactory.upper(type.lowerBounds[0])
        }
        type
    }
}
