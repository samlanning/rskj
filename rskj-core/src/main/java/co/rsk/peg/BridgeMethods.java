/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.peg;

import org.ethereum.config.BlockchainConfig;
import org.ethereum.core.CallTransaction;
import org.ethereum.db.ByteArrayWrapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This enum holds the basic information of the Bridge contract methods: the ABI, the cost and the implementation.
 */
public enum BridgeMethods {
    ADD_FEDERATOR_PUBLIC_KEY(
            CallTransaction.Function.fromSignature(
                    "addFederatorPublicKey",
                    new String[]{"bytes"},
                    new String[]{"int256"}
            ),
            fixedCost(13000L),
            (BridgeMethodExecutorTyped) Bridge::addFederatorPublicKey,
            blockchainConfig -> !blockchainConfig.isRskip123(),
            false
    ),
    ADD_FEDERATOR_PUBLIC_KEY_MULTIKEY(
            CallTransaction.Function.fromSignature(
                    "addFederatorPublicKeyMultikey",
                    new String[]{"bytes", "bytes", "bytes"},
                    new String[]{"int256"}
            ),
            fixedCost(13000L),
            (BridgeMethodExecutorTyped) Bridge::addFederatorPublicKeyMultikey,
            blockchainConfig -> blockchainConfig.isRskip123(),
            false
    ),
    ADD_LOCK_WHITELIST_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "addLockWhitelistAddress",
                    new String[]{"string", "int256"},
                    new String[]{"int256"}
            ),
            fixedCost(25000L),
            (BridgeMethodExecutorTyped) Bridge::addOneOffLockWhitelistAddress,
            blockChainConfig -> !blockChainConfig.isRskip87(),
            false
    ),
    ADD_ONE_OFF_LOCK_WHITELIST_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "addOneOffLockWhitelistAddress",
                    new String[]{"string", "int256"},
                    new String[]{"int256"}
            ),
            fixedCost(25000L), // using same gas estimation as ADD_LOCK_WHITELIST_ADDRESS
            (BridgeMethodExecutorTyped) Bridge::addOneOffLockWhitelistAddress,
            blockChainConfig -> blockChainConfig.isRskip87(),
            false
    ),
    ADD_UNLIMITED_LOCK_WHITELIST_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "addUnlimitedLockWhitelistAddress",
                    new String[]{"string"},
                    new String[]{"int256"}
            ),
            fixedCost(25000L), // using same gas estimation as ADD_LOCK_WHITELIST_ADDRESS
            (BridgeMethodExecutorTyped) Bridge::addUnlimitedLockWhitelistAddress,
            blockChainConfig -> blockChainConfig.isRskip87(),
            false
    ),
    ADD_SIGNATURE(
            CallTransaction.Function.fromSignature(
                    "addSignature",
                    new String[]{"bytes", "bytes[]", "bytes"},
                    new String[]{}
            ),
            fixedCost(70000L),
            Bridge.activeAndRetiringFederationOnly((BridgeMethodExecutorVoid) Bridge::addSignature, "addSignature"),
            false
    ),
    COMMIT_FEDERATION(
            CallTransaction.Function.fromSignature(
                    "commitFederation",
                    new String[]{"bytes"},
                    new String[]{"int256"}
            ),
            fixedCost(38000L),
            (BridgeMethodExecutorTyped) Bridge::commitFederation,
            false
    ),
    CREATE_FEDERATION(
            CallTransaction.Function.fromSignature(
                    "createFederation",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(11000L),
            (BridgeMethodExecutorTyped) Bridge::createFederation,
            false
    ),
    GET_BTC_BLOCKCHAIN_BEST_CHAIN_HEIGHT(
            CallTransaction.Function.fromSignature(
                    "getBtcBlockchainBestChainHeight",
                    new String[]{},
                    new String[]{"int"}
            ),
            fixedCost(19000L),
            (BridgeMethodExecutorTyped) Bridge::getBtcBlockchainBestChainHeight,
            true
    ),
    GET_BTC_BLOCKCHAIN_INITIAL_BLOCK_HEIGHT(
            CallTransaction.Function.fromSignature(
                    "getBtcBlockchainInitialBlockHeight",
                    new String[]{},
                    new String[]{"int"}
            ),
            fixedCost(20000L),
            (BridgeMethodExecutorTyped) Bridge::getBtcBlockchainInitialBlockHeight,
            blockchainConfig -> blockchainConfig.isRskip89(),
            true
    ),
    GET_BTC_BLOCKCHAIN_BLOCK_LOCATOR(
            CallTransaction.Function.fromSignature(
                    "getBtcBlockchainBlockLocator",
                    new String[]{},
                    new String[]{"string[]"}
            ),
            fixedCost(76000L),
            (BridgeMethodExecutorTyped) Bridge::getBtcBlockchainBlockLocator,
            blockchainConfig -> !blockchainConfig.isRskip89(),
            true
    ),
    GET_BTC_BLOCKCHAIN_BLOCK_HASH_AT_DEPTH(
            CallTransaction.Function.fromSignature(
                    "getBtcBlockchainBlockHashAtDepth",
                    new String[]{"int256"},
                    new String[]{"bytes"}
            ),
            fixedCost(20000L),
            (BridgeMethodExecutorTyped) Bridge::getBtcBlockchainBlockHashAtDepth,
            blockchainConfig -> blockchainConfig.isRskip89(),
            true
    ),
    GET_BTC_TRANSACTION_CONFIRMATIONS(
            CallTransaction.Function.fromSignature(
                    "getBtcTransactionConfirmations",
                    new String[]{"bytes32", "bytes32", "uint256", "bytes32[]"},
                    new String[]{"int256"}
            ),
            fixedCost(22000L), //TODO ESTIMATE GAS
            (BridgeMethodExecutorTyped) Bridge::getBtcTransactionConfirmations,
            blockchainConfig -> blockchainConfig.isRskipGetBtcTransactionConfirmations(),
            false
    ),
    GET_BTC_TX_HASH_PROCESSED_HEIGHT(
            CallTransaction.Function.fromSignature(
                    "getBtcTxHashProcessedHeight",
                    new String[]{"string"},
                    new String[]{"int64"}
            ),
            fixedCost(22000L),
            (BridgeMethodExecutorTyped) Bridge::getBtcTxHashProcessedHeight,
            true
    ),
    GET_FEDERATION_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "getFederationAddress",
                    new String[]{},
                    new String[]{"string"}
            ),
            fixedCost(11000L),
            (BridgeMethodExecutorTyped) Bridge::getFederationAddress,
            true
    ),
    GET_FEDERATION_CREATION_BLOCK_NUMBER(
            CallTransaction.Function.fromSignature(
                    "getFederationCreationBlockNumber",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(10000L),
            (BridgeMethodExecutorTyped) Bridge::getFederationCreationBlockNumber,
            true
    ),
    GET_FEDERATION_CREATION_TIME(
            CallTransaction.Function.fromSignature(
                    "getFederationCreationTime",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(10000L),
            (BridgeMethodExecutorTyped) Bridge::getFederationCreationTime,
            true
    ),
    GET_FEDERATION_SIZE(
            CallTransaction.Function.fromSignature(
                    "getFederationSize",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(10000L),
            (BridgeMethodExecutorTyped) Bridge::getFederationSize,
            true
    ),
    GET_FEDERATION_THRESHOLD(
            CallTransaction.Function.fromSignature(
                    "getFederationThreshold",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(11000L),
            (BridgeMethodExecutorTyped) Bridge::getFederationThreshold,
            true
    ),
    GET_FEDERATOR_PUBLIC_KEY(
            CallTransaction.Function.fromSignature(
                    "getFederatorPublicKey",
                    new String[]{"int256"},
                    new String[]{"bytes"}
            ),
            fixedCost(10000L),
            (BridgeMethodExecutorTyped) Bridge::getFederatorPublicKey,
            blockChainConfig -> !blockChainConfig.isRskip123(),
            true
    ),
    GET_FEDERATOR_PUBLIC_KEY_OF_TYPE(
            CallTransaction.Function.fromSignature(
                    "getFederatorPublicKeyOfType",
                    new String[]{"int256", "string"},
                    new String[]{"bytes"}
            ),
            fixedCost(10000L),
            (BridgeMethodExecutorTyped) Bridge::getFederatorPublicKeyOfType,
            blockChainConfig -> blockChainConfig.isRskip123(),
            true
    ),
    GET_FEE_PER_KB(
            CallTransaction.Function.fromSignature(
                    "getFeePerKb",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(2000L),
            (BridgeMethodExecutorTyped) Bridge::getFeePerKb,
            true
    ),
    GET_LOCK_WHITELIST_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "getLockWhitelistAddress",
                    new String[]{"int256"},
                    new String[]{"string"}
            ),
            fixedCost(16000L),
            (BridgeMethodExecutorTyped) Bridge::getLockWhitelistAddress,
            true
    ),
    GET_LOCK_WHITELIST_ENTRY_BY_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "getLockWhitelistEntryByAddress",
                    new String[]{"string"},
                    new String[]{"int256"}
            ),
            fixedCost(16000L),
            (BridgeMethodExecutorTyped) Bridge::getLockWhitelistEntryByAddress,
            blockchainConfig -> blockchainConfig.isRskip87(),
            true
    ),
    GET_LOCK_WHITELIST_SIZE(
            CallTransaction.Function.fromSignature(
                    "getLockWhitelistSize",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(16000L),
            (BridgeMethodExecutorTyped) Bridge::getLockWhitelistSize,
            true
    ),
    GET_MINIMUM_LOCK_TX_VALUE(
            CallTransaction.Function.fromSignature(
                    "getMinimumLockTxValue",
                    new String[]{},
                    new String[]{"int"}
            ),
            fixedCost(2000L),
            (BridgeMethodExecutorTyped) Bridge::getMinimumLockTxValue,
            true
    ),
    GET_PENDING_FEDERATION_HASH(
            CallTransaction.Function.fromSignature(
                    "getPendingFederationHash",
                    new String[]{},
                    new String[]{"bytes"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getPendingFederationHash,
            true
    ),
    GET_PENDING_FEDERATION_SIZE(
            CallTransaction.Function.fromSignature(
                    "getPendingFederationSize",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getPendingFederationSize,
            true
    ),
    GET_PENDING_FEDERATOR_PUBLIC_KEY(
            CallTransaction.Function.fromSignature(
                    "getPendingFederatorPublicKey",
                    new String[]{"int256"},
                    new String[]{"bytes"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getPendingFederatorPublicKey,
            blockChainConfig -> !blockChainConfig.isRskip123(),
            true
    ),
    GET_PENDING_FEDERATOR_PUBLIC_KEY_OF_TYPE(
            CallTransaction.Function.fromSignature(
                    "getPendingFederatorPublicKeyOfType",
                    new String[]{"int256", "string"},
                    new String[]{"bytes"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getPendingFederatorPublicKeyOfType,
            blockChainConfig -> blockChainConfig.isRskip123(),
            true
    ),
    GET_RETIRING_FEDERATION_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederationAddress",
                    new String[]{},
                    new String[]{"string"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederationAddress,
            true
    ),
    GET_RETIRING_FEDERATION_CREATION_BLOCK_NUMBER(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederationCreationBlockNumber",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederationCreationBlockNumber,
            true
    ),
    GET_RETIRING_FEDERATION_CREATION_TIME(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederationCreationTime",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederationCreationTime,
            true
    ),
    GET_RETIRING_FEDERATION_SIZE(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederationSize",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederationSize,
            true
    ),
    GET_RETIRING_FEDERATION_THRESHOLD(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederationThreshold",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederationThreshold,
            true
    ),
    GET_RETIRING_FEDERATOR_PUBLIC_KEY(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederatorPublicKey",
                    new String[]{"int256"},
                    new String[]{"bytes"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederatorPublicKey,
            blockChainConfig -> !blockChainConfig.isRskip123(),
            true
    ),
    GET_RETIRING_FEDERATOR_PUBLIC_KEY_OF_TYPE(
            CallTransaction.Function.fromSignature(
                    "getRetiringFederatorPublicKeyOfType",
                    new String[]{"int256", "string"},
                    new String[]{"bytes"}
            ),
            fixedCost(3000L),
            (BridgeMethodExecutorTyped) Bridge::getRetiringFederatorPublicKeyOfType,
            blockChainConfig -> blockChainConfig.isRskip123(),
            true
    ),
    GET_STATE_FOR_BTC_RELEASE_CLIENT(
            CallTransaction.Function.fromSignature(
                    "getStateForBtcReleaseClient",
                    new String[]{},
                    new String[]{"bytes"}
            ),
            fixedCost(4000L),
            (BridgeMethodExecutorTyped) Bridge::getStateForBtcReleaseClient,
            true
    ),
    GET_STATE_FOR_DEBUGGING(
            CallTransaction.Function.fromSignature(
                    "getStateForDebugging",
                    new String[]{},
                    new String[]{"bytes"}
            ),
            fixedCost(3_000_000L),
            (BridgeMethodExecutorTyped) Bridge::getStateForDebugging,
            true
    ),
    IS_BTC_TX_HASH_ALREADY_PROCESSED(
            CallTransaction.Function.fromSignature(
                    "isBtcTxHashAlreadyProcessed",
                    new String[]{"string"},
                    new String[]{"bool"}
            ),
            fixedCost(23000L),
            (BridgeMethodExecutorTyped) Bridge::isBtcTxHashAlreadyProcessed,
            true
    ),
    RECEIVE_HEADERS(
            CallTransaction.Function.fromSignature(
                    "receiveHeaders",
                    new String[]{"bytes[]"},
                    new String[]{}
            ),
            fromMethod(Bridge::receiveHeadersGetCost),
            Bridge.activeAndRetiringFederationOnly((BridgeMethodExecutorVoid) Bridge::receiveHeaders, "receiveHeaders"),
            false
    ),
    REGISTER_BTC_TRANSACTION(
            CallTransaction.Function.fromSignature(
                    "registerBtcTransaction",
                    new String[]{"bytes", "int", "bytes"},
                    new String[]{}
            ),
            fixedCost(22000L),
            Bridge.activeAndRetiringFederationOnly((BridgeMethodExecutorVoid) Bridge::registerBtcTransaction, "registerBtcTransaction"),
            false
    ),
    RELEASE_BTC(
            CallTransaction.Function.fromSignature(
                    "releaseBtc",
                    new String[]{},
                    new String[]{}
            ),
            fixedCost(23000L),
            (BridgeMethodExecutorVoid) Bridge::releaseBtc,
            false
    ),
    REMOVE_LOCK_WHITELIST_ADDRESS(
            CallTransaction.Function.fromSignature(
                    "removeLockWhitelistAddress",
                    new String[]{"string"},
                    new String[]{"int256"}
            ),
            fixedCost(24000L),
            (BridgeMethodExecutorTyped) Bridge::removeLockWhitelistAddress,
            false
    ),
    ROLLBACK_FEDERATION(
            CallTransaction.Function.fromSignature(
                    "rollbackFederation",
                    new String[]{},
                    new String[]{"int256"}
            ),
            fixedCost(12000L),
            (BridgeMethodExecutorTyped) Bridge::rollbackFederation,
            false
    ),
    SET_LOCK_WHITELIST_DISABLE_BLOCK_DELAY(
            CallTransaction.Function.fromSignature(
                    "setLockWhitelistDisableBlockDelay",
                    new String[]{"int256"},
                    new String[]{"int256"}
            ),
            fixedCost(24000L),
            (BridgeMethodExecutorTyped) Bridge::setLockWhitelistDisableBlockDelay,
            false
    ),
    UPDATE_COLLECTIONS(
            CallTransaction.Function.fromSignature(
                    "updateCollections",
                    new String[]{},
                    new String[]{}
            ),
            fixedCost(48000L),
            Bridge.activeAndRetiringFederationOnly((BridgeMethodExecutorVoid) Bridge::updateCollections, "updateCollections"),
            false
    ),
    VOTE_FEE_PER_KB(
            CallTransaction.Function.fromSignature(
                    "voteFeePerKbChange",
                    new String[]{"int256"},
                    new String[]{"int256"}
            ),
            fixedCost(10000L),
            (BridgeMethodExecutorTyped) Bridge::voteFeePerKbChange,
            false
    );

    private interface CostProvider {
        long getCost(Bridge bridge, BlockchainConfig config, Object[] args);
    }

    private interface BridgeCostProvider {
        long getCost(Bridge bridge, Object[] args);
    }

    private static CostProvider fixedCost(long cost) {
        return (Bridge bridge, BlockchainConfig config, Object[] args) -> cost;
    }

    private static CostProvider fromMethod(BridgeCostProvider bridgeCostProvider) {
        return (Bridge bridge, BlockchainConfig config, Object[] args) -> bridgeCostProvider.getCost(bridge, args);
    }

    private static final Map<ByteArrayWrapper, BridgeMethods> SIGNATURES = Stream.of(BridgeMethods.values())
                        .collect(Collectors.toMap(
                            m -> new ByteArrayWrapper(m.getFunction().encodeSignature()),
                            Function.identity()
                        ));
    private final CallTransaction.Function function;
    private final CostProvider costProvider;
    private final Function<BlockchainConfig, Boolean> isEnabledFunction;
    private final BridgeMethodExecutor executor;
    private final boolean onlyAllowsLocalCalls;

    BridgeMethods(CallTransaction.Function function, CostProvider costProvider, BridgeMethodExecutor executor, boolean onlyAllowsLocalCalls) {
        this(function, costProvider, executor, blockchainConfig -> Boolean.TRUE, onlyAllowsLocalCalls);
    }

    BridgeMethods(CallTransaction.Function function, CostProvider costProvider, BridgeMethodExecutor executor, Function<BlockchainConfig, Boolean> isEnabled, boolean onlyAllowsLocalCalls) {
        this.function = function;
        this.costProvider = costProvider;
        this.executor = executor;
        this.isEnabledFunction = isEnabled;
        this.onlyAllowsLocalCalls = onlyAllowsLocalCalls;
    }

    public static Optional<BridgeMethods> findBySignature(byte[] encoding) {
        return Optional.ofNullable(SIGNATURES.get(new ByteArrayWrapper(encoding)));
    }

    public CallTransaction.Function getFunction() {
        return function;
    }
    public Boolean isEnabled(BlockchainConfig blockchainConfig) {
        return this.isEnabledFunction.apply(blockchainConfig);
    }

    public long getCost(Bridge bridge, BlockchainConfig config, Object[] args) {
        return costProvider.getCost(bridge, config, args);
    }

    public BridgeMethodExecutor getExecutor() {
        return executor;
    }

    public boolean onlyAllowsLocalCalls() {
        return onlyAllowsLocalCalls;
    }

    public interface BridgeMethodExecutor {
        Optional<?> execute(Bridge self, Object[] args) throws Exception;
    }

    private interface BridgeMethodExecutorTyped<T> extends BridgeMethodExecutor {
        @Override
        default Optional<T> execute(Bridge self, Object[] args) throws Exception {
            return Optional.ofNullable(executeTyped(self, args));
        }

        T executeTyped(Bridge self, Object[] args) throws Exception;
    }

    private interface BridgeMethodExecutorVoid extends BridgeMethodExecutor {
        @Override
        default Optional<?> execute(Bridge self, Object[] args) throws Exception {
            executeVoid(self, args);
            return Optional.empty();
        }

        void executeVoid(Bridge self, Object[] args) throws Exception;
    }
}
