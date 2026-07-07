package com.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.data.local.datastore.UserPreferences
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<UserPreferences> = settingsRepository.userPreferencesFlow
}

class SetTimezoneUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(timezoneId: String) = settingsRepository.setTimezoneId(timezoneId)
}
