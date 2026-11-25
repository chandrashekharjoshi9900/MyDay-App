package com.myday.app.domain.calculator

import com.myday.app.domain.model.GoalItem
import com.myday.app.domain.model.GoalStatus
import com.myday.app.domain.model.MilestoneItem
import kotlin.math.roundToInt

object GoalProgressCalculator {

    fun calculateProgressPercent(goal: GoalItem, milestones: List<MilestoneItem>): Int {
        if (goal.status == GoalStatus.COMPLETED) return 100
        if (milestones.isEmpty()) {
            return goal.manualProgressPercent.coerceIn(0, 100)
        }
        val completedCount = milestones.count { it.isCompleted }
        return ((completedCount.toFloat() / milestones.size.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }
}
