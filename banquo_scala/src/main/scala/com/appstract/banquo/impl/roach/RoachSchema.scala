package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.roach.DbConn

import zio.ZIO


object RoachSchema {

	val CREATE_TABLE_ACCOUNT =
		"""CREATE TABLE IF NOT EXISTS account (
			| acct_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
			| cust_name STRING NOT NULL,
		 	| cust_address STRING NOT NULL,
   			| acct_create_time TIMESTAMPTZ DEFAULT NOW()) """.stripMargin

	val CREATE_ENUM_BAL_CHG_FLAVOR = "CREATE TYPE IF NOT EXISTS Bal_Chg_Flavor AS ENUM ('INITIAL', 'FLOW')"

	val BCHG_FLAVOR_INITIAL = "INITIAL"
	val BCHG_FLAVOR_FLOW = "FLOW"

	// prev_bchg_id should be NULL when bal_chg_flavor is INITIAL.
	// UNIQUE constraint on prev_bchg_id should prevent forking by simultaneous transactions on the same account.
	// (We expect one of those simultaneous transactions to fail. Such a failure should happen even without this
	// UNIQUE constraint, assuming Cockroach SERIALIZABLE fails on phantom reads).
	// These INT8 values are 64 bits, so we bind them to Java/Scala Long.
	// TODO: Make a test that shows simultaneous transactions failure.
	// TODO: Consider additional indexes to improve read performance, with some tradeoff in storage cost and write performance.
	// TODO: Consider Foreign-key constraint on link to account.
	// TODO: Consider adding an acct_xact_seq column that would increment for each transaction on the account.

	val CREATE_TABLE_BALANCE_CHG =
		"""CREATE TABLE IF NOT EXISTS balance_change (
 			| bchg_id INT8 PRIMARY KEY DEFAULT unique_rowid(),
 			| acct_id UUID NOT NULL,
 			| chg_flavor Bal_Chg_Flavor NOT NULL,
 			| prev_bchg_id INT8 UNIQUE,
 			| chg_amt DECIMAL NOT NULL,
 			| balance DECIMAL NOT NULL,
 			| description STRING,
  			| chg_create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()) """.stripMargin


	val mySqlExec = new SqlEffectMaker

	// These changes will NOT be permanent unless .commit is sent to the sqlConn before it is closed.
	def createTablesAsNeeded: ZIO[DbConn, Throwable, Unit] = {
		val z1 = mySqlExec.execUpdateNoResult(CREATE_TABLE_ACCOUNT).debug("CREATE_TABLE_ACCOUNT")
		val z2 = mySqlExec.execUpdateNoResult(CREATE_ENUM_BAL_CHG_FLAVOR).debug("CREATE_ENUM_BAL_CHG_FLAVOR")
		val z3 = mySqlExec.execUpdateNoResult(CREATE_TABLE_BALANCE_CHG).debug("CREATE_TABLE_BALANCE_CHG")
		(z1 *> z2 *> z3).unit
	}
}
