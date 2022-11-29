package com.ui.sticky.recycler.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<B : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: B

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBinding()
        setContentView(binding.root)
    }

    //region Binding
    private fun setBinding() {
        binding = getBindingClass()
    }

    abstract fun getBindingClass(): B
    //endregion
}