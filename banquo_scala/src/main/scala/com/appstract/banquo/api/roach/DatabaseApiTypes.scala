package com.appstract.banquo.api.roach

import java.sql.{Connection => SQL_Conn}


/**
 * Wrapper for a JDBC connection.
 */
trait DbConn {
	def getSqlConn : SQL_Conn
}

/////////////////////////////////////////////////////////////////////////////////////////////
// Error types for results from .roach DB layer
/////////////////////////////////////////////////////////////////////////////////////////////

trait DbProblem

case class DbEmptyResult(opName : String, sqlTxt : String, paramsTxt : String) extends DbProblem
case class DbOtherError(opName : String, sqlTxt : String, paramsTxt : String, errorInfo : String) extends DbProblem

object DbOpResultTypes {
	type DbOpResult[X] = Either[DbProblem, X]
}
