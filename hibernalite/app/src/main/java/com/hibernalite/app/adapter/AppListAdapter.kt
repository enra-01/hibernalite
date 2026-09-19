package com.hibernalite.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.hibernalite.app.R
import com.hibernalite.app.model.AppInfo

class AppListAdapter(
    private var apps: List<AppInfo>,
    private val onSelectionChanged: (selectedCount: Int) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvPkg: TextView = itemView.findViewById(R.id.tvPackageName)
        val cbHibernate: MaterialCheckBox = itemView.findViewById(R.id.cbHibernate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.appName
        holder.tvPkg.text = app.packageName

        if (app.icon != null) {
            holder.ivIcon.setImageDrawable(app.icon)
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Avoid triggering listener when scrolling
        holder.cbHibernate.setOnCheckedChangeListener(null)
        holder.cbHibernate.isChecked = app.isSelected

        holder.cbHibernate.setOnCheckedChangeListener { _, isChecked ->
            app.isSelected = isChecked
            onSelectionChanged(getSelectedApps().size)
        }

        holder.itemView.setOnClickListener {
            holder.cbHibernate.isChecked = !holder.cbHibernate.isChecked
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateData(newApps: List<AppInfo>) {
        this.apps = newApps
        notifyDataSetChanged()
        onSelectionChanged(getSelectedApps().size)
    }

    fun getSelectedApps(): List<AppInfo> {
        return apps.filter { it.isSelected }
    }
}
