package com.appstract.banquo.impl.roach

import zio.{URIO, ZIO}
import java.sql.{ResultSet => JdbcResultSet}
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount, BalanceChangeID, ChangeAmount, DbTimestamp}
import com.appstract.banquo.api.bank.BalanceChangeSummary
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.roach.DbConn


/***
 * Note that these operations do NOT commit their own SQL transactions.
 * Commit/rollback is the responsibility of client code.
 */
trait RoachWriter {
	val mySchema = RoachSchema
	val mySqlExec = new SqlEffectMaker

	val INSERT_ACCT = "INSERT INTO account (cust_name, cust_address) VALUES (?, ?) RETURNING acct_id"

	def insertAccount(customerName: String, customerAddress: String): ZIO[DbConn, Nothing, DbOpResult[AccountID]] = {
		val stmtParams = Seq[Any](customerName, customerAddress)
		val sqlJob = mySqlExec.execSqlAndPullOneString(INSERT_ACCT, stmtParams)
		sqlJob.debug(".insertAccount result")
	}

	val INSERT_INIT_BAL =
		"INSERT INTO balance_change (acct_id, chg_flavor, chg_amt, balance) " +
				"VALUES (?, ?, ?, ?) RETURNING bchg_id"

	def insertInitialBalance(acctID: AccountID, initAmt: BalanceAmount): URIO[DbConn, DbOpResult[BalanceChangeID]] = {
		val stmtParams = Seq[Any](acctID, mySchema.BCHG_FLAVOR_INITIAL, initAmt, initAmt)
		val sqlJob = mySqlExec.execSqlAndPullOneLong(INSERT_INIT_BAL, stmtParams)
		sqlJob.debug(".insertInitialBalance result")
	}

	val INSERT_BAL_CHG =
		"INSERT INTO balance_change (acct_id, chg_flavor, prev_bchg_id, chg_amt, balance) " +
				"VALUES (?, ?, ?, ?, ?) RETURNING bchg_id, chg_create_time"

	def insertBalanceChange(acctID: AccountID, prevChgID: BalanceChangeID, chgAmt: ChangeAmount, balAmt: BalanceAmount):
			URIO[DbConn, DbOpResult[BalanceChangeSummary]] = {
		val stmtParams = Seq[Any](acctID, mySchema.BCHG_FLAVOR_FLOW, prevChgID, chgAmt, balAmt)
		val sqlJob = mySqlExec.execSqlAndPullOneRow(INSERT_BAL_CHG, stmtParams, grabBalChangeResult)
		val resultJob = sqlJob.map(_.map(rsltPair => BalanceChangeSummary(acctID, chgAmt, balAmt, rsltPair._2)))
		resultJob.debug(".insertBalanceChange result")
	}

	private def grabBalChangeResult(rs : JdbcResultSet): (BalanceChangeID, DbTimestamp) = {
		val bchgId = rs.getLong(1)
		val chgCreateTime = rs.getTimestamp(2)
		(bchgId, chgCreateTime)
	}
}
