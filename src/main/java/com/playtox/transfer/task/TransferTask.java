package com.playtox.transfer.task;

import com.playtox.transfer.model.Account;
import com.playtox.transfer.service.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferTask implements Runnable {

    private static final Logger log = LogManager.getLogger(TransferTask.class);
    private static final int MAX_TRANSACTION_AMOUNT = 5000;

    private final AccountService accountService;
    private final AtomicInteger transactionCounter;
    private final int maxTransactions;
    private final Random random = new Random();

    public TransferTask(AccountService accountService, AtomicInteger transactionCounter, int maxTransactions) {
        this.accountService = accountService;
        this.transactionCounter = transactionCounter;
        this.maxTransactions = maxTransactions;
    }

    @Override
    public void run() {
        log.info("Thread {} started.", Thread.currentThread().getName());
        while (transactionCounter.get() < maxTransactions) {
            try {
                performRandomTransfer();

                int sleepTime = 1000 + random.nextInt(1001);
                Thread.sleep(sleepTime);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread {} was interrupted.", Thread.currentThread().getName());
                break;
            } catch (Exception e) {
                log.error("An unexpected error occurred in thread {}.", Thread.currentThread().getName(), e);
            }
        }
        log.info("Thread {} finished its work.", Thread.currentThread().getName());
    }

    private void performRandomTransfer() {
        if (transactionCounter.incrementAndGet() > maxTransactions) {
            transactionCounter.decrementAndGet();
            return;
        }

        List<Account> accounts = accountService.getAccounts();
        int accountCount = accounts.size();

        Account from = accounts.get(random.nextInt(accountCount));
        Account to = accounts.get(random.nextInt(accountCount));

        int amount = 1 + random.nextInt(MAX_TRANSACTION_AMOUNT);

        accountService.transfer(from, to, amount);
    }
}
