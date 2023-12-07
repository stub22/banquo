package com.appstract.banquo.api
import com.appstract.banquo.api.BankScalarTypes.AccountId

import java.sql.{Connection => SQL_Conn}


trait DbConn {
	def getSqlConn : SQL_Conn
}
