package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.GoalItem
import com.myday.app.domain.model.GoalStatus
import com.myday.app.domain.model.GoalWithMilestones
import com.myday.app.domain.model.MilestoneItem
import com.myday.app.domain.repository.CategoryRepository
import com.myday.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalsUiState(
    val isLoading: Boolean = true,
    val selectedFilter: GoalStatus? = null,
    val goals: List<GoalWithMilestones> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<GoalStatus?>(null)

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.observeGoalsWithMilestones(),
        categoryRepository.observeAll(),
        selectedFilter
    ) { allGoals, categories, filter ->
        val filtered = if (filter == null) allGoals else allGoals.filter { it.goal.status == filter }
        GoalsUiState(
            isLoading = false,
            selectedFilter = filter,
            goals = filtered,
            categories = categories,
            activeCount = allGoals.count { it.goal.status == GoalStatus.ACTIVE },
            completedCount = allGoals.count { it.goal.status == GoalStatus.COMPLETED }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsUiState()
    )

    fun selectFilter(status: GoalStatus?) {
        selectedFilter.value = status
    }

    fun saveGoal(
        existing: GoalItem?,
        title: String,
        description: String,
        categoryId: String?,
        colorHex: Long,
        targetEpochDay: Long?,
        manualProgressPercent: Int
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val goal = GoalItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                categoryId = categoryId,
                colorHex = colorHex,
                targetEpochDay = targetEpochDay,
                status = existing?.status ?: GoalStatus.ACTIVE,
                manualProgressPercent = manualProgressPercent.coerceIn(0, 100),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
                completedAtMillis = existing?.completedAtMillis
            )
            goalRepository.upsertGoal(goal)
        }
    }

    fun updateStatus(goalId: String, status: GoalStatus) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, status)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goalId)
        }
    }

    fun addMilestone(goalId: String, title: String, sortOrder: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            goalRepository.upsertMilestone(
                MilestoneItem(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    title = title.trim(),
                    isCompleted = false,
                    sortOrder = sortOrder,
                    completedAtMillis = null
                )
            )
        }
    }

    fun toggleMilestone(milestoneId: String) {
        viewModelScope.launch {
            goalRepository.toggleMilestone(milestoneId)
        }
    }

    fun deleteMilestone(milestoneId: String) {
        viewModelScope.launch {
            goalRepository.deleteMilestone(milestoneId)
        }
    }
}
