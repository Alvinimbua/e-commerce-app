package com.project.bestladyapp.utils

import android.widget.Toast
import androidx.fragment.app.Fragment

internal fun Fragment.showToast(message:String){
	Toast.makeText(requireContext(),message,Toast.LENGTH_LONG).show()
}