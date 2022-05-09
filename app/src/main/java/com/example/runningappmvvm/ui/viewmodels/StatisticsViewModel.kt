package com.example.runningappmvvm.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.runningappmvvm.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel(){

    val totalTimeRun = mainRepository.getTotalTimeINMillis()
    val totalCaloriesBurned = mainRepository.getTotalCaloriesBurned()
    val totalDistance = mainRepository.getTotalDistance()
    val totalAvgSpeed = mainRepository.getTotalAvgSpeed()

    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
}