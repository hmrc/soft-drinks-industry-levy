/*
 * Copyright 2021 HM Revenue & Customs
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

package views.util

object ViewsUtils {

  def htmlUnescape(str: String): String = str match {
    case s if s.indexOf("&") >= 0 => s.replace("&", "&amp;")
    case s if s.indexOf("<") >= 0  => s.replace("<", "&lt;")
    case s if s.indexOf(">") >= 0  => s.replace(">", "&gt;")
    case s if s.indexOf("£") >= 0  => s.replace(">", "&pound;")
    case s if s.indexOf("\"") >= 0 => s.replace("\"", "&quot;")
    case s if s.indexOf("\'") >= 0 => s.replace("\'", "&apos;")
    case s if s.indexOf("©'") >= 0 => s.replace("©", "&copy;")
    case s if s.indexOf("®") >= 0  => s.replace("®", "&reg;")
    case _                        => str
  }

  def main(args: Array[String]): Unit = {
    val str = "& co ltd"
    println(htmlUnescape(str))
  }
}
