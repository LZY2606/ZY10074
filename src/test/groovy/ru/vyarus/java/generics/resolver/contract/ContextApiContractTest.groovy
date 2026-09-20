package ru.vyarus.java.generics.resolver.contract

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.contract.support.BothNamed
import ru.vyarus.java.generics.resolver.contract.support.ContractChecks
import ru.vyarus.java.generics.resolver.contract.support.CtorHost
import ru.vyarus.java.generics.resolver.contract.support.Foreign
import ru.vyarus.java.generics.resolver.contract.support.Leg
import ru.vyarus.java.generics.resolver.contract.support.LeftLeg
import ru.vyarus.java.generics.resolver.contract.support.MethodHost
import ru.vyarus.java.generics.resolver.contract.support.MethodRoot
import ru.vyarus.java.generics.resolver.contract.support.Middle
import ru.vyarus.java.generics.resolver.contract.support.Named
import ru.vyarus.java.generics.resolver.contract.support.NumberWalker
import ru.vyarus.java.generics.resolver.contract.support.OtherOwnerRoot
import ru.vyarus.java.generics.resolver.contract.support.Owner
import ru.vyarus.java.generics.resolver.contract.support.OwnerRoot
import ru.vyarus.java.generics.resolver.contract.support.RecHolder
import ru.vyarus.java.generics.resolver.contract.support.RecRoot
import ru.vyarus.java.generics.resolver.contract.support.RightLeg
import ru.vyarus.java.generics.resolver.contract.support.StringWalker
import ru.vyarus.java.generics.resolver.contract.support.Tagged
import ru.vyarus.java.generics.resolver.contract.support.TrackHost
import ru.vyarus.java.generics.resolver.contract.support.TrackMiddle
import ru.vyarus.java.generics.resolver.contract.support.TrackRoot
import ru.vyarus.java.generics.resolver.contract.support.Trackable
import ru.vyarus.java.generics.resolver.contract.support.WildcardHost
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import ru.vyarus.java.generics.resolver.error.WrongGenericsContextException
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.type.TypeFactory
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Contract tests for the public context API: every entry point is checked against
 * a hierarchy combining inner classes (non-static, shadowing, static), interface diamond,
 * method/constructor variables, recursive bounds, wildcards, generic arrays and raw types.
 */
class ContextApiContractTest extends Specification {

    def "resolve: same middle class under different roots keeps own values"() {

        when: "same class Middle resolved from two roots"
        def root = GenericsResolver.resolve(OwnerRoot)
        def other = GenericsResolver.resolve(OtherOwnerRoot)
        then: "structural descriptor cached per root, values not shared"
        root.genericsInfo.is(GenericsResolver.resolve(OwnerRoot).genericsInfo)
        !root.genericsInfo.is(other.genericsInfo)
        root.type(Middle).generic('T') == String
        other.type(Middle).generic('T') == Integer

        when: "resolution alternated between roots"
        def values = (1..5).collect {
            [GenericsResolver.resolve(OwnerRoot).type(Middle).generic('T'),
             GenericsResolver.resolve(OtherOwnerRoot).type(Middle).generic('T')]
        }
        then: "no substituted value leaks through the shared structure cache"
        values.every { it == [String, Integer] }
    }

    def "type: diamond resolves single variable through both paths"() {

        when: "diamond hierarchy resolved"
        def context = GenericsResolver.resolve(StringWalker)
        then: "same variable value via left and right path"
        context.type(Leg).generic('T') == String
        context.type(LeftLeg).generic('T') == String
        context.type(RightLeg).generic('T') == String
        and: "method declared on the common leg resolved with auto context switch"
        context.method(Leg.getMethod('leg')).resolveReturnClass() == String
        and: "second root of the same diamond"
        GenericsResolver.resolve(NumberWalker).type(Leg).generic('T') == Integer
    }

    def "type: same-name variables resolved by declaration identity"() {

        when: "class implements two interfaces declaring variable T"
        def context = GenericsResolver.resolve(BothNamed)
        then: "each declaration keeps its own value"
        ContractChecks.checkSameNameVariableValues(
                context.type(Named).generic('T'),
                context.type(Tagged).generic('T'))
        and: "method return types follow their own declaration"
        context.method(Named.getMethod('named')).resolveReturnClass() == String
        context.method(Tagged.getMethod('tagged')).resolveReturnClass() == Integer
    }

    def "inlying: non-static inner class sees owner generics"() {

        when: "inlying context for inner class field"
        def context = GenericsResolver.resolve(OwnerRoot)
        def inner = context.fieldType(OwnerRoot.getField('inner'))
        then: "owner generic value visible inside inner class"
        ContractChecks.checkInnerSeesOwnerGenerics(
                inner.resolveFieldClass(Owner.Inner.getField('innerField')),
                inner.ownerGenericsMap())
        and: "inlying metadata"
        inner.isInlying()
        inner.currentClass() == Owner.Inner
        inner.ownerClass() == Owner
        inner.rootContext().currentClass() == OwnerRoot
        and: "owner generic inside parameterized field"
        TypeToStringUtils.toStringType(inner.resolveFieldType(Owner.Inner.getField('innerList'))) == 'List<String>'
        and: "direct inlyingType call is equivalent to the field shortcut"
        context.inlyingType(OwnerRoot.getField('inner').genericType).resolveFieldClass(
                Owner.Inner.getField('innerField')) == String
    }

    def "inlying: shadowing inner hides owner generic"() {

        when: "inner class declares its own variable with the same name"
        def shadowed = GenericsResolver.resolve(OwnerRoot).fieldType(OwnerRoot.getField('shadowed'))
        then: "inner declaration wins"
        shadowed.generic('T') == Long
        shadowed.resolveFieldClass(Owner.ShadowingInner.getField('ownField')) == Long
        and: "hidden owner generic is not reported as owner generic"
        shadowed.ownerGenericsMap() == [:]
    }

    def "inlying: static inner does not see owner generics"() {

        when: "static inner class context"
        def stat = GenericsResolver.resolve(OwnerRoot).fieldType(OwnerRoot.getField('stat'))
        then: "only own generic present"
        stat.generic('T') == Double
        stat.resolveFieldClass(Owner.StaticInner.getField('staticField')) == Double
        and: "no owner relation"
        stat.ownerClass() == null
        stat.ownerGenericsMap() == [:]
    }

    def "resolveType: wildcard upper and lower bounds preserved"() {

        when: "wildcard fields resolved"
        def context = GenericsResolver.resolve(WildcardHost)
        then: "upper bounded wildcard flattened to its bound (documented resolveType contract)"
        TypeToStringUtils.toStringType(context.resolveFieldType(WildcardHost.getField('upper'))) ==
                'List<Number>'
        and: "lower bound stays lower (shared contract check)"
        ContractChecks.checkLowerBoundPreserved(context.resolveFieldType(WildcardHost.getField('lower')))
        and: "both bounds in one type: upper flattened, lower preserved"
        TypeToStringUtils.toStringType(context.resolveFieldType(WildcardHost.getField('mixed'))) ==
                'Map<Number, ? super Integer>'
        and: "wildcard does not affect raw class resolution"
        context.resolveFieldClass(WildcardHost.getField('lower')) == List
    }

    def "resolveType: generic array component resolved"() {

        when: "generic array field"
        def context = GenericsResolver.resolve(WildcardHost)
        def arrayField = WildcardHost.getField('upperArray')
        then: "array class resolved"
        context.resolveFieldClass(arrayField) == List[].class
        and: "component type resolved (upper wildcard flattened by contract)"
        TypeToStringUtils.toStringType(context.resolveFieldType(arrayField)) == 'List<Number>[]'
    }

    def "resolveType: raw type stays raw"() {

        when: "raw field resolved"
        def context = GenericsResolver.resolve(WildcardHost)
        def rawField = WildcardHost.getField('raw')
        then: "raw class preserved"
        context.resolveFieldClass(rawField) == List
        context.resolveFieldType(rawField) == List
        and: "raw class generics resolved as declared bounds"
        context.resolveFieldGenerics(rawField) == [Object]
    }

    def "resolveType: recursive bound resolves to root type"() {

        when: "self-referencing generic"
        def context = GenericsResolver.resolve(RecRoot)
        then: "variable value is the root itself"
        context.type(RecHolder).generic('R') == RecRoot
        context.type(RecHolder).genericType('R') == RecRoot
        context.resolveFieldClass(RecHolder.getDeclaredField('value')) == RecRoot
        context.resolveFieldType(RecHolder.getDeclaredField('value')) == RecRoot
    }

    def "method: method variable scoped to method context and hides class variable"() {

        when: "method with variable hiding the class variable"
        Method hidden = MethodHost.getMethod('hidden', Object)
        def context = GenericsResolver.resolve(MethodRoot)
        def mctx = context.method(hidden)
        then: "method variable resolved to its bound, not to the class value"
        mctx.resolveReturnClass() == Object
        mctx.resolveParameters() == [Object]
        mctx.methodGenericsMap() == [T: Object]
        and: "class variable still visible through class generics map"
        mctx.genericsMap() == [T: String]
        and: "visible generics show the hiding effect"
        mctx.visibleGenericsMap() == [T: Object]

        when: "method without own variables"
        def classVarCtx = context.method(MethodHost.getMethod('classVar'))
        then: "class variable value used"
        classVarCtx.resolveReturnClass() == String

        when: "recursive method bound"
        def maxCtx = context.method(MethodHost.getMethod('max', Comparable, Comparable))
        then: "bound resolved"
        maxCtx.resolveParameters() == [Comparable, Comparable]
        maxCtx.resolveReturnClass() == Comparable

        when: "method variable inside generic array"
        def arrayCtx = context.method(MethodHost.getMethod('arrayOf', Object))
        then: "array component resolved to variable bound"
        arrayCtx.resolveReturnClass() == Object[].class
        TypeToStringUtils.toStringType(arrayCtx.resolveReturnType()) == 'Object[]'
    }

    def "constructor: constructor variable resolved to its bound"() {

        when: "constructor with own variable"
        def cctx = GenericsResolver.resolve(CtorHost).constructor(CtorHost.getConstructor(Number))
        then: "variable resolved to bound"
        cctx.resolveParameters() == [Number]
        cctx.constructorGenericsMap() == [C: Number]
        and: "parameter context navigable"
        cctx.parameterType(0).currentClass() == Number
    }

    def "track: root generic tracked from known subtype"() {

        when: "inlying context for implementation with known middle type generic"
        def context = GenericsResolver.resolve(TrackHost)
        def tracked = context.fieldTypeAs(TrackHost.getDeclaredField('known'), TrackRoot)
        then: "root generic tracked through the hierarchy"
        tracked.generic('Q') == String
        tracked.type(TrackMiddle).generic('P') == String
        tracked.type(Trackable).generic('T') == String

        when: "direct tracking api"
        def map = GenericsTrackingUtils.track(TrackRoot, Trackable, [T: String] as LinkedHashMap)
        then: "root generic mapped"
        map == [Q: String]

        when: "type improvement api"
        Type improved = GenericsUtils.trackGenerics(TrackRoot, TypeFactory.param(Trackable, String))
        then: "root type enriched with tracked generic"
        TypeToStringUtils.toStringType(improved) == 'TrackRoot<String>'
    }

    def "navigation: child to parent context does not pollute variable mapping"() {

        when: "enter method context and return to class context"
        def context = GenericsResolver.resolve(MethodRoot)
        def mctx = context.type(MethodHost).method(MethodHost.getMethod('hidden', Object))
        def back = mctx.type(MethodRoot).type(MethodHost)
        then: "method variable does not leak into class context"
        mctx.visibleGenericsMap() == [T: Object]
        back.visibleGenericsMap() == [T: String]
        back.generic('T') == String
        and: "original context unchanged"
        context.type(MethodHost).visibleGenericsMap() == [T: String]

        when: "enter inlying context and return to root"
        def rootCtx = GenericsResolver.resolve(OwnerRoot)
        def inner = rootCtx.fieldType(OwnerRoot.getField('inner'))
        def root = inner.rootContext()
        then: "root context mapping not affected by inner navigation"
        root.currentClass() == OwnerRoot
        root.type(Owner).genericsMap() == [T: String]
        root.ownerGenericsMap() == [:]
        rootCtx.type(Owner).visibleGenericsMap() == [T: String]

        when: "navigate down the hierarchy and back"
        def roundTrip = GenericsResolver.resolve(OwnerRoot).type(Owner).type(OwnerRoot).type(Middle)
        then: "mapping identical to direct resolution"
        roundTrip.genericsMap() == GenericsResolver.resolve(OwnerRoot).type(Middle).genericsMap()
        roundTrip.currentClass() == Middle
    }

    def "concurrency: concurrent resolution of multiple roots matches single-threaded"() {

        setup: "single-threaded baseline"
        def roots = [OwnerRoot, OtherOwnerRoot, StringWalker, NumberWalker]
        Map<Class, List<Object>> baseline = roots.collectEntries { [(it): contractValues(it)] }
        ExecutorService executor = Executors.newFixedThreadPool(8)

        when: "same resolutions executed concurrently"
        def errors = new ConcurrentLinkedQueue<Throwable>()
        def results = new ConcurrentLinkedQueue<List<Object>>()
        def start = new CountDownLatch(1)
        List<Future<?>> tasks = (1..40).collect { idx ->
            executor.submit({
                try {
                    start.await()
                    def root = roots[idx % roots.size()]
                    results << ([root] + contractValues(root))
                } catch (Throwable ex) {
                    errors << ex
                }
            } as Runnable)
        }
        start.countDown()
        tasks.each { it.get() }
        executor.shutdown()

        then: "no failures and every result matches the single-threaded baseline"
        errors.isEmpty()
        results.size() == 40
        results.every { row -> row[1..-1] == baseline[row[0]] }
    }

    def "exception: wrong context error names correct declaration with stable hierarchy order"() {

        when: "type with foreign variable resolved in root context"
        GenericsResolver.resolve(OwnerRoot).resolveClass(Foreign.getField('foreignField').genericType)
        then: "error points to the exact variable declaration and dumps hierarchy in stable order"
        def ex = thrown(WrongGenericsContextException)
        ex.genericName == 'T'
        ex.genericSource == Foreign
        ex.contextType == OwnerRoot
        ex.message.replace('\r', '') == """Type T contains generic 'T' (defined on Foreign<T>) and can't be resolved in context of current class OwnerRoot. Generic does not belong to any type in current context hierarchy:
class OwnerRoot
  extends Middle<String>
    extends Owner<String>
"""
    }

    def "exception: foreign method navigation error lists diamond hierarchy in stable order"() {

        when: "method from foreign type requested"
        GenericsResolver.resolve(StringWalker).method(Foreign.getMethod('foreignMethod'))
        then: "error contains method declaration and hierarchy with deterministic branch order"
        def ex = thrown(IllegalArgumentException)
        ex.message.replace('\r', '') == """Method 'T foreignMethod()' declaration type Foreign is not present in current hierarchy:
class StringWalker
  extends DiamondWalker<String>
    implements LeftLeg<String>
      extends Leg<String>
    implements RightLeg<String>
      extends Leg<String>
"""
    }

    def "exception: unknown generic error names context type"() {

        when: "not declared generic requested"
        GenericsResolver.resolve(OwnerRoot).generic('UNKNOWN')
        then: "error names generic and context"
        def ex = thrown(UnknownGenericException)
        ex.genericName == 'UNKNOWN'
        ex.contextType == OwnerRoot
        ex.message == "Generic 'UNKNOWN' is not declared on type ${OwnerRoot.name}"
    }

    /**
     * Values checked by the concurrency contract: variable in the middle of the hierarchy
     * and, for the owner roots, the inner class field depending on owner generics.
     */
    private static List<Object> contractValues(Class<?> root) {
        def context = GenericsResolver.resolve(root)
        if (root == OwnerRoot || root == OtherOwnerRoot) {
            return [context.type(Middle).generic('T'),
                    context.fieldType(root.getField('inner'))
                            .resolveFieldClass(Owner.Inner.getField('innerField'))]
        }
        return [context.type(Leg).generic('T')]
    }
}
