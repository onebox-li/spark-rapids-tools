/*
 * Copyright (c) 2022-2024, NVIDIA CORPORATION.
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

import com.nvidia.spark.rapids.tool.qualification.PluginTypeChecker

import org.apache.spark.internal.Logging
import org.apache.spark.sql.execution.ui.SparkPlanGraphNode
import org.apache.spark.sql.rapids.tool.AppBase

case class ShuffledHashJoinExecParser(
    node: SparkPlanGraphNode,
    checker: PluginTypeChecker,
    sqlID: Long,
    app: AppBase) extends ExecParser with Logging {

  val fullExecName = node.name + "Exec"

  override def parse: ExecInfo = {
    // TODO - Its partial duration only. We need a way to specify it as partial.
    val accumId = node.metrics.find(_.name == "time to build hash map").map(_.accumulatorId)
    val maxDuration = SQLPlanParser.getTotalDuration(accumId, app)
    val exprString = node.desc.replaceFirst("ShuffledHashJoin ", "")
    val (expressions, supportedJoinType) = SQLPlanParser.parseEquijoinsExpressions(exprString)
    val notSupportedExprs = expressions.filterNot(expr => checker.isExprSupported(expr))
    val (speedupFactor, isSupported) = if (checker.isExecSupported(fullExecName) &&
      notSupportedExprs.isEmpty && supportedJoinType) {
      (checker.getSpeedupFactor(fullExecName), true)
    } else {
      (1.0, false)
    }
    ExecInfo(node, sqlID, node.name, "", speedupFactor,
      maxDuration, node.id, isSupported, children = None, expressions = expressions)
  }
}
