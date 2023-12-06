package com.appstract.banquo.roach

import zio.{Task, UIO, ZIO}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}

object RoachSchema {
	val TABLE_ACCOUNT = "account"
	val COL_ACCT_ID = "acct_id"
	val CREATE_TABLE_ACCOUNT =
		"CREATE TABLE IF NOT EXISTS account (acct_id UUID PRIMARY KEY, cust_name STRING, cust_address STRING)"

	val CREATE_ENUM_BAL_CHG_FLAVOR = "CREATE TYPE IF NOT EXISTS bal_chg_flavor AS ENUM ('INITIAL', 'UPDATE')"

	val COL_BCHG_ID = "bchg_id"
	// prev_bchg_id should be NULL when bal_chg_flavor is INITIAL.
	// UNIQUE constraint on prev_bchg_id should prevent forking by simultaneous transactions on the same account.
	// (We expect one of the transactions to fail.
	// This failure should actually happen even without the UNIQUE constraint, assuming Cockroach SERIALIZABLE fails on
	// phantom reads).
	val COL_PREV_BCHG_ID = "prev_bchg_id"
	val CREATE_TABLE_BALANCE_CHG =
		"""CREATE TABLE IF NOT EXISTS balance_change (
	 			bchg_id INT PRIMARY KEY DEFAULT unique_rowid(),
	 			acct_id UUID,
				chg_flavor bal_chg_flavor,
				prev_bchg_id INT UNIQUE,
				chg_amt DECIMAL,
				balance DECIMAL) """


	val mySqlExec = new SqlExecutor

	def createTablesAsNeeded = {
		val z1 = mySqlExec.execUpdateNoResult(CREATE_TABLE_ACCOUNT).debug("CREATE_TABLE_ACCOUNT")
		val z2 = mySqlExec.execUpdateNoResult(CREATE_ENUM_BAL_CHG_FLAVOR).debug("CREATE_ENUM_BAL_CHG_FLAVOR")
		val z3 = mySqlExec.execUpdateNoResult(CREATE_TABLE_BALANCE_CHG).debug("CREATE_TABLE_BALANCE_CHG")
		(z1 *> z2 *> z3).unit
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
	val mySqlExec = new DirectSqlExecutor

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
	def selectLastBalanceChange(acctId : AccountId) : Either[DbError,BalanceChange] = ???

	// TODO: AllBalanceChanges should be some kind of stream or paged result.
	def selectAllBalanceChanges(acctId : AccountId) : Either[DbError, Iterable[BalanceChange]] = ???

	def selectAccountDetails(acctId : AccountId) : Either[DbError, AccountDetails] = ???
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

	def storeAccountXact(acctID : AccountId, changeAmt: ChangeAmount)(implicit sqlConn: SQL_Conn) : ZIO[Any, DbError, BalanceChangeId] = {
		val combinedResultEith: Either[DbError, BalanceChangeId] = for {
			previousChange <- myRoachReader.selectLastBalanceChange(acctID)
			nxtBalAmt = previousChange.balanceAmt.+(changeAmt)
			nextChgId <- myRoachWriter.insertBalanceChange(acctID, previousChange.changeId, changeAmt, nxtBalAmt)
		} yield(nextChgId)
		ZIO.fromEither(combinedResultEith)
	}
}