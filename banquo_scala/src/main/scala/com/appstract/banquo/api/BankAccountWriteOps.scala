package com.appstract.banquo.api

import zio.ZIO
import java.sql.{Connection => SQL_Conn}
import com.appstract.banquo.api.BankScalarTypes._

trait DbConn {
	def getSqlConn : SQL_Conn
}

trait BankAccountWriteOps {
	def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount):
			ZIO[DbConn, Throwable, (AccountId, BalanceChangeId)]
}
