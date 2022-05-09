package com.example.runningappmvvm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.runningappmvvm.R
import com.example.runningappmvvm.db.Run
import com.example.runningappmvvm.other.TrackingUtility
import kotlinx.android.synthetic.main.item_run.view.*
import java.text.SimpleDateFormat
import java.util.*

class RunAdapter:  RecyclerView.Adapter<RunAdapter.RunViewHolder>(){

    inner class RunViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        RunViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false))

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = differ.currentList[position]
        val calendar = Calendar.getInstance().apply {
            timeInMillis = run.timeStamp
        }
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        holder.itemView.apply {
            Glide.with(this).load(run.img).diskCacheStrategy(DiskCacheStrategy.ALL).into(ivRunImage)
            tvDate.text = dateFormat.format(calendar.time)
            tvTime.text = TrackingUtility.getFormattedStpWatchTime(run.timeInMillis)
            tvDistance.text = "${run.distanceInMeters / 1000f}Km"
            tvAvgSpeed.text = "${run.avgSpeedINKMH}Km/h"
            tvCalories.text = "${run.caloriesBurned}Kcal"
        }
    }

    override fun getItemCount() = differ.currentList.size

    private val diffCallback = object : DiffUtil.ItemCallback<Run>(){
        override fun areItemsTheSame(oldItem: Run, newItem: Run) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Run, newItem: Run) = oldItem.hashCode() == newItem.hashCode()
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)
}