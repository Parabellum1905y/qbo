package me.luger.qbosync.utils

import scala.concurrent.{ ExecutionContext, Future }

object MonadTransformers {

  implicit class FutureOptionMonadTransformer[A](t: Future[Option[A]])(implicit executionContext: ExecutionContext) {

    def mapT[B](f: A => B): Future[Option[B]] =
      t.map(_.map(f))

    def filterT(f: A => Boolean): Future[Option[A]] =
      t.map {
        case Some(data) if f(data) =>
          Some(data)
        case _ =>
          None
      }

    def flatMapT[B](f: A => Future[Option[B]]): Future[Option[B]] =
      t.flatMap {
        case Some(data) =>
          f(data)
        case None =>
          Future.successful(None)
      }

    def flatMapTOuter[B](f: A => Future[B]): Future[Option[B]] =
      t.flatMap {
        case Some(data) =>
          f(data).map(Some.apply)
        case None =>
          Future.successful(None)
      }
  }

  implicit class FutureEitherMonadTransformer[E, A](t: Either[E, Future[A]])(
      implicit executionContext: ExecutionContext
  ) {
    def foldT(): Future[Either[E, A]] =
      t match {
        case Left(s)  => Future.successful(Left(s))
        case Right(f) => f.map(Right(_))
      }
  }

}
