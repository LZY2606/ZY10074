package ru.vyarus.java.generics.resolver.contract.mutant

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.contract.support.Owner

import java.lang.reflect.Type

/**
 * MUTATION (test double only): builds the inner class context directly from the inner class,
 * dropping the declared owner type (Owner<String>) of the field. Simulates the regression
 * "owner type ignored": results still look plausible (declared bound Object) but the
 * context is wrong.
 */
class OwnerTypeIgnoringResolver {

    static Class resolveInnerField() {
        // bug: context built from raw inner class, owner type of the declaration is lost
        GenericsResolver.resolve(Owner.Inner).resolveFieldClass(Owner.Inner.getField('innerField'))
    }

    static Map<String, Type> ownerGenerics() {
        GenericsResolver.resolve(Owner.Inner).ownerGenericsMap()
    }
}
