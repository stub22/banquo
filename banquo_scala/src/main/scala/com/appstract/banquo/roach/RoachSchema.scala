package com.appstract.banquo.roach

import com.appstract.banquo.model.{AccountDetails, BalanceChange}
import com.appstract.banquo.model.BankScalarTypes.AccountId


object RoachSchema {
	val TABLE_ACCOUNT = "account"
	val COL_ACCT_ID = "acct_id"
	val CREATE_TABLE_ACCOUNT =
		"""CREATE TABLE IF NOT EXISTS account (
		 acct_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
		 cust_name STRING,
		 cust_address STRING)""".stripMargin

	val CREATE_ENUM_BAL_CHG_FLAVOR = "CREATE TYPE IF NOT EXISTS bal_chg_flavor AS ENUM ('INITIAL', 'FLOW')"

	val BCHG_FLAVOR_INITIAL = "INITIAL"
	val BCHG_FLAVOR_FLOW = "FLOW"

	val COL_BCHG_ID = "bchg_id"
	// prev_bchg_id should be NULL when bal_chg_flavor is INITIAL.
	// UNIQUE constraint on prev_bchg_id should prevent forking by simultaneous transactions on the same account.
	// (We expect one of the transactions to fail.
	// This failure should actually happen even without the UNIQUE constraint, assuming Cockroach SERIALIZABLE fails on
	// phantom reads).
	// These INT8 values are 64 bits, so we bind them to Java/Scala Long.
	val COL_PREV_BCHG_ID = "prev_bchg_id"
	val CREATE_TABLE_BALANCE_CHG =
		"""CREATE TABLE IF NOT EXISTS balance_change (
	 			bchg_id INT8 PRIMARY KEY DEFAULT unique_rowid(),
	 			acct_id UUID,
				chg_flavor bal_chg_flavor,
				prev_bchg_id INT8 UNIQUE,
				chg_amt DECIMAL,
				balance DECIMAL) """.stripMargin


	val mySqlExec = new SqlExecutor

	def createTablesAsNeeded = {
		val z1 = mySqlExec.execUpdateNoResult(CREATE_TABLE_ACCOUNT).debug("CREATE_TABLE_ACCOUNT")
		val z2 = mySqlExec.execUpdateNoResult(CREATE_ENUM_BAL_CHG_FLAVOR).debug("CREATE_ENUM_BAL_CHG_FLAVOR")
		val z3 = mySqlExec.execUpdateNoResult(CREATE_TABLE_BALANCE_CHG).debug("CREATE_TABLE_BALANCE_CHG")
		(z1 *> z2 *> z3).unit
	}
}


trait RoachReader {
	def selectLastBalanceChange(acctId : AccountId) : Either[DbError,BalanceChange] = ???

	// TODO: AllBalanceChanges should be some kind of stream or paged result.
	def selectAllBalanceChanges(acctId : AccountId) : Either[DbError, Iterable[BalanceChange]] = ???

	def selectAccountDetails(acctId : AccountId) : Either[DbError, AccountDetails] = ???
}



trait BankAccountReadOps {
	val myRoachReader = new RoachReader {}
}