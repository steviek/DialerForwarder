package com.sixbynine.dialerforwarder.dialerappinfo

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.sixbynine.dialerforwarder.R.id
import com.sixbynine.dialerforwarder.R.layout
import com.sixbynine.dialerforwarder.inflateChild

class DialerAppInfoAdapter(private val apps: List<DialerAppInfo>) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val appInfo = getItem(position)

        val view = convertView ?: parent.inflateChild(layout.dialer_app_list_item)

        view.findViewById<ImageView>(id.app_icon).setImageDrawable(appInfo.icon)
        view.findViewById<TextView>(id.app_name).text = appInfo.label

        return view
    }

    override fun getItem(position: Int) = apps[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = apps.size
}