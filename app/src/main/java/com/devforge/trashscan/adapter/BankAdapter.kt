package com.devforge.trashscan.adapter

import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devforge.trashscan.R
import com.devforge.trashscan.databinding.AdapterBankBinding
import com.devforge.trashscan.model.Bank

class BankAdapter(
    var banks: ArrayList<Bank>,
    var listener: AdapterListener?,
    var userLat: Double = 0.0,
    var userLon: Double = 0.0
): RecyclerView.Adapter<BankAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankAdapter.ViewHolder {
        return ViewHolder(
            AdapterBankBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = banks.size

    override fun onBindViewHolder(holder: BankAdapter.ViewHolder, position: Int) {
        val bank = banks[position]

        holder.binding.bankName.text = bank.name
        holder.binding.bankName.text = bank.name
        Glide.with(holder.itemView.context)
            .load(bank.photoUrl)
            .placeholder(R.drawable.img_placeholder_square)
            .into(holder.binding.bankPhotoUrl)
        holder.binding.container.setOnClickListener {
            listener?.onClick(bank)
        }

        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, bank.latitude.toDouble(), bank.longitude.toDouble(), results)
        val distance = results[0] / 1000 // convert to kilometers
        holder.binding.bankDistance.text = String.format("%.2f KM", distance)

        val drivingSpeedKmH = 24.06 // kecepatan rata-rata berkendara di Makassar
        val estimatedTimeHours = distance / drivingSpeedKmH
        val estimatedTimeMinutes = (estimatedTimeHours * 60).toInt()
        holder.binding.bankDistanceTime.text = String.format("%d Menit", estimatedTimeMinutes)
    }

    class ViewHolder(val binding: AdapterBankBinding): RecyclerView.ViewHolder(binding.root)

    fun setData(data: List<Bank>, userLat: Double, userLon: Double) {
        this.userLat = userLat
        this.userLon = userLon
        banks.clear()
        banks.addAll(data)
        notifyDataSetChanged()
    }

    fun setFilteredList(banks: ArrayList<Bank>){
        this.banks = banks
        notifyDataSetChanged()
    }

    interface AdapterListener {
        fun onClick(bank: Bank)
    }
}
