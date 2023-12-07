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

The core functionality of Banquo is implemented by the SQL table called **balance_change**.

This table functions like an append-only log, and in principle could be replaced with some other kind of append-log data storage.  We make use of CockroachDB integrity and concurrency features to ensure that this table serves our purposes, architecturally. The full impact of these features is only clear when we consider heavy workloads in a disributed deployment. This design would need careful review before another SQL database was used in place of CockroachDB.

The essence of the design is embodied in this Scala method, which is used to store all account balance changes (excluding the initial balance for an account).
	
```
def insertBalanceChange(acctId: AccountId, prevChgId: BalanceChangeId, chgAmt: ChangeAmount, balAmt: BalanceAmount): ZIO[DbConn, Throwable, BalanceChangeId]
```

Our Scala code accesses the JDBC driver directly, which ensures we have complete control over both DB connections and SQL Transactions.

#### Schema of the balance_change table




### Building and Testing Scala Code

### Container Build and Launch
