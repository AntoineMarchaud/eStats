package com.amarchaud.estats.viewmodel.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.estats.utils.SingleLiveEvent

class NumberPickerViewModel : ViewModel() {

    private val _pickerValueMutableLiveData = MutableLiveData<Int>()
    val pickerValueLiveData: LiveData<Int>
        get() = _pickerValueMutableLiveData

    fun setPickerValue(pos : Int) {
        _pickerValueMutableLiveData.value = pos
    }
}