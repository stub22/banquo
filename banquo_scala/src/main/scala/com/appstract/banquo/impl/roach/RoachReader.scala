package com.appstract.banquo.impl.roach

// TODO: In principle, we should not see these higher level Bank types here in the lower level Roach impl package.
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, CustomerAddress, CustomerName}
import com.appstract.banquo.api.bank.AccountDetails
import com.appstract.banquo.api.roach.DbOpResultTypes.{DbOpResult}
import com.appstract.banquo.api.roach.{BalanceChangeInternal, DbConn, DbEmptyResult, DbOtherError}
import zio.{RIO, URIO, ZIO}

import java.sql.{ResultSet => JdbcResultSet}

/***
 * These are all read-only operations.  They do not .commit or .rollback.
 * When these operations encounter exceptions, they are routed
 */
trait RoachReader {

	val mySqlJobMaker = new SqlEffectMaker

	val SELECT_ACCT_DETAILS = "SELECT acct_id, cust_name, cust_address, acct_create_time FROM account WHERE acct_id = ?"
	def selectAccountDetails(acctID: AccountID): URIO[DbConn, DbOpResult[AccountDetails]] = {
		val stmtParams = Seq[Any](acctID)
		val sqlJob: URIO[DbConn, DbOpResult[AccountDetails]] =
				mySqlJobMaker.execSqlAndPullOneRow(SELECT_ACCT_DETAILS, stmtParams, grabAccountDetails)

		// Redundant sanity check - make sure the acctID matches!
		// 	val jobWithSanityCheck = sqlJob.map(acctDetails => {assert(acctDetails.accountID == acctID); acctDetails})
		sqlJob
	}

	private def grabAccountDetails(rs : JdbcResultSet) : AccountDetails = {
		val resultAcctID: AccountID = rs.getString(1)
		val custName: CustomerName = rs.getString(2)
		val custAddr: CustomerAddress = rs.getString(3)
		val createStamp = rs.getTimestamp(4)
		AccountDetails(resultAcctID, custName, custAddr, createStamp)
	}

	/*
	There are at least 3 different ways we might attempt to select the LAST balance_change for an account.
	1) Select the row with the highest bchg_id (which is set by a mostly-increasing sequence)
	2) Select the row with the highest chg_create_time timestamp.
	3) Select the row with a bchg_id which is not currently used by any other row as a prev_bchg_id.

	In most cases, all of these approaches will find the same row.
	In our prototype implementation we have decided to use approach #3.

https://www.cockroachlabs.com/docs/v23.1/sql-faqs#how-do-i-auto-generate-unique-row-ids-in-cockroachdb

"Upon insert or upsert, the unique_rowid() function generates a default value from the timestamp and ID of the
node executing the insert. Such time-ordered values are likely to be globally unique except in cases where a very
large number of IDs (100,000+) are generated per node per second. Also, there can be gaps and the order is not
completely guaranteed."
	 */
	val SELECT_LAST_BAL_CHG = "SELECT bchg_id, acct_id, chg_flavor, prev_bchg_id, chg_amt, balance, chg_create_time " +
			"FROM balance_change WHERE bchg_id NOT IN SELECT "
	".stripMargin"
	def selectLastBalanceChange(acctID : AccountID) : URIO[DbConn, BalanceChangeInternal] = {
		val stmtArgs = Seq[Any](acctID)
		???
	}

	// TODO: AllBalanceChanges should be some kind of paged result set, or stream.
	// Our initial implementation returns only the last maxRecordCount records, with the most recent record first.
	val SELECT_ALL_BAL_CHGS = "SELECT bchg_id, acct_id, chg_flavor, prev_bchg_id, chg_amt, balance, chg_create_time " +
			"FROM balance_change WHERE acct_id = ? ORDER BY bchg_id DESC LIMIT ?"
	def selectRecentBalanceChanges(acctID : AccountID, maxRecordCount : Int) : URIO[DbConn, DbOpResult[Seq[BalanceChangeInternal]]] = {
		val OP_NAME = "selectRecentBalanceChanges"
		val stmtParams = Seq[Any](acctID, maxRecordCount)

		val sqlJob: ZIO[DbConn, Throwable, Seq[BalanceChangeInternal]] = mySqlJobMaker.execSqlAndPullRows(SELECT_ALL_BAL_CHGS, stmtParams, grabBalanceChange)
		val jobWithSizeHandled: RIO[DbConn, DbOpResult[Seq[BalanceChangeInternal]]] = sqlJob.map(balChgSeq => {
			val numBalChgs = balChgSeq.size
			if (numBalChgs > 0) Right(balChgSeq)
			else Left(DbEmptyResult(OP_NAME, SELECT_ALL_BAL_CHGS, stmtParams.mkString(", ")))
		})
		val jobWithErrorHandling = jobWithSizeHandled.catchAll(thrown =>
			ZIO.succeed(Left(DbOtherError(OP_NAME, SELECT_ALL_BAL_CHGS, stmtParams.mkString(", "), thrown.toString))))
		jobWithErrorHandling
	}

	private def grabBalanceChange(rs : JdbcResultSet) : BalanceChangeInternal = {
		val bchgID = rs.getLong(1)
		val acctID = rs.getString(2)
		val changeFlavor = rs.getString(3)
		val prevBchgID_opt : Option[Long] = Option(rs.getLong(4)) // May be null/None, but only when changeFlavor == 'INITIAL'
		val changeAmt = rs.getBigDecimal(5)
		val balanceAmt = rs.getBigDecimal(6)
		val createStamp = rs.getTimestamp(7)
		BalanceChangeInternal(bchgID, acctID, changeFlavor, prevBchgID_opt, changeAmt, balanceAmt, createStamp)
	}

}

