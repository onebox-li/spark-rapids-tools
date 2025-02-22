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

package com.nvidia.spark.rapids.tool.analysis.util

/**
 * Implementation of AggAccumHelper for Photon.
 * It takes the shuffleWriteValues and peakMemValues Accumulables as an argument because those
 * values are not available in the TaskModel.
 */
class AggAccumPhotonHelper(
  shuffleWriteValues: Iterable[Long],
  peakMemValues: Iterable[Long]) extends AggAccumHelper {

  override def createStageAccumRecord(): TaskMetricsAccumRec = {
    StageAggPhoton(shuffleWriteValues, peakMemValues)
  }
}
