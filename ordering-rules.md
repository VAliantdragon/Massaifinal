# Event Ordering Rules

## 1. Introduction

In this distributed financial transaction processing system, maintaining the correct order of events is critical for data integrity. The `shadow-ledger-service` constructs account balances by processing a stream of raw transactions and data corrections. Incorrect event ordering can lead to invalid states, such as negative balances from withdrawals being processed before deposits, and will compromise the accuracy of the ledger.

This document outlines the rules and strategies employed to guarantee sequential processing of events related to the same financial entity (e.g., a customer account).

## 2. Core Principle: Kafka Partitioning

The system leverages Apache Kafka's ordering guarantees to enforce sequential processing. Kafka guarantees that all messages sent to a single topic partition will be written in the order they are produced and read by a consumer in that same order.

### The Partitioning Key

To ensure all transactions for a specific account are processed sequentially, we use the **`accountId`** as the Kafka message key.

**Rule:** All producers publishing to the `transactions.raw` and `transactions.corrections` topics **MUST** use the unique `accountId` of the affected account as the message key.

By using the `accountId` as the partition key, Kafka's hashing mechanism ensures that all messages for the same account are consistently routed to the same partition. This forces the `shadow-ledger-service` consumer to process all events for a given account in the exact order they were published.

## 3. Event Timestamps

While Kafka guarantees processing order, the event data itself must contain a definitive timestamp for auditing and reconciliation.

**Rule:** Every event message **MUST** contain a `timestamp` field. This timestamp should be in UTC and represent the time the transaction or correction occurred at its source.

This application-level timestamp is the ultimate source of truth for the chronological sequence of business events, independent of system processing latencies.

## 4. Consumer Idempotency

Failures are inevitable in a distributed system. A consumer might fail after processing a message but before committing the offset, leading to reprocessing upon recovery.

**Rule:** The `shadow-ledger-service` **MUST** be idempotent. It must be designed to handle duplicate messages without corrupting the state of the ledger.

This is typically achieved by tracking the IDs or sequence numbers of processed transactions for each account. If a message arrives that has already been processed, it is safely ignored. This ensures that at-least-once delivery semantics do not lead to incorrect calculations.

## 5. Summary of Rules

1.  **Use `accountId` as Key:** All Kafka messages on `transactions.raw` and `transactions.corrections` topics must use the `accountId` as the message key.
2.  **Include UTC Timestamp:** Every event must include an application-level UTC timestamp indicating when the event occurred.
3.  **Ensure Consumer Idempotency:** The `shadow-ledger-service` must be able to process the same message multiple times without side effects.
