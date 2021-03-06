/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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

package co.rsk.core.bc;

import co.rsk.core.Coin;
import co.rsk.core.RskAddress;
import co.rsk.db.StateRootHandler;
import org.bouncycastle.util.encoders.Hex;
import co.rsk.metrics.profilers.Metric;
import co.rsk.metrics.profilers.Profiler;
import co.rsk.metrics.profilers.ProfilerFactory;
import org.ethereum.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BlockExecutor has methods to execute block with its transactions.
 * There are two main use cases:
 * - execute and validate the block final state
 * - execute and complete the block final state
 * <p>
 * Created by ajlopez on 29/07/2016.
 */
public class BlockExecutor {
    private static final Logger logger = LoggerFactory.getLogger("blockexecutor");
    private static final Profiler profiler = ProfilerFactory.getInstance();

    private final Repository repository;
    private final TransactionExecutorFactory transactionExecutorFactory;
    private final StateRootHandler stateRootHandler;

    public BlockExecutor(
            Repository repository,
            TransactionExecutorFactory transactionExecutorFactory,
            StateRootHandler stateRootHandler) {
        this.repository = repository;
        this.transactionExecutorFactory = transactionExecutorFactory;
        this.stateRootHandler = stateRootHandler;
    }

    /**
     * Execute and complete a block.
     *
     * @param block        A block to execute and complete
     * @param parent       The parent of the block.
     */
    public void executeAndFill(Block block, BlockHeader parent) {
        BlockResult result = execute(block, parent, true);
        fill(block, result);
    }

    public void executeAndFillAll(Block block, BlockHeader parent) {
        BlockResult result = executeAll(block, parent);
        fill(block, result);
    }

    public void executeAndFillReal(Block block, BlockHeader parent) {
        BlockResult result = execute(block, parent, false, false);
        if (result != BlockResult.INTERRUPTED_EXECUTION_BLOCK_RESULT) {
            fill(block, result);
        }
    }

    private void fill(Block block, BlockResult result) {
        Metric metric = profiler.start(Profiler.PROFILING_TYPE.FILLING_EXECUTED_BLOCK);
        block.setTransactionsList(result.getExecutedTransactions());
        BlockHeader header = block.getHeader();
        header.setTransactionsRoot(Block.getTxTrie(block.getTransactionsList()).getHash().getBytes());
        header.setReceiptsRoot(result.getReceiptsRoot());
        header.setGasUsed(result.getGasUsed());
        header.setPaidFees(result.getPaidFees());
        block.setStateRoot(result.getStateRoot());
        header.setLogsBloom(result.getLogsBloom());

        block.flushRLP();
        profiler.stop(metric);
    }

    /**
     * Execute and validate the final state of a block.
     *
     * @param block        A block to execute and complete
     * @param parent       The parent of the block.
     * @return true if the block final state is equalBytes to the calculated final state.
     */
    public boolean executeAndValidate(Block block, BlockHeader parent) {
        BlockResult result = execute(block, parent, false);

        return this.validate(block, result);
    }

    /**
     * Validate the final state of a block.
     *
     * @param block        A block to validate
     * @param result       A block result (state root, receipts root, etc...)
     * @return true if the block final state is equalBytes to the calculated final state.
     */
    public boolean validate(Block block, BlockResult result) {
        Metric metric = profiler.start(Profiler.PROFILING_TYPE.BLOCK_FINAL_STATE_VALIDATION);
        if (result == BlockResult.INTERRUPTED_EXECUTION_BLOCK_RESULT) {
            logger.error("Block's execution was interrupted because of an invalid transaction: {} {}.", block.getNumber(), block.getShortHash());
            profiler.stop(metric);
            return false;
        }

        boolean isValidStateRoot = stateRootHandler.validate(block.getHeader(), result);
        if (!isValidStateRoot) {
            logger.error("Block's given State Root doesn't match: {} {} {} != {}", block.getNumber(), block.getShortHash(), Hex.toHexString(block.getStateRoot()), Hex.toHexString(result.getStateRoot()));
            profiler.stop(metric);
            return false;
        }

        if (!Arrays.equals(result.getReceiptsRoot(), block.getReceiptsRoot())) {
            logger.error("Block's given Receipt Hash doesn't match: {} {} != {}", block.getNumber(), block.getShortHash(), Hex.toHexString(result.getReceiptsRoot()));
            profiler.stop(metric);
            return false;
        }

        byte[] resultLogsBloom = result.getLogsBloom();
        byte[] blockLogsBloom = block.getLogBloom();

        if (!Arrays.equals(resultLogsBloom, blockLogsBloom)) {
            String resultLogsBloomString = Hex.toHexString(resultLogsBloom);
            String blockLogsBloomString = Hex.toHexString(blockLogsBloom);

            logger.error("Block's given logBloom Hash doesn't match: {} != {} Block {} {}", resultLogsBloomString, blockLogsBloomString, block.getNumber(), block.getShortHash());
            profiler.stop(metric);
            return false;
        }

        if (result.getGasUsed() != block.getGasUsed()) {
            logger.error("Block's given gasUsed doesn't match: {} != {} Block {} {}", block.getGasUsed(), result.getGasUsed(), block.getNumber(), block.getShortHash());
            profiler.stop(metric);
            return false;
        }

        Coin paidFees = result.getPaidFees();
        Coin feesPaidToMiner = block.getFeesPaidToMiner();

        if (!paidFees.equals(feesPaidToMiner))  {
            logger.error("Block's given paidFees doesn't match: {} != {} Block {} {}", feesPaidToMiner, paidFees, block.getNumber(), block.getShortHash());
            profiler.stop(metric);
            return false;
        }

        List<Transaction> executedTransactions = result.getExecutedTransactions();
        List<Transaction> transactionsList = block.getTransactionsList();

        if (!executedTransactions.equals(transactionsList))  {
            logger.error("Block's given txs doesn't match: {} != {} Block {} {}", transactionsList, executedTransactions, block.getNumber(), block.getShortHash());
            profiler.stop(metric);
            return false;
        }

        profiler.stop(metric);
        return true;
    }

    /**
     * Execute a block, from initial state, returning the final state data.
     *
     * @param block        A block to validate
     * @param parent       The parent of the block to validate
     * @return BlockResult with the final state data.
     */
    public BlockResult execute(Block block, BlockHeader parent, boolean discardInvalidTxs) {
        return execute(block, parent, discardInvalidTxs, false);
    }

    public BlockResult executeAll(Block block, BlockHeader parent) {
        return execute(block, parent, false, true);
    }

    private BlockResult execute(Block block, BlockHeader parent, boolean discardInvalidTxs, boolean ignoreReadyToExecute) {
        logger.trace("applyBlock: block: [{}] tx.list: [{}]", block.getNumber(), block.getTransactionsList().size());

        byte[] lastStateRootHash = stateRootHandler.translate(parent).getBytes();
        Metric metric = profiler.start(Profiler.PROFILING_TYPE.BLOCK_EXECUTE);
        Repository initialRepository = repository.getSnapshotTo(lastStateRootHash);

        Repository track = initialRepository.startTracking();
        int i = 1;
        long totalGasUsed = 0;
        Coin totalPaidFees = Coin.ZERO;
        List<TransactionReceipt> receipts = new ArrayList<>();
        List<Transaction> executedTransactions = new ArrayList<>();

        int txindex = 0;

        for (Transaction tx : block.getTransactionsList()) {
            logger.trace("apply block: [{}] tx: [{}] ", block.getNumber(), i);

            TransactionExecutor txExecutor = transactionExecutorFactory.newInstance(
                    tx,
                    txindex++,
                    block.getCoinbase(),
                    track,
                    block,
                    totalGasUsed
            );
            boolean readyToExecute = txExecutor.init();
            if (!ignoreReadyToExecute && !readyToExecute) {
                if (discardInvalidTxs) {
                    logger.warn("block: [{}] discarded tx: [{}]", block.getNumber(), tx.getHash());
                    continue;
                } else {
                    logger.warn("block: [{}] execution interrupted because of invalid tx: [{}]",
                                block.getNumber(), tx.getHash());
                    profiler.stop(metric);
                    return BlockResult.INTERRUPTED_EXECUTION_BLOCK_RESULT;
                }
            }

            executedTransactions.add(tx);

            txExecutor.execute();
            txExecutor.go();
            txExecutor.finalization();

            logger.trace("tx executed");

            track.commit();

            logger.trace("track commit");

            long gasUsed = txExecutor.getGasUsed();
            totalGasUsed += gasUsed;
            Coin paidFees = txExecutor.getPaidFees();
            if (paidFees != null) {
                totalPaidFees = totalPaidFees.add(paidFees);
            }

            TransactionReceipt receipt = new TransactionReceipt();
            receipt.setGasUsed(gasUsed);
            receipt.setCumulativeGas(totalGasUsed);
            lastStateRootHash = initialRepository.getRoot();
            receipt.setTxStatus(txExecutor.getReceipt().isSuccessful());
            receipt.setTransaction(tx);
            receipt.setLogInfoList(txExecutor.getVMLogs());
            receipt.setStatus(txExecutor.getReceipt().getStatus());

            logger.trace("block: [{}] executed tx: [{}] state: [{}]", block.getNumber(), tx.getHash(),
                         Hex.toHexString(lastStateRootHash));

            logger.trace("tx[{}].receipt", i);

            i++;

            receipts.add(receipt);

            logger.trace("tx done");
        }

        BlockResult result =  new BlockResult(executedTransactions, receipts, lastStateRootHash, totalGasUsed, totalPaidFees);
        profiler.stop(metric);
        return result;
    }

    public interface TransactionExecutorFactory {
        TransactionExecutor newInstance(Transaction tx, int txindex, RskAddress coinbase, Repository track, Block block, long totalGasUsed);
    }
}
