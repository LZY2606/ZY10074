package ru.vyarus.java.generics.resolver.contract.mutant

import ru.vyarus.java.generics.resolver.GenericsResolver

import java.lang.reflect.Type

/**
 * MUTATION (test double only): caches resolved variables by name only, ignoring
 * declaration identity. The first resolved value of a name is reused for every
 * other declaration of a variable with the same name.
 */
class NameKeyedVariableResolver {

    /** bug: cache key is the variable name, not (declaration, name) */
    private final Map<String, Type> cache = new HashMap<>()

    Class resolve(Class<?> root, Class<?> declaration, String name) {
        if (!cache.containsKey(name)) {
            cache[name] = GenericsResolver.resolve(root).type(declaration).generic(name)
        }
        cache[name] as Class
    }
}
