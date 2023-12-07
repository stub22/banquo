package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.{AccountDetails, BalanceChange, DbConn}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, CustomerAddress, CustomerName}

import zio.{RIO, URIO, ZIO}

import java.sql.{Timestamp => JsqlTimestamp}
import java.sql.{ResultSet => JdbcResultSet}

trait RoachReader {

	val mySqlExec = new SqlEffectMaker

	val SELECT_ACCT_DETAILS = "SELECT acct_id, cust_name, cust_address, acct_create_time FROM account WHERE acct_id = ?"
	def selectAccountDetails(acctID: AccountId): RIO[DbConn, AccountDetails] = {
		val stmtParams = Seq[Any](acctID)
		val grabDetailsFunc = (rs : JdbcResultSet) => {
			val resultAcctID : AccountId = rs.getString(1)
			val custName : CustomerName = rs.getString(2)
			val custAddr : CustomerAddress = rs.getString(3)
			val createStamp = rs.getTimestamp(4)
			assert(resultAcctID == acctID)
			AccountDetails(resultAcctID, custName, custAddr, createStamp)
		}
		val sqlJob: RIO[DbConn, AccountDetails] = mySqlExec.execSqlAndPullOneRow(SELECT_ACCT_DETAILS, stmtParams, grabDetailsFunc)
		???
	}

	val SELECT_LAST_BAL_CHG = ""
	def selectLastBalanceChange(acctId : AccountId) : URIO[DbConn, BalanceChange] = {
		val stmtArgs = Seq[Any](acctId)
		???
	}

	// TODO: AllBalanceChanges should be some kind of paged result set, or stream
	val SELECT_ALL_BAL_CHGS = "SELECT acct_id, cust_name, cust_address, acct_create_time"
	def selectAllBalanceChanges(acctId : AccountId) : URIO[DbConn, Iterable[BalanceChange]] = {
		val stmtArgs = Seq[Any](acctId)
		???
	}

}

