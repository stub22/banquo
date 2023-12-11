package com.appstract.banquo.svc

import zio._
import zio.http._
import com.appstract.banquo.api.bank._
import com.appstract.banquo.api.bank.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, ChangeAmount, XactDescription}
import com.appstract.banquo.api.roach.DbConn
import com.appstract.banquo.main.RunRoachTests

/*
 * Each inbound HTTP Request should be assigned its own JDBC connection (possibly from some pool).
 * The connection is opened+closed at the boundary of the .provideLayer(dbConnLayer) step in each operation.
 */

class BanquoHttpAppBuilder(accountWriteOps: => BankAccountWriteOps, accountReadOps: => BankAccountReadOps,
						   dbConnLayer: => ZLayer[Any, Throwable, DbConn]) {
	def makeHttpApp: Http[Any, Nothing, Request, Response] = {

		Http.collectZIO[Request] {

			case Method.GET -> Root / "account" / acct_id => handleGetAccountInfo(acct_id)

			case Method.GET -> Root / "transaction" / "history" / acct_id => handleGetTransactionHistory(acct_id)

			case req@(Method.POST -> Root / "transaction") => handlePostTransaction(req)

			case Method.POST -> Root / "make-dummy-account" => handleMakeDummyAccount
		}
	}
	def handleGetAccountInfo(acctId : AccountID): UIO[Response] = {
		// 200 OK: Returns the balance and user details of the specified bank account in JSON format.
		// 404 Not Found: If the account does not exist.

		val OP_NAME = "handleGetAccountInfo"

		val acctInfJob: URIO[DbConn, AcctOpResult[AccountSummary]]
					= accountReadOps.fetchAccountInfo(acctId)

		val wiredAcctInfJob: UIO[AcctOpResult[AccountSummary]]
					= acctInfJob.provideLayer(dbConnLayer).catchAll(thrown => {
			// Most likely we got an error in the DB connection opening process.
			ZIO.succeed(Left(AcctOpError(OP_NAME, acctId, s"Exception: ${thrown.toString}")))
		})

		// This responseJob must always produce an HTTP response.
		val responseJob: UIO[Response] = wiredAcctInfJob.map(_ match {
			case Right(acctSummary) => {
				val acctSummJson = OurJsonEncoders.encodeAccountSummary(acctSummary)
				// val txt = s"acctSummary=${acctSummary}"
				Response.json(acctSummJson).withStatus(Status.Ok)
			}
			case Left(failedNoAcct : AcctOpFailedNoAccount)	=> {
				Response.text(failedNoAcct.toString).withStatus(Status.NotFound)
			}
			case other => {
				Response.text(other.toString).withStatus(Status.InternalServerError)
			}
		})

		responseJob.debug(s"${OP_NAME} Response: ")
	}


	def handleMakeDummyAccount: UIO[Response] = {
		val OP_NAME = "makeDummyAccount"
		val dummyAcctJob: URIO[DbConn, AcctOpResult[AccountID]] = RunRoachTests.mkDummyAccountOp
		val wiredDummyAcctJob: UIO[AcctOpResult[AccountID]] =
			dummyAcctJob.provideLayer(dbConnLayer).catchAll(thrown => {
				// Most likely we got an error in the DB connection opening process.
				ZIO.succeed(Left(AcctOpError(OP_NAME, "DUMMY", s"Exception: ${thrown.toString}")))
			})
		// This responseJob must always produce an HTTP response.
		val responseJob: UIO[Response] = wiredDummyAcctJob.flatMap(_ match {
			case Right(acctId) => {
				handleGetAccountInfo(acctId)
			}
			case Left(createFailed: AcctCreateFailed) => {
				ZIO.succeed(Response.text(createFailed.toString).withStatus(Status.ServiceUnavailable))
			}
			case other => {
				ZIO.succeed(Response.text(other.toString).withStatus(Status.InternalServerError))
			}
		})

		responseJob.debug(s"${OP_NAME} Response: ")
	}
	def handleGetTransactionHistory(acctId : AccountID): UIO[Response] = {
		// 200 OK: Returns the list of transaction history for the specified bank account in JSON format.
		// 404 Not Found: If the account does not exist.

		// Here our initial implementation is to eagerly, naively fetch a single chunk of account transaction history
		// into one sequence in memory, then serialize that into a Json value in memory, which is used to build the response.
		// TODO: AccountHistory should be some kind of paged result set, or stream.

		val OP_NAME = "handleGetTransactionHistory"

		val acctHistJob: URIO[DbConn, AcctOpResult[AccountHistory]] = accountReadOps.fetchAccountHistory(acctId)

		val wiredAcctHistJob: UIO[AcctOpResult[AccountHistory]] =
							acctHistJob.provideLayer(dbConnLayer).catchAll(thrown => {
			// Most likely we got an error in the DB connection opening process.
			ZIO.succeed(Left(AcctOpError(OP_NAME, acctId, s"Exception: ${thrown.toString}")))
		})

		// This responseJob must always produce an HTTP response.
		val responseJob: UIO[Response] = wiredAcctHistJob.map(_ match {
			case Right(acctHistorySeq) => {
				val historyJson = OurJsonEncoders.encodeAccountHistory(acctHistorySeq)
				// val txt = s"acctHistorySeq=${acctHistorySeq}"
				Response.json(historyJson).withStatus(Status.Ok)
			}
			case Left(failedNoAcct: AcctOpFailedNoAccount) => {
				Response.text(failedNoAcct.toString).withStatus(Status.NotFound)
			}
			case other => {
				Response.text(other.toString).withStatus(Status.InternalServerError)
			}
		})
		responseJob.debug(s"${OP_NAME} Response: ")

	}
	def handlePostTransaction(request : Request): UIO[Response] = {
		// 201 Created: If the transaction is successful it returns the transaction details in JSON format
		// 400 Bad Request: If the request body is invalid
		// 404 Not Found: If the account does not exist
		// 422 Unprocessable Entity: If the transaction fails due to insufficient funds or other reasons.

		val OP_NAME = "handlePostTransaction"
		val rb: Body = request.body
		val rbt: Task[String] = rb.asString.debug(".handlePostTransaction bodyTxt")
		rbt.foldZIO(
			bodyErr => ZIO.succeed(Response.text(bodyErr.toString).withStatus(Status.BadRequest)),
			bodyTxt => {
			val xactInEither = OurJsonDecoders.decodeXactInput(bodyTxt)
			xactInEither match {
				case Left(errMsg) => {
					val respTxt = Response.text(s"Json extraction failed, \nerrMsg=[${errMsg}], \ninput=[${bodyTxt}]")
					ZIO.succeed(respTxt.withStatus(Status.BadRequest))
				}
				case Right(xactIn) => {
					val acctID: AccountID = xactIn.account_id
					val chgAmt: ChangeAmount = xactIn.amount
					val xactDesc : XactDescription = xactIn.description
					val bchgJob: URIO[DbConn, AcctOpResult[BalanceChangeSummary]] =
							accountWriteOps.storeBalanceChange(acctID, chgAmt, xactDesc)
					val wiredBchgJob = bchgJob.provideLayer(dbConnLayer).catchAll(thrown => {
						// Most likely we got an error in the DB connection opening process.
						ZIO.succeed(Left(AcctOpError(OP_NAME, acctID, s"Exception: ${thrown.toString}")))
					})
					wiredBchgJob.map(_ match {
						case Right(bcSumm) => {
							val bcSummJson = OurJsonEncoders.encodeBalanceSummary(bcSumm)
							Response.json(bcSummJson).withStatus(Status.Created)
						}
						case Left(failedNoAcct : AcctOpFailedNoAccount) => {
							Response.text(failedNoAcct.toString).withStatus(Status.NotFound)
						}
						case Left(failedInsuff : AcctOpFailedInsufficientFunds) => {
							Response.text(failedInsuff.toString).withStatus(Status.UnprocessableEntity)
						}
						case Left(otherErr) => {
							Response.text(otherErr.toString).withStatus(Status.InternalServerError)
						}
					})
				}
			}
		})
	}
}
