package com.appstract.banquo.api.roach

import java.sql.{Connection => SQL_Conn}


trait DbConn {
	def getSqlConn : SQL_Conn
}

trait DbProblem

case class DbEmptyResult(opName : String, sqlTxt : String, paramsTxt : String) extends DbProblem
case class DbOtherError(opName : String, sqlTxt : String, paramsTxt : String, errorInfo : String) extends DbProblem

object DbOpResultTypes {
	type DbOpResult[X] = Either[DbProblem, X]
}
