package com.playtox.transfer;

import com.playtox.transfer.model.Account;
import com.playtox.transfer.service.AccountService;
import com.playtox.transfer.service.impl.AccountServiceImpl;
import com.playtox.transfer.task.TransferTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    private static final int ACCOUNT_COUNT = 4;
    private static final int INITIAL_MONEY = 10000;
    private static final int THREAD_COUNT = 2;
    private static final int MAX_TRANSACTIONS = 30;

    public static void main(String[] args) {
        log.info("Application starting...");

        AccountService accountService = new AccountServiceImpl(ACCOUNT_COUNT, INITIAL_MONEY);
        AtomicInteger transactionCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        log.info("Starting {} transfer threads...", THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(new TransferTask(accountService, transactionCounter, MAX_TRANSACTIONS));
        }

        while (transactionCounter.get() < MAX_TRANSACTIONS) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Maximum number of transactions ({}) reached. Shutting down...", MAX_TRANSACTIONS);
        shutdownExecutor(executor);

        int totalMoney = accountService.getAccounts().stream().mapToInt(Account::getMoney).sum();
        log.info("Final verification: Total money across all accounts: {}. Initial total was: {}",
                totalMoney, ACCOUNT_COUNT * INITIAL_MONEY);

        if (totalMoney != ACCOUNT_COUNT * INITIAL_MONEY) {
            log.error("DATA INCONSISTENCY DETECTED! Total money has changed.");
        } else {
            log.info("Data consistency check passed.");
        }

        log.info("Application finished.");
    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in the specified time. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
}
