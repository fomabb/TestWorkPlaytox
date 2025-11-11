package com.playtox.transfer.service;

import com.playtox.transfer.model.Account;
import com.playtox.transfer.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountServiceTest {

    private static final int ACCOUNT_COUNT = 4;
    private static final int INITIAL_MONEY = 10000;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(ACCOUNT_COUNT, INITIAL_MONEY);
    }

    @Test
    @DisplayName("Успешный перевод: балансы должны корректно измениться")
    void transfer_whenSuccessful_thenBalancesAreCorrect() {
        List<Account> accounts = accountService.getAccounts();
        Account accountFrom = accounts.get(0);
        Account accountTo = accounts.get(1);
        int amount = 500;
        int initialFromMoney = accountFrom.getMoney();
        int initialToMoney = accountTo.getMoney();

        boolean result = accountService.transfer(accountFrom, accountTo, amount);

        assertTrue(result, "Транзакция должна завершиться успешно");
        assertThat(accountFrom.getMoney()).isEqualTo(initialFromMoney - amount);
        assertThat(accountTo.getMoney()).isEqualTo(initialToMoney + amount);
    }

    @Test
    @DisplayName("Ошибка перевода: недостаточно средств -> балансы не должны измениться")
    void transfer_whenInsufficientFunds_thenBalancesDoNotChange() {
        List<Account> accounts = accountService.getAccounts();
        Account accountFrom = accounts.get(0);
        Account accountTo = accounts.get(1);
        int amount = INITIAL_MONEY + 1; // Сумма больше, чем есть на счете
        int initialFromMoney = accountFrom.getMoney();
        int initialToMoney = accountTo.getMoney();

        boolean result = accountService.transfer(accountFrom, accountTo, amount);

        assertThat(result).isFalse();
        assertThat(accountFrom.getMoney()).isEqualTo(initialFromMoney);
        assertThat(accountTo.getMoney()).isEqualTo(initialToMoney);
    }

    @Test
    @DisplayName("Ошибка перевода: перевод на тот же счет -> баланс не должен измениться")
    void transfer_whenToSameAccount_thenBalanceDoesNotChange() {
        List<Account> accounts = accountService.getAccounts();
        Account account = accounts.get(0);
        int amount = 100;
        int initialMoney = account.getMoney();

        boolean result = accountService.transfer(account, account, amount);

        assertThat(result).isFalse();
        assertThat(account.getMoney()).isEqualTo(initialMoney);
    }

    @Test
    @DisplayName("Конкурентные переводы: общая сумма денег должна остаться неизменной")
    void transfer_whenConcurrentTransfers_thenTotalSumIsConsistent() throws InterruptedException {
        List<Account> accounts = accountService.getAccounts();
        int initialTotalMoney = accounts.stream().mapToInt(Account::getMoney).sum();

        int threadCount = 4;
        int transfersPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Random random = new Random();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < transfersPerThread; j++) {
                    Account from = accounts.get(random.nextInt(ACCOUNT_COUNT));
                    Account to = accounts.get(random.nextInt(ACCOUNT_COUNT));
                    int amount = random.nextInt(100);
                    accountService.transfer(from, to, amount);
                }
            });
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue(terminated, "Потоки не завершили работу за выделенное время. Возможно, произошел deadlock.");

        int finalTotalMoney = accounts.stream().mapToInt(Account::getMoney).sum();
        assertThat(finalTotalMoney)
                .withFailMessage("Общая сумма денег изменилась! Начальная: %d, Конечная: %d. Нарушена потокобезопасность.",
                        initialTotalMoney, finalTotalMoney)
                .isEqualTo(initialTotalMoney);
    }
}
