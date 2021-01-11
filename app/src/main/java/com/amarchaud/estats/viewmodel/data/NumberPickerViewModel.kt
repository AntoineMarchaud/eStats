package com.amarchaud.estats.viewmodel.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NumberPickerViewModel : ViewModel() {
    val pickerValue = MutableLiveData<Int>()
}