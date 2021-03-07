package com.amarchaud.estats.viewmodel.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NumberPickerViewModel : ViewModel() {

    private val _pickerValueMutableLiveData = MutableLiveData<Int>()
    val pickerValueMutableLiveData: LiveData<Int>
        get() = _pickerValueMutableLiveData

    fun setPickerValue(pos : Int) {
        _pickerValueMutableLiveData.value = pos
    }
}