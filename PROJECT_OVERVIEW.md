# Money Transfer System - Project Overview

## 1. Executive Summary

This project is a Spring Boot based money transfer microservice for managing simple bank accounts, account login, balance lookup, transaction history, money transfers, and a reward-points ledger.

The application exposes REST APIs under `/api/v1`, persists data through Spring Data JPA, uses Spring Security with HTTP Basic authentication, and runs against an in-memory H2 database by default. A MySQL profile is also present for local or persistent database use.

At a high level, the service supports:

- Creating bank accounts with encrypted passwords.
- Authenticating users by username and password.
- Reading account and balance details.
- Listing transactions for an account.
- Transferring money between active accounts.
- Preventing duplicate transfer execution through idempotency keys.
- Recording successful and failed transfer attempts as transaction logs.
- Awarding reward points for qualifying cash transfers.
- Redeeming reward points as a discount against future transfers.

## 2. Technology Stack

| Area | Technology |
| --- | --- |
| Runtime | Java 17 |
| Framework | Spring Boot 3.2.2 |
| Web layer | Spring MVC / `spring-boot-starter-web` |
| Persistence | Spring Data JPA / Hibernate |
| Default database | H2 in-memory database |
| Optional database | MySQL |
| Security | Spring Security, BCrypt password hashing, HTTP Basic |
| Validation | Jakarta Bean Validation |
| API documentation | Springdoc OpenAPI / Swagger UI |
| Logging | SLF4J, Logback, Spring AOP logging aspect |
| Testing | JUnit 5, Mockito, Spring Boot Test, Spring Security Test |
| Build tool | Maven |

## 3. Project Structure

```text
src/main/java/com/banking/transfer
+-- MoneyTransferSystemApplication.java
+-- aspect
|   +-- LoggingAspect.java
+-- config
|   +-- JpaAuditingConfig.java
|   +-- SecurityConfig.java
|   +-- WebConfig.java
+-- controller
|   +-- AccountController.java
|   +-- RewardController.java
|   +-- TransferController.java
+-- dto
|   +-- AccountResponse.java
|   +-- CreateAccountRequest.java
|   +-- ErrorResponse.java
|   +-- LoginRequest.java
|   +-- RewardLedgerResponse.java
|   +-- RewardSummaryResponse.java
|   +-- TransactionResponse.java
|   +-- TransferRequest.java
|   +-- TransferResponse.java
+-- entity
|   +-- Account.java
|   +-- AccountStatus.java
|   +-- RewardLedger.java
|   +-- RewardType.java
|   +-- TransactionLog.java
|   +-- TransactionStatus.java
+-- exception
|   +-- AccountNotActiveException.java
|   +-- AccountNotFoundException.java
|   +-- DuplicateTransferException.java
|   +-- DuplicateUsernameException.java
|   +-- GlobalExceptionHandler.java
|   +-- InsufficientBalanceException.java
|   +-- InvalidCredentialsException.java
+-- repository
|   +-- AccountRepository.java
|   +-- RewardLedgerRepository.java
|   +-- TransactionLogRepository.java
+-- security
|   +-- CustomUserDetailsService.java
+-- service
    +-- AccountService.java
    +-- RewardService.java
    +-- TransferService.java
```

The code follows a conventional Spring layered architecture:

- Controllers receive HTTP requests and return HTTP responses.
- DTOs define request and response payloads.
- Services contain business logic and transaction boundaries.
- Repositories provide database access.
- Entities define the persistence model.
- Exceptions and `GlobalExceptionHandler` normalize API error responses.
- Configuration classes define security, CORS, and JPA auditing behavior.

## 4. Domain Model

### 4.1 Account

`Account` represents a bank account and is stored in the `accounts` table.

Key fields:

| Field | Purpose |
| --- | --- |
| `id` | Database-generated primary key. |
| `username` | Unique login identifier. |
| `password` | BCrypt encoded password. |
| `holderName` | Account holder's display name. |
| `balance` | Monetary balance, precision `18,2`. |
| `status` | `ACTIVE`, `LOCKED`, or `CLOSED`. |
| `version` | JPA optimistic-locking field. |
| `lastUpdated` | Audited last-modified timestamp. |

Behavior:

- `debit(amount)` subtracts money from the balance and rejects overdrafts.
- `credit(amount)` adds money to the balance.
- `isActive()` returns true only for `ACTIVE` accounts.

### 4.2 TransactionLog

`TransactionLog` records a transfer attempt and is stored in the `transaction_logs` table.

Key fields:

| Field | Purpose |
| --- | --- |
| `id` | UUID string primary key generated before persistence. |
| `fromAccountId` | Source account ID. |
| `toAccountId` | Destination account ID. |
| `amount` | Requested transfer amount. |
| `status` | `SUCCESS` or `FAILED`. |
| `failureReason` | Failure reason, or reward redemption metadata for successful reward transfers. |
| `idempotencyKey` | Unique key used to reject duplicate transfer requests. |
| `createdOn` | Timestamp generated before persistence. |

### 4.3 RewardLedger

`RewardLedger` stores reward point changes and is persisted in the `reward_ledger` table.

Key fields:

| Field | Purpose |
| --- | --- |
| `id` | Database-generated primary key. |
| `accountId` | Account whose points changed. |
| `transactionId` | Related transaction log ID. |
| `rewardType` | `EARN` or `REDEEM`. |
| `pointsEarned` | Positive for earned points, negative for redeemed points. |
| `createdOn` | Timestamp generated before persistence. |

The table has a unique constraint on `(transaction_id, reward_type)`, allowing a transaction to have at most one earn entry and one redeem entry.

## 5. REST API Surface

### 5.1 Account APIs

Base path: `/api/v1/accounts`

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/accounts` | Public | Create a new account. |
| `POST` | `/api/v1/accounts/login` | Public | Validate credentials and return account details. |
| `GET` | `/api/v1/accounts/{id}` | Authenticated | Return account details. |
| `GET` | `/api/v1/accounts/{id}/balance` | Authenticated | Return the same account response, including balance. |
| `GET` | `/api/v1/accounts/{id}/transactions` | Authenticated | Return debit and credit transaction history. |

Create account request:

```json
{
  "username": "alice",
  "password": "password123",
  "holderName": "Alice Johnson",
  "initialBalance": 1000.00
}
```

Account response:

```json
{
  "id": 1,
  "accountNumber": "ACC-000001",
  "username": "alice",
  "holderName": "Alice Johnson",
  "balance": 1000.00,
  "status": "ACTIVE"
}
```

Validation rules:

- `username`, `password`, and `holderName` are required.
- `initialBalance` is required and must be non-negative.
- `initialBalance` allows up to 10 integer digits and 2 fractional digits.
- Usernames must be unique.

### 5.2 Transfer API

Base path: `/api/v1/transfers`

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/transfers` | Authenticated | Transfer money from one account to another. |

Transfer request:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 500.00,
  "idempotencyKey": "txn-001",
  "redeemRewards": false
}
```

Transfer response:

```json
{
  "transactionId": "generated-uuid",
  "status": "SUCCESS",
  "message": "Transfer completed successfully",
  "debitedFrom": 1,
  "creditedTo": 2,
  "amount": 500.00
}
```

Validation and business rules:

- Source account and destination account are required.
- Amount is required and must be greater than zero.
- Idempotency key is required and must be unique.
- Source and destination accounts cannot be the same.
- Both accounts must exist.
- Both accounts must be `ACTIVE`.
- Source account must have enough cash balance for the net cash required.

### 5.3 Reward API

Base path: `/api/v1/rewards`

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/rewards/{accountId}` | Authenticated | Return total reward points and reward history. |

Reward summary response:

```json
{
  "accountId": 1,
  "totalPoints": 9,
  "history": [
    {
      "id": 10,
      "transactionId": "TX123",
      "rewardType": "EARN",
      "pointsEarned": 9,
      "createdOn": "2026-06-21T15:00:00"
    }
  ]
}
```

## 6. Core Business Flows

### 6.1 Account Creation Flow

1. Client calls `POST /api/v1/accounts`.
2. Request validation checks required fields and initial balance format.
3. `AccountService` checks whether the username already exists.
4. Password is encoded with BCrypt.
5. A new account is created with:
   - provided username,
   - encoded password,
   - provided holder name,
   - provided initial balance,
   - status `ACTIVE`.
6. Account is saved through `AccountRepository`.
7. API returns HTTP `201 Created` with `AccountResponse`.

### 6.2 Login Flow

1. Client calls `POST /api/v1/accounts/login`.
2. `AccountService` looks up the account by username.
3. BCrypt verifies the supplied password against the stored password hash.
4. Invalid username or invalid password both return the same invalid-credentials error.
5. Successful login returns account details.

This login endpoint does not issue a token. It simply validates credentials and returns account information. Protected APIs use HTTP Basic authentication through Spring Security.

### 6.3 Standard Transfer Flow

1. Client calls `POST /api/v1/transfers`.
2. `TransferService` validates:
   - source and destination are different,
   - amount is positive,
   - idempotency key has not already been used.
3. Source and destination accounts are loaded.
4. Both accounts must be active.
5. Source account balance must cover the full transfer amount.
6. Source account is debited.
7. A successful `TransactionLog` is saved.
8. Destination account is credited.
9. `RewardService` processes reward earning for the sender.
10. API returns a success response with the transaction ID.

### 6.4 Transfer With Reward Redemption

When `redeemRewards` is `true`, the transfer service tries to use the sender's available points to reduce the cash that must be debited from the source account.

Current redemption rule:

- `1 reward point = 1 unit of cash discount`.
- Points can cover part or all of the requested transfer amount.
- The destination account still receives the full requested transfer amount.
- The source account is debited only for the remaining net cash amount.

Example:

| Item | Value |
| --- | --- |
| Transfer amount | `1000.00` |
| Available points | `50` |
| Reward discount | `50.00` |
| Net cash debited | `950.00` |
| Destination credited | `1000.00` |

After the successful transaction log is saved:

1. A `REDEEM` reward ledger entry is created with negative points.
2. Reward earning is calculated on the net cash amount only.

### 6.5 Reward Earning Flow

`RewardService.processReward(...)` awards points only when:

- the transaction status is `SUCCESS`,
- source and destination accounts are different,
- the net cash spent is greater than `100`,
- calculated points are greater than zero,
- no `EARN` ledger entry already exists for the transaction.

Current earning rule:

- Points are calculated as `floor(netCashSpent / 100)`.
- `100.00` does not qualify because the code checks for amounts greater than `100`, not greater than or equal to `100`.
- `950.00` earns `9` points.
- `1000.00` earns `10` points.

### 6.6 Transaction History Flow

1. Client calls `GET /api/v1/accounts/{id}/transactions`.
2. `AccountService` first verifies that the account exists.
3. `TransactionLogRepository.findByAccountId(...)` finds transactions where the account is either sender or receiver.
4. Results are ordered newest first.
5. Each transaction is mapped to a response with:
   - `type = "DEBIT"` when the account is the sender,
   - `type = "CREDIT"` when the account is the receiver.

## 7. Persistence and Repository Layer

### 7.1 AccountRepository

Extends `JpaRepository<Account, Long>`.

Custom methods:

- `Optional<Account> findByUsername(String username)`
- `boolean existsByUsername(String username)`

### 7.2 TransactionLogRepository

Extends `JpaRepository<TransactionLog, String>`.

Custom methods:

- `Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey)`
- `List<TransactionLog> findByAccountId(Long accountId)`

`findByAccountId` uses a JPQL query to fetch transactions where the account is either the source or destination account.

### 7.3 RewardLedgerRepository

Extends `JpaRepository<RewardLedger, Long>`.

Custom methods:

- `boolean existsByTransactionIdAndRewardType(String transactionId, RewardType rewardType)`
- `List<RewardLedger> findByAccountIdOrderByCreatedOnDesc(Long accountId)`
- `int sumPointsByAccountId(Long accountId)`

The reward summary uses `sumPointsByAccountId` to calculate the current point balance.

## 8. Security Model

Security is configured in `SecurityConfig`.

Public endpoints:

- `POST /api/v1/accounts`
- `POST /api/v1/accounts/login`
- `/h2-console/**`
- `/actuator/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

All other endpoints require authentication.

Authentication details:

- Passwords are stored using `BCryptPasswordEncoder`.
- `CustomUserDetailsService` loads users from the `accounts` table.
- Accounts that are not `ACTIVE` are treated as locked for Spring Security authentication.
- HTTP Basic authentication is enabled.
- CSRF is disabled.
- H2 console frames are allowed from the same origin.

Authorization note:

The current implementation authenticates requests but does not enforce account-level authorization. An authenticated user can request another account's details or initiate transfers for account IDs supplied in the request body. If this service were hardened for production, account ownership checks would be an important next step.

## 9. CORS and Frontend Integration

Both controller annotations and `WebConfig` allow requests from:

```text
http://localhost:4200
```

This suggests the service is intended to be consumed by a local Angular frontend or another frontend development server running on port `4200`.

Allowed methods for `/api/**`:

- `GET`
- `POST`
- `PUT`
- `DELETE`
- `OPTIONS`

All headers are allowed, and credentials are allowed.

## 10. Configuration

### 10.1 Default Profile

`src/main/resources/application.yml` configures:

- application name: `money-transfer-system`
- server port: `8080`
- H2 in-memory database: `jdbc:h2:mem:transferdb`
- H2 console: `/h2-console`
- Hibernate DDL mode: `create-drop`
- SQL logging enabled
- default Spring security user: `admin/admin`
- log file: `logs/money-transfer-system.log`

Because the default database is in-memory and uses `create-drop`, data is lost when the application stops.

### 10.2 MySQL Profile

`src/main/resources/application-mysql.yml` configures a MySQL datasource:

```text
jdbc:mysql://localhost:3306/transferdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
```

It also configures HikariCP pool settings and Hibernate `ddl-auto: update`.

Typical startup command for MySQL profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

The MySQL password is currently a placeholder:

```text
your_mysql_password
```

## 11. Logging and Observability

Logging is configured through `logback-spring.xml`.

Outputs:

- Console logs.
- Rolling file logs under `logs/`.

Log rotation:

- Daily file pattern: `money-transfer-system-%d{yyyy-MM-dd}.log`
- Maximum history: 30 days
- Total size cap: 1 GB

`LoggingAspect` wraps all service-layer method calls:

- logs service method entry,
- measures execution time,
- logs method exit,
- logs exceptions with execution time.

Spring Boot Actuator is included and `/actuator/**` is public.

## 12. Error Handling

`GlobalExceptionHandler` maps exceptions into a consistent `ErrorResponse`:

```json
{
  "errorCode": "TRX-400",
  "message": "Insufficient balance",
  "timestamp": 1718950000000
}
```

Main mappings:

| Exception | HTTP Status | Error Code |
| --- | --- | --- |
| `AccountNotFoundException` | `404 Not Found` | `ACC-404` |
| `AccountNotActiveException` | `403 Forbidden` | `ACC-403` |
| `InsufficientBalanceException` | `400 Bad Request` | `TRX-400` |
| `DuplicateTransferException` | `409 Conflict` | `TRX-409` |
| `DuplicateUsernameException` | `409 Conflict` | `ACC-409` |
| `InvalidCredentialsException` | `401 Unauthorized` | `AUTH-401` |
| `IllegalArgumentException` | `422 Unprocessable Entity` | `VAL-422` |
| `MethodArgumentNotValidException` | `422 Unprocessable Entity` | `VAL-422` |
| `HttpMessageNotReadableException` | `400 Bad Request` | `VAL-400` |
| Generic `Exception` | `500 Internal Server Error` | `SYS-500` |

## 13. API Documentation

The project includes Springdoc OpenAPI.

When the application is running, API documentation should be available at:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

## 14. Build and Run

### Build

```bash
mvn clean package
```

### Run With Default H2 Database

```bash
mvn spring-boot:run
```

The service starts on:

```text
http://localhost:8080
```

H2 console:

```text
http://localhost:8080/h2-console
```

Default H2 connection details:

| Setting | Value |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:transferdb` |
| Username | `sa` |
| Password | empty |

### Run With MySQL

1. Start MySQL locally.
2. Ensure the configured username and password are correct in `application-mysql.yml`.
3. Run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## 15. Testing

The test suite is organized around service and controller behavior.

### AccountServiceTest

Covers:

- successful account creation,
- duplicate username rejection,
- successful login,
- invalid username login,
- invalid password login,
- account lookup,
- account-not-found handling,
- transaction history mapping for debit and credit entries.

### TransferServiceTest

Covers intended transfer behavior including:

- successful transfer,
- reward redemption behavior,
- insufficient balance,
- duplicate idempotency key,
- same-account transfer rejection,
- source account not found,
- destination account not found,
- inactive source or destination account,
- negative and zero amount validation,
- attempted failed-transaction logging.

### RewardServiceTest

Covers:

- redeem ledger entry creation,
- earn ledger entry creation,
- duplicate earn prevention,
- duplicate redeem prevention.

### AccountControllerTest

Covers:

- account creation endpoint,
- login endpoint,
- account lookup endpoint,
- request validation failures.

### Test Suite Notes

There is a file named:

```text
src/test/java/com/banking/transfer/service/CustomUserDetailServiceTest
```

It contains a `CustomUserDetailsServiceTest` class but has no `.java` extension. Maven normally compiles Java test files with `.java` extensions, so this file may not be included in standard test compilation unless it is renamed.

## 16. Current Implementation Notes and Risks

These are not necessarily defects for a demo application, but they are important for maintainers to understand.

### 16.1 Failed Transaction Logging and Rollback

`TransferService.transfer(...)` catches exceptions, saves a failed `TransactionLog`, and rethrows the exception.

Because the method is annotated with `@Transactional`, rethrowing a runtime exception usually marks the whole transaction for rollback. That means the failed log may not actually persist in a real database unless transaction handling is adjusted.

### 16.2 Idempotency Behavior

The transfer API rejects any request whose `idempotencyKey` already exists.

This prevents duplicate execution, but it does not return the original successful response for repeated requests. Instead, duplicate requests receive a `409 Conflict`.

### 16.3 Reward Metadata Stored in `failureReason`

For successful transfers with redeemed points, the service stores text such as `REDEEMED_POINTS: 50` in `TransactionLog.failureReason`.

That field name is misleading for successful transactions. A dedicated metadata field or normalized redemption relation would be clearer.

### 16.4 Account-Level Authorization

The service authenticates users but does not verify that the authenticated username owns the account ID being read or debited.

For production-style behavior, protected endpoints should compare the authenticated principal with the requested account or apply a role/permission model.

### 16.5 Optimistic Locking

`Account` has a `@Version` field, so concurrent updates can be detected by JPA optimistic locking.

The transfer flow does not explicitly lock rows. High-concurrency transfer scenarios should be tested to confirm the service handles optimistic-lock failures cleanly.

### 16.6 H2 Default Data Lifecycle

The default profile uses H2 in-memory storage and `ddl-auto: create-drop`.

This is convenient for development, but all accounts, transactions, and rewards disappear when the application stops.

### 16.7 Public Operational Endpoints

The current security configuration permits unauthenticated access to `/actuator/**` and `/h2-console/**`.

That is convenient locally, but these endpoints should be restricted outside development.

## 17. Typical End-to-End Usage

1. Start the application with the default H2 profile.
2. Create account A:

```http
POST /api/v1/accounts
```

3. Create account B:

```http
POST /api/v1/accounts
```

4. Call login or use HTTP Basic credentials for authenticated endpoints.
5. Transfer money from account A to account B:

```http
POST /api/v1/transfers
```

6. Check account A balance:

```http
GET /api/v1/accounts/{id}/balance
```

7. Check transaction history:

```http
GET /api/v1/accounts/{id}/transactions
```

8. Check rewards:

```http
GET /api/v1/rewards/{accountId}
```

9. Make a later transfer with:

```json
{
  "redeemRewards": true
}
```

to apply available reward points as a cash discount.

## 18. Summary

This project implements a small but complete banking transfer backend. The main business value is concentrated in `TransferService`, where the service validates transfers, enforces idempotency, updates balances, records transaction logs, and coordinates reward earning and redemption.

The application is suitable as a learning, prototype, or interview-style microservice. Before production use, the biggest hardening areas would be account-level authorization, transaction rollback semantics for failed logs, operational endpoint security, concurrency testing, and persistence configuration.
