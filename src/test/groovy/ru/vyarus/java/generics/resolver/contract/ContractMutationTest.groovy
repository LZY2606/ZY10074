package ru.vyarus.java.generics.resolver.contract

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.contract.mutant.LowerBoundAsUpperBoundResolver
import ru.vyarus.java.generics.resolver.contract.mutant.NameKeyedVariableResolver
import ru.vyarus.java.generics.resolver.contract.mutant.OwnerTypeIgnoringResolver
import ru.vyarus.java.generics.resolver.contract.support.BothNamed
import ru.vyarus.java.generics.resolver.contract.support.ContractChecks
import ru.vyarus.java.generics.resolver.contract.support.Named
import ru.vyarus.java.generics.resolver.contract.support.Owner
import ru.vyarus.java.generics.resolver.contract.support.OwnerRoot
import ru.vyarus.java.generics.resolver.contract.support.Tagged
import ru.vyarus.java.generics.resolver.contract.support.WildcardHost
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import spock.lang.Specification

/**
 * Proves that the shared contract assertions ({@link ContractChecks}) catch wrong implementations.
 * Each mutation exists only as a test double (see contract.mutant package): production code
 * is never touched. For every mutation the test shows the plausible but wrong result and
 * verifies that the same contract check, which the correct implementation passes, fails
 * with a descriptive error.
 */
class ContractMutationTest extends Specification {

    def "Mutation: owner type ignored"() {

        when: "mutant resolves inner field without the declared owner type"
        Class resolved = OwnerTypeIgnoringResolver.resolveInnerField()
        Map ownerGenerics = OwnerTypeIgnoringResolver.ownerGenerics()
        then: "result looks plausible (declared bound) but the context is wrong"
        resolved == Object
        ownerGenerics == [T: Object]

        when: "the same contract check is applied to the mutant result"
        ContractChecks.checkInnerSeesOwnerGenerics(resolved, ownerGenerics)
        then: "contract violation detected"
        def err = thrown(AssertionError)
        err.message.contains('owner generic value <String>')

        when: "correct implementation"
        def inner = GenericsResolver.resolve(OwnerRoot).fieldType(OwnerRoot.getField('inner'))
        then: "contract check passes"
        ContractChecks.checkInnerSeesOwnerGenerics(
                inner.resolveFieldClass(Owner.Inner.getField('innerField')), inner.ownerGenericsMap())
    }

    def "Mutation: variables cached by name instead of declaration identity"() {

        setup: "mutant resolver with a name-keyed cache"
        def mutant = new NameKeyedVariableResolver()

        when: "Tagged#T resolved first (cache warmed with Integer)"
        Class tagged = mutant.resolve(BothNamed, Tagged, 'T')
        and: "Named#T resolved after"
        Class named = mutant.resolve(BothNamed, Named, 'T')
        then: "both collapse to the first cached value"
        tagged == Integer
        named == Integer

        when: "the same contract check is applied to the mutant result"
        ContractChecks.checkSameNameVariableValues(named, tagged)
        then: "contract violation detected"
        def err = thrown(AssertionError)
        err.message.contains('Named#T')

        when: "correct implementation"
        def context = GenericsResolver.resolve(BothNamed)
        then: "contract check passes"
        ContractChecks.checkSameNameVariableValues(
                context.type(Named).generic('T'), context.type(Tagged).generic('T'))
    }

    def "Mutation: lower wildcard bound treated as upper bound"() {

        when: "mutant resolves the lower bound field moving the bound to the upper side"
        def resolved = LowerBoundAsUpperBoundResolver.resolveLowerField()
        then: "result looks plausible but the bound side is wrong"
        TypeToStringUtils.toStringType(resolved) == 'List<? extends Integer>'

        when: "the same contract check is applied to the mutant result"
        ContractChecks.checkLowerBoundPreserved(resolved)
        then: "contract violation detected"
        def err = thrown(AssertionError)
        err.message.contains('List<? super Integer>')

        when: "correct implementation"
        def real = GenericsResolver.resolve(WildcardHost).resolveFieldType(WildcardHost.getField('lower'))
        then: "contract check passes"
        ContractChecks.checkLowerBoundPreserved(real)
    }
}
