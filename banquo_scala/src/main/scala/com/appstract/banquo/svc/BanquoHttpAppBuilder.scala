package com.appstract.banquo.svc

import com.appstract.banquo.api.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount, ChangeAmount}
import com.appstract.banquo.api.{AccountDetails, AcctOpError, AcctOpFailedNoAccount, BankAccountReadOps, BankAccountWriteOps, DbConn}
import com.appstract.banquo.impl.bank.BankAccountWriteOpsImpl
import zio._
import zio.http._
import zio.json._

/*
We want each inbound HTTP Request to be assigned its own JDBC connection (possibly from some pool).
 */

class BanquoHttpAppBuilder(accountWriteOps: => BankAccountWriteOps, accountReadOps: => BankAccountReadOps,
						   dbConnLayer: => ZLayer[Any, Throwable, DbConn]) {
	def makeHttpApp: Http[Any, Nothing, Request, Response] = {

		Http.collectZIO[Request] {

			case Method.GET -> Root / "account" / acct_id => handleGetAccountInfo(acct_id)

			case Method.GET -> Root / "transaction" / "history" / acct_id => handleGetTransactionHistory(acct_id)

			case req@(Method.POST -> Root / "transaction") => handlePostTransaction(req)

		}
	}
	def handleGetAccountInfo(acctId : AccountId): UIO[Response] = {
		// 200 OK: Returns the balance and user details of the specified bank account in JSON format.
		// 404 Not Found: If the account does not exist.

		val OP_NAME = "handleGetAccountInfo"

		val acctInfJob: URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]]
					= accountReadOps.fetchAccountInfo(acctId)

		val wiredAcctInfJob: UIO[AcctOpResult[(AccountDetails, BalanceAmount)]]
					= acctInfJob.provideLayer(dbConnLayer).catchAll(thrown => {
			ZIO.succeed(Left(AcctOpError(OP_NAME, acctId, s"Exception: ${thrown.toString}")))
		})

		// This responseJob must always produce an HTTP response.
		val responseJob: UIO[Response] = wiredAcctInfJob.map(_ match {
			case Right((acctInfo, balAmt)) => {
				val txt = s"acctInfo=${acctInfo}, balAmt=${balAmt}"
				Response.text(txt).withStatus(Status.Ok)
			}
			case Left(failedNoAcct : AcctOpFailedNoAccount)	=> {
				Response.text(failedNoAcct.toString).withStatus(Status.NotFound)
			}
			case other => {
				Response.text(other.toString).withStatus(Status.InternalServerError)
			}
		})
		// When we apply the dbLayer, we now have a job that might fail in the case where
		responseJob.debug(s"${OP_NAME} Response: ")
	}

	def handleGetTransactionHistory(acctId : AccountId): UIO[Response] = {
		// 200 OK: Returns the list of transaction history for the specified bank account in JSON format.
		// 404 Not Found: If the account does not exist.

		// TODO: AccountHistory should be some kind of paged result set, or stream.
		// But our initial implementation is to eagerly, naively fetch ALL account transactions into one
		// sequence in memory, then serialize that into a Json value in memory.
		val OP_NAME = "handleGetTransactionHistory"

		val acctHistJob: URIO[DbConn, AcctOpResult[AccountHistory]] = accountReadOps.fetchAccountHistory(acctId)

		val wiredAcctHistJob: UIO[AcctOpResult[AccountHistory]] =
							acctHistJob.provideLayer(dbConnLayer).catchAll(thrown => {
			ZIO.succeed(Left(AcctOpError(OP_NAME, acctId, s"Exception: ${thrown.toString}")))
		})

		// This responseJob must always produce an HTTP response.
		val responseJob: UIO[Response] = wiredAcctHistJob.map(_ match {
			case Right(acctHistorySeq) => {
				val txt = s"acctHistorySeq=${acctHistorySeq}"
				Response.text(txt).withStatus(Status.Ok)
			}
			case Left(failedNoAcct: AcctOpFailedNoAccount) => {
				Response.text(failedNoAcct.toString).withStatus(Status.NotFound)
			}
			case other => {
				Response.text(other.toString).withStatus(Status.InternalServerError)
			}
		})
		// When we apply the dbLayer, we now have a job that might fail in the case where
		responseJob.debug(s"${OP_NAME} Response: ")

	}
	def handlePostTransaction(request : Request) = {
		// 201 Created: If the transaction is successful it returns the transaction details in JSON format
		// 400 Bad Request: If the request body is invalid
		// 404 Not Found: If the account does not exist
		// 422 Unprocessable Entity: If the transaction fails due to insufficient funds or other reasons.

		val OP_NAME = "handlePostTransaction"

		val bodyTxt = request.body.asString.debug("POST /transaction with bodyTxt")

		// def storeBalanceChange(acctID: AccountId, changeAmt: ChangeAmount): URIO[DbConn, AcctOpResult[Unit]]

		ZIO.succeed(Response.text("bad request").withStatus(Status.BadRequest))

	}
}
