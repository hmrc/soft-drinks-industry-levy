/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest._
import cats.Monad
import cats.implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.stm.TMap

class MemoizedWithSTMSpec extends AsyncFlatSpec {

  def testFunc(k: String): Future[LocalDateTime] = Future(LocalDateTime.now)
  val cache: TMap[String, (LocalDateTime, LocalDateTime)] = TMap[String, (LocalDateTime, LocalDateTime)]()

  "Memoized function" should "return a cached result" in {
    val memoized = Memoized.memoizedCache[Future, String, LocalDateTime](cache, 60 * 60)(testFunc)
    val x = for {
      a <- memoized("test1")
      b <- memoized("test1")
    } yield (a, b)
    x.map { y =>
      assert(y._1 == y._2)
    }

  }

  "Memoized function" should "return a fresh result when the cache has timed out" in {
    val memoized = Memoized.memoizedCache[Future, String, LocalDateTime](cache, 1)(testFunc)
    val x = for {
      a <- memoized("test2")
      _ = Thread.sleep(2000)
      b <- memoized("test2")
    } yield (a, b)
    x.map { y =>
      assert(y._1 != y._2)
    }
  }
}
