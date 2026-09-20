package ru.vyarus.java.generics.resolver.contract

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.contract.support.ContractChecks
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Diamond
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.DiamondRoot
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Foreign
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Owner
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Rec
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.RecRoot
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.RootA
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.RootB
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Seed
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.ShadowBase
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.ShadowRoot
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.ShadowSub
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Tracked
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Tracker
import ru.vyarus.java.generics.resolver.error.WrongGenericsContextException
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification

import java.lang.reflect.Field
import java.util.concurrent.Callable
import java.util.concurrent.Executors

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.lower
import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.param
import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.upper

/**
 * Contract tests for the public context api over a composed type family
 * (inner classes, interface diamond, method/constructor variables, recursive bound,
 * wildcards, generic array, raw type).
 */
class ContextApiContractTest extends Specification {

    def "Resolve and resolveType over composed hierarchy"() {

        expect: "interface diamond resolves to the same variable value"
        def diamond = GenericsResolver.resolve(DiamondRoot)
        diamond.type(Seed).generic('P') == String
        diamond.resolveType(Seed.getMethod('seed').genericReturnType) == String

        and: "recursive bound"
        def rec = GenericsResolver.resolve(RecRoot)
        rec.type(Rec).generic('R') == RecRoot
        rec.resolveType(Rec.getDeclaredField('value').genericType) == RecRoot

        and: "wildcard bounds and raw type"
        def staticInner = GenericsResolver.resolve(Owner.StaticInner)
        // upper wildcard bound is resolved to the bound itself, lower bound stays a wildcard
        staticInner.resolveType(Owner.StaticInner.getDeclaredField('upper').genericType) ==
                param(List, Number)
        staticInner.resolveType(Owner.StaticInner.getDeclaredField('mixed').genericType) ==
                param(Map, Number, lower(Integer))
        staticInner.resolveType(Owner.StaticInner.getDeclaredField('raw').genericType) == List
        staticInner.resolveFieldGenerics(Owner.StaticInner.getDeclaredField('raw')) == [Object]

        and: "same-name variables resolved by declaration identity"
        def shadow = GenericsResolver.resolve(ShadowRoot)
        ContractChecks.checkShadowedDeclarations(
                shadow.resolveClass(ShadowBase.getDeclaredField('baseField').genericType),
                shadow.resolveClass(ShadowSub.getDeclaredField('subField').genericType))
    }

    def "Inner class context inherits owner generics"() {

        given:
        def context = GenericsResolver.resolve(RootA)

        when: "inlying context for non-static inner class"
        def inner = context.fieldType(RootA.getDeclaredField('inner'))

        then: "owner generic visible, generic array resolved"
        inner.inlying
        inner.rootContext().is(context)
        inner.ownerClass() == Owner
        inner.ownerGenericsMap() == ['O': String]
        inner.resolveFieldType(Owner.Inner.getDeclaredField('values')) == String[]
        ContractChecks.checkOwnerPropagation(inner.resolveFieldClass(Owner.Inner.getDeclaredField('value')))
    }

    def "Type navigation keeps substituted values per root"() {

        expect: "same class resolved differently under different roots"
        GenericsResolver.resolve(RootA).type(Owner).generic('O') == String
        GenericsResolver.resolve(RootB).type(Owner).generic('O') == Integer

        and: "diamond branches agree"
        def diamond = GenericsResolver.resolve(DiamondRoot)
        diamond.type(Seed).generic('P') == String
        diamond.type(Diamond).generic('Q') == String
    }

    def "Method context exposes method level variables"() {

        given:
        def context = GenericsResolver.resolve(RootA)
        def pick = Owner.getMethod('pick', Object)

        when:
        def method = context.method(pick)

        then: "method variable unbound, class generics still visible"
        method.currentMethod() == pick
        method.methodGenericsMap() == ['M': Object]
        method.generic('M') == Object
        method.generic('O') == String
        method.resolveReturnClass() == Object
        method.resolveParameterType(0) == Object

        and: "parent context not polluted"
        context.type(Owner).genericsMap() == ['O': String]
        context.currentClass() == RootA
    }

    def "Constructor context exposes constructor level variables"() {

        given:
        def context = GenericsResolver.resolve(RootA)
        def ctor = Owner.getConstructor(Object)

        when:
        def ctorContext = context.constructor(ctor)

        then: "constructor variable unbound, class generics still visible"
        ctorContext.currentConstructor() == ctor
        ctorContext.constructorGenericsMap() == ['C': Object]
        ctorContext.generic('O') == String
        ctorContext.resolveParameterType(0) == Object

        and: "parent context not polluted"
        context.type(Owner).genericsMap() == ['O': String]
    }

    def "Track generics through direct and diamond hierarchies"() {

        expect:
        GenericsUtils.trackGenerics(Tracked, param(Tracker, String)) == param(Tracked, String)
        GenericsUtils.trackGenerics(Diamond, param(Seed, String)) == param(Diamond, String)
    }

    def "Inlying context carries known generics without polluting root"() {

        given:
        def context = GenericsResolver.resolve(RootA)

        when:
        def inlying = context.inlyingType(param(Owner.StaticInner, Long))

        then: "inlying context resolved from provided type"
        inlying.inlying
        inlying.rootContext().is(context)
        inlying.generic('S') == Long
        inlying.resolveFieldClass(Owner.StaticInner.getDeclaredField('value')) == Long
        ContractChecks.checkLowerBoundPreserved(
                inlying.resolveType(Owner.StaticInner.getDeclaredField('lower').genericType))

        and: "root context unchanged"
        !context.inlying
        context.type(Owner).genericsMap() == ['O': String]
    }

    def "Child contexts do not pollute parent variable mapping"() {

        given:
        def parent = GenericsResolver.resolve(RootA)
        def before = parent.type(Owner).genericsMap()

        when: "drill down into child contexts and return back"
        def inner = parent.fieldType(RootA.getDeclaredField('inner'))
        inner.resolveFieldClass(Owner.Inner.getDeclaredField('value'))
        parent.method(Owner.getMethod('pick', Object)).resolveReturnClass()
        parent.constructor(Owner.getConstructor(Object)).resolveParameterType(0)
        def back = inner.rootContext()

        then: "parent mapping intact"
        parent.type(Owner).genericsMap() == before
        back.type(Owner).genericsMap() == before
        parent.type(Owner).generic('O') == String
        parent.currentClass() == RootA
    }

    def "Structural cache shared but substituted values isolated"() {

        expect: "same root class reuses cached structure"
        GenericsResolver.resolve(RootA).genericsInfo.is(GenericsResolver.resolve(RootA).genericsInfo)
        GenericsResolver.resolve(Owner).genericsInfo.is(GenericsResolver.resolve(Owner).genericsInfo)

        and: "different roots never share substituted values"
        GenericsResolver.resolve(RootA).type(Owner).generic('O') == String
        GenericsResolver.resolve(RootB).type(Owner).generic('O') == Integer

        and: "re-resolution after other root stays consistent"
        GenericsResolver.resolve(RootA).type(Owner).generic('O') == String

        and: "inlying inner values stay root-specific"
        GenericsResolver.resolve(RootA).fieldType(RootA.getDeclaredField('inner'))
                .resolveFieldClass(Owner.Inner.getDeclaredField('value')) == String
        GenericsResolver.resolve(RootB).fieldType(RootB.getDeclaredField('inner'))
                .resolveFieldClass(Owner.Inner.getDeclaredField('value')) == Integer
    }

    def "Concurrent resolution with different roots matches sequential results"() {

        given: "sequential reference values"
        def refA = probe(RootA, RootA.getDeclaredField('inner'))
        def refB = probe(RootB, RootB.getDeclaredField('inner'))

        when: "both roots resolved concurrently"
        def pool = Executors.newFixedThreadPool(8)
        def futures = (0..<40).collect { i ->
            pool.submit({ -> i % 2 == 0 ? probe(RootA, RootA.getDeclaredField('inner'))
                    : probe(RootB, RootB.getDeclaredField('inner')) } as Callable)
        }
        def results = futures.collect { it.get() }
        pool.shutdown()

        then: "all concurrent results equal sequential reference"
        results.eachWithIndex { res, i ->
            assert res == (i % 2 == 0 ? refA : refB)
        }
    }

    def "Exception reports correct declaration with stable hierarchy order"() {

        when:
        GenericsResolver.resolve(RootA).resolveClass(Foreign.getDeclaredField('field').genericType)

        then:
        def ex = thrown(WrongGenericsContextException)
        ex.genericName == 'E'
        ex.genericSource == Foreign
        ex.contextType == RootA
        ex.message.replace('\r', '') == "Type E contains generic 'E' (defined on ContractTypes.Foreign<E>) " +
                "and can't be resolved in context of current class ContractTypes.RootA. " +
                "Generic does not belong to any type in current context hierarchy:\n" +
                "class ContractTypes.RootA\n" +
                "  extends ContractTypes.Owner<String>\n"
    }

    private static Map probe(Class root, Field innerField) {
        def context = GenericsResolver.resolve(root)
        def inner = context.fieldType(innerField)
        [owner : context.type(Owner).generic('O'),
         value : inner.resolveFieldClass(Owner.Inner.getDeclaredField('value')),
         values: inner.resolveFieldType(Owner.Inner.getDeclaredField('values')),
         pick  : context.method(Owner.getMethod('pick', Object)).resolveReturnClass(),
         ctor  : context.constructor(Owner.getConstructor(Object)).resolveParameterType(0)]
    }
}
