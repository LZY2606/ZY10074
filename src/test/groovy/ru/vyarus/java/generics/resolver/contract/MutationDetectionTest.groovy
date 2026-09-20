package ru.vyarus.java.generics.resolver.contract

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.contract.support.ContractChecks
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.Owner
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.RootA
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.ShadowBase
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.ShadowRoot
import ru.vyarus.java.generics.resolver.contract.support.ContractTypes.ShadowSub
import ru.vyarus.java.generics.resolver.contract.support.MutatedResolvers
import spock.lang.Specification

/**
 * Proves that the contract checks detect erroneous resolver implementations.
 * Each mutation lives only in a test double ({@link MutatedResolvers}); production
 * code is never modified. Every case first shows the real implementation passing
 * the shared contract check and then shows the mutated one failing it.
 */
class MutationDetectionTest extends Specification {

    def "Mutation: ignoring owner type is detected by inner class contract"() {

        given: "real implementation passes the contract"
        def real = GenericsResolver.resolve(RootA).fieldType(RootA.getDeclaredField('inner'))
                .resolveFieldClass(Owner.Inner.getDeclaredField('value'))
        ContractChecks.checkOwnerPropagation(real)

        when: "same contract applied to mutated resolver (owner type ignored)"
        ContractChecks.checkOwnerPropagation(MutatedResolvers.resolveInnerValueIgnoringOwner())

        then: "mutation is detected"
        thrown(AssertionError)
    }

    def "Mutation: name-based variable cache is detected by shadowing contract"() {

        given: "real implementation passes the contract"
        def context = GenericsResolver.resolve(ShadowRoot)
        ContractChecks.checkShadowedDeclarations(
                context.resolveClass(ShadowBase.getDeclaredField('baseField').genericType),
                context.resolveClass(ShadowSub.getDeclaredField('subField').genericType))

        when: "same contract applied to mutated resolver (variables cached by name)"
        ContractChecks.checkShadowedDeclarations(
                MutatedResolvers.resolveShadowedByName(ShadowBase, 'baseField'),
                MutatedResolvers.resolveShadowedByName(ShadowSub, 'subField'))

        then: "mutation is detected"
        thrown(AssertionError)
    }

    def "Mutation: lower bound treated as upper is detected by wildcard contract"() {

        given: "real implementation passes the contract"
        def real = GenericsResolver.resolve(Owner.StaticInner)
                .resolveType(Owner.StaticInner.getDeclaredField('lower').genericType)
        ContractChecks.checkLowerBoundPreserved(real)

        when: "same contract applied to mutated resolver (lower bound as upper)"
        ContractChecks.checkLowerBoundPreserved(MutatedResolvers.resolveLowerBoundAsUpper())

        then: "mutation is detected"
        thrown(AssertionError)
    }
}
