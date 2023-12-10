## Banquo

Banquo is a prototype implementation of a bank account ledger service with the potential to be highly available, horizontally scalable, and strongly consistent. 

The design relies on the scalable strong consistency features of Cockroach DB, which is protocol compatible with Postgresql.  

The Banquo HTTP service is implemented in Functional Scala (v. 2.13) using the following main libraries:
  * ZIO 2.x
  * ZIO-HTTP
  * Postgres JDBC driver

Banquo is designed to be run as a set of parallel stateless service instances, deployed and managed by  Kubernetes.  As of 2023-12-06, Banquo has been tested using only a single service instance, using a single-server instance of the free desktop edition of CockroachDB core version 23.1.12.

CockroachDB may be deployed as a scalable, distributed database under Kubernetes (commercial license required), and is also offered as a commercial cloud-hosted service.

### Persistent Store Design 

The core functionality of Banquo is implemented by the SQL table called `balance_change`.

This table functions like an append-only log, and in principle could be replaced with some other kind of append-log data storage.  We make use of CockroachDB integrity and concurrency features to ensure that this table serves our purposes, architecturally. The full impact of these features is only clear when we consider heavy workloads in a disributed deployment. This design would need careful review before another SQL database was used in place of CockroachDB.

  * The current Banquo implementation never mutates any rows of the `balance_change` table.
  * Our Scala code accesses the JDBC driver directly, which ensures we have complete control over both DB connections and SQL Transactions.

#### Schema and Concurrency Design of the `balance_change` table

The column schema is as follows.  
Notice that bchg_id is automatically generated as a mostly-increasing integer.

```
	column_name   |            data_type            | is_nullable | column_default | 
------------------+---------------------------------+-------------+----------------+
  bchg_id         | INT8                            |      f      | unique_rowid() | 
  acct_id         | UUID                            |      f      | NULL           | 
  chg_flavor      | defaultdb.public.bal_chg_flavor |      f      | NULL           |
  prev_bchg_id    | INT8                            |      t      | NULL           |
  chg_amt         | DECIMAL                         |      f      | NULL           |
  balance         | DECIMAL                         |      f      | NULL           |
  description     | STRING                          |      t      | NULL           |
  chg_create_time | TIMESTAMPTZ                     |      f      | now()          |
```

The table has the following constraints 
```
    table_name   | constraint_type |          details          | validated
-----------------+---------------------------------+-----------------+--------
  balance_change | PRIMARY KEY     | PRIMARY KEY (bchg_id ASC) |     t
  balance_change | UNIQUE          | UNIQUE (prev_bchg_id ASC) |     t
```

With the above schema in mind, the essence of the Banquo design is illustrated by this Scala method signature, which is used to store all account balance changes (excluding the initial balance for an account).
	
```
def insertBalanceChange(acctID: AccountID, prevChgID: BalanceChangeID, chgAmt: ChangeAmount, 
                        balAmt: BalanceAmount, xactDesc : XactDescription): 
                        URIO[DbConn, DbOpResult[BalanceChangeSummary]] 
```

Note the linkage by `prevChgID` of each `BalanceChange` to a previous (immutable!) BalanceChange record.
Also note that the `UNIQUE` constraint on `prev_bchg_id` ensures that each BalanceChange record may 
only be used **once** as a previous balance.  This constraint ensures that we will grow an unforked 
chain of BalanceChange records for each account. 

This constraint comes with its own storage and write-performance costs (the cost of checking and storing  the UNIQUE index on prev_bchg_id).  Given the serializable transaction model of Cockroach DB, this
index-based approach may not be strictly necessary.  (We could possibly prevent account balances from 
forking using only phantom-read isolation between competing transactions).  However it is an explicit 
and understandable mechanism that delivers the functionality we need for the Banquo prototype.

### Building and Testing Scala Code
Build scala code with

```sbt clean compile```

To launch the Banquo HTTP service on port 8484 (hardcoded)

```sbt run```

Tested with:  OpenJDK 11.0.19 (GraalVM) on Microsoft Windows 10

### Database Connection Config

When present, these environment variables configure the Banquo JDBC connection

`ROACH_HOST`  defaults to localhost

`ROACH_PORT`  defaults to 26257

### Container Build and Launch

Docker compose is configured to assemple and launch two containers:  One for Banquo, and one for an in-memory Cockroach DB instance. 

Best practice is to build + pull the images first, using:

  * `docker compose pull`
    * This will pull the Cockroach DB image.  You can ignore any errors about the Banquo image.

  * `docker compose build`
    * This will build the Banquo Scala image.

Then you can start the combined container setup using:

`docker compose up`  

To stop the containers, from another window use the command:

`docker compose down`

While the containers are running, three services will be exposed to the host machine and its network:

 1. Banquo service at http://localhost:8499/
 2. Cockroach web console at http://localhost:8199/
 3. Cockroach DB SQL access on localhost port `26299`
     * Access using Cockroach SQL client
       * https://www.cockroachlabs.com/docs/stable/cockroach-sql 
     * On Microsoft Windows, connect to the containerized service with
       * `cockroach.exe sql --insecure --port=26299`

### Creating Test Accounts

Dummy accounts may be created, one at a time, using HTTP POST to the running service at path `/make-dummy-account`.

The intial balance of the account will be a random amount.  The name and address will be timestamped Strings.
```
curl.exe -s -X POST http://localhost:8499/make-dummy-account
{"accountID":"a464e29f-d98f-482d-9d3b-08df36cc3653","customerName":"dummy_1702212638165","customerAddress":"dummy_1702212638165","balanceAmt":244.11}

curl.exe -s -X POST http://localhost:8499/make-dummy-account
{"accountID":"02733529-aaff-422f-a5c8-d22126191f17","customerName":"dummy_1702212641608","customerAddress":"dummy_1702212641608","balanceAmt":522.50}
```

### Creating Transactions
```
 curl.exe -d '{"account_id" : "02733529-aaff-422f-a5c8-d22126191f17", "amount" : "4522.77", "description": "Net Pay Deposit" }' -X POST http://localhost:8499/transaction

{"acctID":"02733529-aaff-422f-a5c8-d22126191f17","changeAmt":4522.77,"balanceAmt":5045.27 createTimestampTxt":"2023-12-10 13:14:32.894762","xactDescription_opt":"Net Pay Deposit"}
```

### Fetching Account Summary
```
 curl.exe -s http://localhost:8499/account/02733529-aaff-422f-a5c8-d22126191f17

{"accountID":"02733529-aaff-422f-a5c8-d22126191f17","customerName":"dummy_1702212641608" "customerAddress":"dummy_1702212641608","balanceAmt":5045.27
```

### Fetching Account Transaction History
Access this same URL in a web browser to get a more readably formatted result.
```
curl.exe -s http://localhost:8499/transaction/history/02733529-aaff-422f-a5c8-d22126191f17

[{"acctID":"02733529-aaff-422f-a5c8-d22126191f17","changeAmt":4522.77,"balanceAmt":5045.27,"createTimestampTxt":"2023-12-10 13:14:32.894762","xactDescription_opt":"Net Pay Deposit"},{"acctID":"02733529-aaff-422f-a5c8-d22126191f17","changeAmt":522.50,"balanceAmt":522.50,"createTimestampTxt":"2023-12-10 12:50:41.609989","xactDescription_opt":"INITIAL ACCOUNT BALANCE"}]
```
