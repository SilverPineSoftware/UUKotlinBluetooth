package com.silverpine.uu.sample.bluetooth.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.silverpine.uu.sample.bluetooth.databinding.ActivityRecyclerBinding
import com.silverpine.uu.sample.bluetooth.viewmodel.RecyclerViewModel
import com.silverpine.uu.ux.UUViewModelRecyclerAdapter

abstract class RecyclerActivity: BaseActivity()
{
    protected lateinit var adapter: UUViewModelRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val viewModel = getViewModel()
        val binding = ActivityRecyclerBinding.inflate(layoutInflater)
        setupViewModel(viewModel, binding)

        viewModel.data.observe(this)
        {
            adapter.update(it)
        }

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UUViewModelRecyclerAdapter(this)
        recyclerView.adapter = adapter
        setupAdapter()

        viewModel.start()
    }

    protected abstract fun getViewModel(): RecyclerViewModel
    protected abstract fun setupAdapter()
}
