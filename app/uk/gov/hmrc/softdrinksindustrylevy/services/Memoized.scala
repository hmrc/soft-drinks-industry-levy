/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.softdrinksindustrylevy.services

import java.time.LocalDateTime

import cats.Monad
import cats.implicits._
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.stm.{TMap, atomic}
import scala.language.higherKinds

trait Memoized {

  def memoized[F[_] : Monad,A,B](
    f: A => F[B],
    cacheRead: A => F[Option[(B,LocalDateTime)]],
    cacheWrite: (A, (B,LocalDateTime)) => F[Unit],
    ttl: LocalDateTime
  ): A => F[B] = { args =>
    cacheRead(args).flatMap {
      case Some((v,d)) if d.isBefore(ttl) => {
        v.pure[F]
      }
      case None => {
        f(args).flatMap { z =>
          cacheWrite(args, (z, LocalDateTime.now())).map(_ => z)
        }
      }
    }
  }

  protected trait Cache[F[_],A,B] {
    def read(key: A)(implicit ec: ExecutionContext): F[Option[(B,LocalDateTime)]]
    def write(key: A, value: (B,LocalDateTime))(implicit ec: ExecutionContext): F[Unit]
    def ttl: LocalDateTime
  }

}

class MemoizedWithSTM[F[_]:Monad,A,B](hours: Int) extends Memoized {

  val underlyingCache: TMap[A, (Option[B], LocalDateTime)] = TMap[A, (Option[B], LocalDateTime)]()
  val cache: Cache[F, A, Option[B]] = new Cache[F, A, Option[B]] {

    override def read(key: A)(implicit ec: ExecutionContext): F[Option[(Option[B], LocalDateTime)]] = {
      Logger.info("#################################################################################")
      Logger.info(s"Reading from MemoizedWithSTM cache with key: $key")
      Logger.info("#################################################################################")
      atomic {implicit t => underlyingCache.get(key)}.pure[F]
    }

    override def write(key: A, value: (Option[B], LocalDateTime))(implicit ec: ExecutionContext): F[Unit] = {
      Logger.info("#################################################################################")
      Logger.info(s"Writing to MemoizedWithSTM cache with key: $key and value: $value")
      Logger.info("#################################################################################")
      atomic(implicit t => underlyingCache.put(key, value)).map(x => ()).getOrElse(()).pure[F]
    }

    override def ttl: LocalDateTime = LocalDateTime.now().plusHours(hours)
  }

  def getSubscription(f: A => F[Option[B]], url: A)
    (implicit ec: ExecutionContext): F[Option[B]] = {

    memoized(
      f,
      cache.read,
      cache.write,
      cache.ttl
    ).apply(url)
  }

}



