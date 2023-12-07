## Banquo

Banquo is a prototype implementation of a bank account ledger service with the potential to be highly available, horizontally scalable, and strongly consistent. 

The design relies on the scalable strong consistency features of Cockroach DB, which is protocol compatible with Postgresql.  

The Banquo HTTP service is implemented in Functional Scala (v. 2.13) using the following libraries:
  * ZIO 2.x
  * ZIO-HTTP
  * Postgres JDBC driver

Banquo is designed to be run as a set of parallel stateless service instances, deployed and managed by  Kubernetes.  As of 2023-12-06, Banquo has been tested using only a single service instance launched in a Docker Compose setup, using a single-server instance of the free edition of Cockroach DB version ____.

Cockroach DB may be deployed as a scalable, distributed database under Kubernetes (commercial license required), and is also offered as a commercial cloud-hosted service.

### Persistent Store Design Overview

The core functionality of Banquo is implemented by the SQL table called `balance_change`.

This table functions like an append-only log, and in principle could be replaced with some other kind of append-log data storage.  We make use of CockroachDB integrity and concurrency features to ensure that this table serves our purposes, architecturally. The full impact of these features is only clear when we consider heavy workloads in a disributed deployment. This design would need careful review before another SQL database was used in place of CockroachDB.

  * The current Banquo implementation never mutates any rows of the `balance_change` table.
  * Our Scala code accesses the JDBC driver directly, which ensures we have complete control over both DB connections and SQL Transactions.



#### Schema and concurrency design of the `balance_change` table

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
def insertBalanceChange(acctId: AccountId, prevChgId: BalanceChangeId, chgAmt: ChangeAmount, balAmt: BalanceAmount): ZIO[DbConn, Throwable, BalanceChangeId]
```

Note the linkage of each BalanceChange to a previous (immutable!) BalanceChange record.
Also note that the UNIQUE constraint on prev_bchg_id ensures that each BalanceChange record may 
only be used **once** as a previous balance.  This constraint ensures that we will grow an unforked 
chain of balance-change operations for each account. 

This constraint comes with its own storage and write-performance costs (the cost of checking and storing  the UNIQUE index on prev_bchg_id).  Given the serializable transaction model of Cockroach DB, this
index-based approach may not be strictly necessary.  (We could possibly prevent account balances from 
forking using only phantom-read isolation between competing transactions).  However it is an explicit 
and understandable mechanism that delivers the functionality we need for the Banquo prototype.

### Building and Testing Scala Code

### Container Build and Launch
