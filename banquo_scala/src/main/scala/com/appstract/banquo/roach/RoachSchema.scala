package com.appstract.banquo.roach

import zio.Task

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}

object RoachSchema {
	val TABLE_ACCOUNT = "account"
	val COL_ACCT_ID = "acct_id"
	val CREATE_TABLE_ACCOUNT =
		"CREATE TABLE IF NOT EXISTS account (acct_id UUID PRIMARY KEY, cust_name STRING, cust_address STRING)"


	val CREATE_ENUM_BAL_CHG_FLAVOR = "CREATE TYPE IF NOT EXISTS bal_chg_flavor AS ENUM ('INITIAL', 'UPDATE')"


	val COL_BCHG_ID = "bchg_id"
	// prev_bchg_id is NULL when
	val COL_PREV_BCHG_ID = "prev_bchg_id"
	val CREATE_TABLE_BALANCE_CHG =
		"""CREATE TABLE IF NOT EXISTS balance_change (bchg_id INT DEFAULT unique_rowid(), acct_id UUID,
				chg_flavor bal_chg_flavor, prev_bchg_id INT, chg_amt DECIMAL, balance DECIMAL) """

	val mySqlExec = new SqlExecutor

	def createTablesAsNeeded(implicit sqlConn: SQL_Conn): Unit = {
		mySqlExec.runSome(CREATE_TABLE_ACCOUNT)
		mySqlExec.runSome(CREATE_ENUM_BAL_CHG_FLAVOR)
		mySqlExec.runSome(CREATE_TABLE_BALANCE_CHG)
	}
}

object BankTypes {
	type AccountId = String
	type CustomerName = String
	type CustomerAddress = String
	type BalanceChangeId = Long
	type ChangeAmount = BigDecimal
	type BalanceAmount = BigDecimal
}
import BankTypes._
case class AccountDetails(acctID : AccountId, customerName: CustomerName, customerAddress: CustomerAddress)
case class BalanceChange(changeId : BalanceChangeId, acctID : AccountId, prevChangeId_opt : Option[BalanceChangeId],
					changeAmt: ChangeAmount, balanceAmt : BalanceAmount)

trait RoachWriter {
	val mySchema = RoachSchema
	val mySqlExec = new SqlExecutor

	val INSERT_ACCT = "INSERT INTO account (cust_name, cust_address) VALUES (?, ?) RETURNING acct_id"
	def insertAccount(customerName : String, customerAddress : String)(implicit sqlConn: SQL_Conn): Either[DbError, AccountId] = {
		val stmtArgs = Array[String](customerName, customerAddress)
		mySqlExec.runSome(INSERT_ACCT)
		???
	}
	def insertInitialBalance(acctId : AccountId, initAmt : BalanceAmount) : Either[DbError, BalanceChangeId] = ???

	def insertBalanceChange(acctId : AccountId, prevChgId : BalanceChangeId, chgAmt : ChangeAmount, balAmt : BalanceAmount)
			: Either[DbError, BalanceChangeId] = ???
}
trait RoachReader {
	def selectLastBalanceChange(acctId : AccountId) : BalanceChange = ???

	// TODO: AllBalanceChanges should be some kind of stream or paged result.
	def selectAllBalanceChanges(acctId : AccountId) : Iterable[BalanceChange] = ???

	def selectAccountDetails(acctId : AccountId) : AccountDetails = ???
}

// Xact stands for "transaction" in the context of a bank account (not a database).
trait BankAccountXactWriter {
	val myRoachWriter = new RoachWriter {}
	val myRoachReader = new RoachReader {}

	def makeAccount (customerName : String, customerAddress : String, initBal : BalanceAmount)(implicit sqlConn: SQL_Conn) = {
		// Must insert the Account record AND create an initial balance record.
		val x: Either[DbError, (AccountId, BalanceChangeId)] = for {
			acctId <- myRoachWriter.insertAccount(customerName, customerAddress)
			initChgId <- myRoachWriter.insertInitialBalance(acctId, initBal)
		} yield (acctId, initChgId)

	}

	def storeAccountXact(acctID : AccountId, changeAmt: ChangeAmount)(implicit sqlConn: SQL_Conn) : Task[BalanceChange] = {
		val previousChange = myRoachReader.selectLastBalanceChange(acctID)
		???
	}
}