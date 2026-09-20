package ru.vyarus.java.generics.resolver.contract.support

import ru.vyarus.java.generics.resolver.util.type.TypeFactory

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

/**
 * Mutated resolver implementations (test doubles). Each one mimics a single typical
 * implementation mistake. They exist only to prove that contract tests detect them;
 * production code is never touched.
 */
class MutatedResolvers {

    /**
     * Mutation 1: owner type ignored. Inner class variables are resolved only against
     * the inner class declaration scope, enclosing (owner) class generics are never mapped.
     */
    static Type resolveInnerValueIgnoringOwner() {
        def field = ContractTypes.Owner.Inner.getDeclaredField('value')
        TypeVariable variable = field.genericType as TypeVariable
        // buggy scope: only variables declared by the inner class itself
        Map<String, Type> innerOnlyScope = [:]
        ContractTypes.Owner.Inner.typeParameters.each { innerOnlyScope[it.name] = Object }
        return innerOnlyScope[variable.name] ?: Object
    }

    /**
     * Mutation 2: variables cached by name instead of declaration identity. The first
     * binding found while walking the hierarchy wins, so a subclass variable shadows
     * an unrelated superclass variable with the same name.
     */
    static Type resolveShadowedByName(Class variableDeclaration, String fieldName) {
        Map<String, Type> nameCache = [:]
        Class current = ContractTypes.ShadowRoot
        while (current != null && current != Object) {
            Type superType = current.genericSuperclass
            if (superType instanceof ParameterizedType) {
                Class raw = superType.rawType as Class
                raw.typeParameters.eachWithIndex { TypeVariable var, int pos ->
                    // buggy key: variable name only, declaration identity ignored
                    nameCache.putIfAbsent(var.name, superType.actualTypeArguments[pos])
                }
            }
            current = current.superclass
        }
        TypeVariable target = variableDeclaration.getDeclaredField(fieldName).genericType as TypeVariable
        return nameCache[target.name] ?: Object
    }

    /**
     * Mutation 3: wildcard lower bound treated as upper bound.
     */
    static Type resolveLowerBoundAsUpper() {
        def field = ContractTypes.Owner.StaticInner.getDeclaredField('lower')
        WildcardType wildcard = (field.genericType as ParameterizedType).actualTypeArguments[0] as WildcardType
        // buggy bound selection: lower bound used as an upper bound
        Type bound = wildcard.lowerBounds.length > 0 ? wildcard.lowerBounds[0] : wildcard.upperBounds[0]
        return TypeFactory.param(List, TypeFactory.upper(bound))
    }
}
