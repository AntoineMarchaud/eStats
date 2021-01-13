package com.amarchaud.estats.viewmodel.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NumberPickerViewModel : ViewModel() {
    val pickerValueMutableLiveData = MutableLiveData<Int>()
}