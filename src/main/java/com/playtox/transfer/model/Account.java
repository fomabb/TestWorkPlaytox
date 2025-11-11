package com.playtox.transfer.model;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private final String id;
    private int money;
    private final Lock lock = new ReentrantLock();


    public Account(int initialMoney) {
        this.id = UUID.randomUUID().toString();
        this.money = initialMoney;
    }

    public String getId() {
        return id;
    }

    public int getMoney() {
        return money;
    }

    public Lock getLock() {
        return lock;
    }

    public void deposit(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deposit amount cannot be negative.");
        }
        this.money += amount;
    }

    public void withdraw(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Withdrawal amount cannot be negative.");
        }
        if (this.money < amount) {
            throw new IllegalStateException("Insufficient funds for withdrawal.");
        }
        this.money -= amount;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", money=" + money +
                '}';
    }
}
