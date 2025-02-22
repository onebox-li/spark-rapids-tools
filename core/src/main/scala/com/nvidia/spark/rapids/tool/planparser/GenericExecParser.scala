/*
 * Copyright (c) 2024, NVIDIA CORPORATION.
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

package com.nvidia.spark.rapids.tool.planparser

import com.nvidia.spark.rapids.tool.planparser.ops.UnsupportedExprOpRef
import com.nvidia.spark.rapids.tool.qualification.PluginTypeChecker

import org.apache.spark.sql.execution.ui.SparkPlanGraphNode
import org.apache.spark.sql.rapids.tool.AppBase

class GenericExecParser(
    val node: SparkPlanGraphNode,
    val checker: PluginTypeChecker,
    val sqlID: Long,
    val execName: Option[String] = None,
    val expressionFunction: Option[String => Array[String]] = None,
    val app: Option[AppBase] = None
) extends ExecParser {

  val fullExecName: String = execName.getOrElse(node.name + "Exec")

  override def parse: ExecInfo = {
    val duration = computeDuration
    val expressions = parseExpressions()

    val notSupportedExprs = getNotSupportedExprs(expressions)
    val isExecSupported = checker.isExecSupported(fullExecName) &&
      notSupportedExprs.isEmpty

    val (speedupFactor, isSupported) = if (isExecSupported) {
      (checker.getSpeedupFactor(fullExecName), true)
    } else {
      (1.0, false)
    }

    createExecInfo(speedupFactor, isSupported, duration,
      notSupportedExprs = notSupportedExprs, expressions = expressions)
  }

  protected def parseExpressions(): Array[String] = {
    expressionFunction match {
      case Some(func) =>
        val exprString = getExprString
        func(exprString)
      case None =>
        Array.empty[String]
    }
  }

  protected def getExprString: String = {
    node.desc.replaceFirst(s"^${node.name}\\s*", "")
  }

  protected def getNotSupportedExprs(expressions: Array[String]): Seq[UnsupportedExprOpRef] = {
    checker.getNotSupportedExprs(expressions)
  }

  protected def getDurationMetricIds: Seq[Long] = {
    Seq.empty
  }

  protected def computeDuration: Option[Long] = {
    // Sum the durations for all metrics returned by getDurationMetricIds
    val durations = getDurationMetricIds.flatMap { metricId =>
      app.flatMap(appInstance => SQLPlanParser.getTotalDuration(Some(metricId), appInstance))
    }
    durations.reduceOption(_ + _)
  }

  protected def createExecInfo(
      speedupFactor: Double,
      isSupported: Boolean,
      duration: Option[Long],
      notSupportedExprs: Seq[UnsupportedExprOpRef],
      expressions: Array[String]
  ): ExecInfo = {
    ExecInfo(
      node,
      sqlID,
      // Remove trailing spaces from node name if any
      node.name.trim,
      "",
      speedupFactor,
      duration,
      node.id,
      isSupported,
      None,
      unsupportedExprs = notSupportedExprs,
      expressions = expressions
    )
  }
}

object GenericExecParser {
  def apply(
      node: SparkPlanGraphNode,
      checker: PluginTypeChecker,
      sqlID: Long,
      execName: Option[String] = None,
      expressionFunction: Option[String => Array[String]] = None,
      app: Option[AppBase] = None
  ): GenericExecParser = {
    val fullExecName = execName.getOrElse(node.name + "Exec")
    new GenericExecParser(node, checker, sqlID, Some(fullExecName), expressionFunction, app)
  }
}
