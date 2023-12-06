package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount, BalanceChangeId, ChangeAmount}
import com.appstract.banquo.api.DbConn
import zio.ZIO


trait RoachWriter {
	val mySchema = RoachSchema
	val mySqlExec = new SqlExecutor

	val INSERT_ACCT = "INSERT INTO account (cust_name, cust_address) VALUES (?, ?) RETURNING acct_id"

	def insertAccount(customerName: String, customerAddress: String): ZIO[DbConn, Throwable, AccountId] = {
		val stmtArgs = Seq[Any](customerName, customerAddress)
		val z1 = mySqlExec.execSqlAndPullOneString(INSERT_ACCT, stmtArgs)
		z1.debug(".insertAccount result")
	}

	val INSERT_INIT_BAL =
		"INSERT INTO balance_change (acct_id, chg_flavor, chg_amt, balance) " +
				"VALUES (?, ?, ?, ?) RETURNING bchg_id"

	def insertInitialBalance(acctId: AccountId, initAmt: BalanceAmount): ZIO[DbConn, Throwable, BalanceChangeId] = {
		val stmtArgs = Seq[Any](acctId, mySchema.BCHG_FLAVOR_INITIAL, initAmt, initAmt)
		val z1 = mySqlExec.execSqlAndPullOneLong(INSERT_INIT_BAL, stmtArgs)
		z1.debug(".insertInitialBalance result")
	}

	val INSERT_BAL_CHG =
		"INSERT INTO balance_change (acct_id, chg_flavor, prev_bchg_id, chg_amt, balance) " +
				"VALUES (?, ?, ?, ?, ?) RETURNING bchg_id"

	def insertBalanceChange(acctId: AccountId, prevChgId: BalanceChangeId, chgAmt: ChangeAmount, balAmt: BalanceAmount):
	ZIO[DbConn, Throwable, BalanceChangeId] = {
		val stmtArgs = Seq[Any](acctId, mySchema.BCHG_FLAVOR_FLOW, prevChgId, chgAmt, balAmt)
		val z1 = mySqlExec.execSqlAndPullOneLong(INSERT_BAL_CHG, stmtArgs)
		z1.debug(".insertBalanceChange result")
	}
}
