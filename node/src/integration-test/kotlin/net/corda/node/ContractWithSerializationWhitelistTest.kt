package net.corda.node

import net.corda.client.rpc.CordaRPCClient
import net.corda.contracts.serialization.whitelist.WhitelistData
import net.corda.core.contracts.TransactionVerificationException.ContractRejection
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.flows.serialization.whitelist.WhitelistFlow
import net.corda.node.services.Permissions
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.driver.internal.incrementalPortAllocation
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.User
import net.corda.testing.node.internal.cordappWithPackages
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertFailsWith

@Suppress("FunctionName")
@RunWith(Parameterized::class)
class ContractWithSerializationWhitelistTest(private val runInProcess: Boolean) {
    companion object {
        const val DATA = 123456L

        @JvmField
        val logger = loggerFor<ContractWithSerializationWhitelistTest>()

        @Parameters
        @JvmStatic
        fun modes() = listOf(Array(1) { true }, Array(1) { false })

        @BeforeClass
        @JvmStatic
        fun checkData() {
            assertNotCordaSerializable<WhitelistData>()
        }
    }

    @Test
    fun `test serialization whitelist`() {
        logger.info("RUN-IN-PROCESS={}", runInProcess)

        val user = User("u", "p", setOf(Permissions.all()))
        driver(DriverParameters(
            portAllocation = incrementalPortAllocation(),
            startNodesInProcess = runInProcess,
            notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = true)),
            cordappsForAllNodes = listOf(
                cordappWithPackages("net.corda.flows.serialization.whitelist").signed(),
                cordappWithPackages("net.corda.contracts.serialization.whitelist").signed()
            )
        )) {
            val badData = WhitelistData(DATA)
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            val ex = assertFailsWith<ContractRejection> {
                CordaRPCClient(hostAndPort = alice.rpcAddress)
                    .start(user.username, user.password)
                    .use { client ->
                        client.proxy.startFlow(::WhitelistFlow, badData)
                            .returnValue
                            .getOrThrow()
                    }
            }
            assertThat(ex)
                .hasMessageContaining("WhitelistData $badData exceeds maximum value!")
        }
    }
}