package com.playtox.transfer.service;

import com.playtox.transfer.model.Account;

import java.util.List;

public interface AccountService {

    boolean transfer(Account from, Account to, int amount);

    List<Account> getAccounts();
}
