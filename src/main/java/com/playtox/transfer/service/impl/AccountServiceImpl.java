package com.playtox.transfer.service.impl;

import com.playtox.transfer.model.Account;
import com.playtox.transfer.service.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AccountServiceImpl implements AccountService {

    private static final Logger log = LogManager.getLogger(AccountServiceImpl.class);
    private static final long LOCK_TIMEOUT_MS = 1000;

    private final List<Account> accounts;

    public AccountServiceImpl(int accountCount, int initialMoney) {
        this.accounts = Stream.generate(() -> new Account(initialMoney))
                .limit(accountCount)
                .collect(Collectors.toList());
        log.info("Initialized {} accounts with {} money each.", accountCount, initialMoney);
    }

    @Override
    public boolean transfer(Account from, Account to, int amount) {
        if (isSameAccount(from, to)) {
            log.warn("Attempt to transfer money to the same account: {}. Transaction cancelled.", from.getId());
            return false;
        }

        Account firstLockAccount = from.getId().compareTo(to.getId()) < 0 ? from : to;
        Account secondLockAccount = from.getId().compareTo(to.getId()) < 0 ? to : from;

        Lock lockFirst = firstLockAccount.getLock();
        Lock lockSecond = secondLockAccount.getLock();

        boolean lockFirstAcquired = false;
        boolean lockSecondAcquired = false;

        try {
            lockFirstAcquired = lockFirst.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (lockFirstAcquired) {
                lockSecondAcquired = lockSecond.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (lockSecondAcquired) {
                    return executeTransferUnderLock(from, to, amount);
                }
            }
            log.warn("FAILED transaction: Could not acquire locks for accounts {} and {}. Timeout exceeded.",
                    from.getId(), to.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Transaction interrupted while acquiring locks.", e);
        } finally {
            if (lockSecondAcquired) {
                lockSecond.unlock();
            }
            if (lockFirstAcquired) {
                lockFirst.unlock();
            }
        }

        return false;
    }

    @Override
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Выполняет основную бизнес-логику перевода под блокировками.
     * @return true, если операция прошла; false, если не хватило средств.
     */
    private boolean executeTransferUnderLock(Account from, Account to, int amount) {
        if (from.getMoney() < amount) {
            log.error("FAILED transaction: Not enough money on account {}. Required: {}, available: {}.",
                    from.getId(), amount, from.getMoney());
            return false;
        }

        from.withdraw(amount);
        to.deposit(amount);

        log.info("SUCCESS transaction: Transferred {} from {} to {}. Balances: from={}, to={}",
                amount, from.getId(), to.getId(), from.getMoney(), to.getMoney());

        return true;
    }

    private boolean isSameAccount(Account from, Account to) {
        return from.getId().equals(to.getId());
    }
}
