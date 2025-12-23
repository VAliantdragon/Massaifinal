package com.shadowledger.shadow_ledger_service.dto;

public class ShadowBalance {
    private String accountId;
    private Double balance;
    private String lastEvent;

    public ShadowBalance() {
    }

    public ShadowBalance(String accountId, Double balance, String lastEvent) {
        this.accountId = accountId;
        this.balance = balance;
        this.lastEvent = lastEvent;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(String lastEvent) {
        this.lastEvent = lastEvent;
    }
}