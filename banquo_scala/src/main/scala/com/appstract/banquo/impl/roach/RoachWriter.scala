package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount, BalanceChangeID, ChangeAmount}
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.roach.DbConn
import zio.ZIO


/***
 * Note that these operations do NOT commit their own SQL transactions.
 * Commit/rollback is the responsibility of client code.
 */
trait RoachWriter {
	val mySchema = RoachSchema
	val mySqlExec = new SqlEffectMaker

	val INSERT_ACCT = "INSERT INTO account (cust_name, cust_address) VALUES (?, ?) RETURNING acct_id"

	def insertAccount(customerName: String, customerAddress: String): ZIO[DbConn, Throwable, DbOpResult[AccountID]] = {
		val stmtArgs = Seq[Any](customerName, customerAddress)
		val sqlJob = mySqlExec.execSqlAndPullOneString(INSERT_ACCT, stmtArgs)
		sqlJob.debug(".insertAccount result")
	}

	val INSERT_INIT_BAL =
		"INSERT INTO balance_change (acct_id, chg_flavor, chg_amt, balance) " +
				"VALUES (?, ?, ?, ?) RETURNING bchg_id"

	def insertInitialBalance(acctId: AccountID, initAmt: BalanceAmount): ZIO[DbConn, Throwable, DbOpResult[BalanceChangeID]] = {
		val stmtArgs = Seq[Any](acctId, mySchema.BCHG_FLAVOR_INITIAL, initAmt, initAmt)
		val sqlJob = mySqlExec.execSqlAndPullOneLong(INSERT_INIT_BAL, stmtArgs)
		sqlJob.debug(".insertInitialBalance result")
	}

	val INSERT_BAL_CHG =
		"INSERT INTO balance_change (acct_id, chg_flavor, prev_bchg_id, chg_amt, balance) " +
				"VALUES (?, ?, ?, ?, ?) RETURNING bchg_id"

	def insertBalanceChange(acctId: AccountID, prevChgId: BalanceChangeID, chgAmt: ChangeAmount, balAmt: BalanceAmount):
			ZIO[DbConn, Throwable, DbOpResult[BalanceChangeID]] = {
		val stmtArgs = Seq[Any](acctId, mySchema.BCHG_FLAVOR_FLOW, prevChgId, chgAmt, balAmt)
		val sqlJob = mySqlExec.execSqlAndPullOneLong(INSERT_BAL_CHG, stmtArgs)
		sqlJob.debug(".insertBalanceChange result")
	}
}
