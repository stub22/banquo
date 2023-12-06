package com.appstract.banquo.api
import com.appstract.banquo.api.BankScalarTypes.AccountId

import java.sql.{Connection => SQL_Conn}


trait DbConn {
	def getSqlConn : SQL_Conn
}

trait DbProblem

case class DbFailureNoAccount(accountId: AccountId, details : String) extends DbProblem
case class DbFailureInsufficientFunds(accountId: AccountId, details : String) extends DbProblem

case class DbError(details : String) extends DbProblem

object DbResultTypes {
	type DbResult[X] = Either[DbProblem, X]
}
