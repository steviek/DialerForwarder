package com.sixbynine.dialerforwarder

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ListView
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfo
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfoAdapter
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class AppChooserDialogShower @Inject constructor(
    private val activity: Activity,
    private val dialerAppInfos: List<@JvmSuppressWildcards DialerAppInfo>
) {

    @SuppressLint("InflateParams")
    fun showAppChooserDialog(
        callback: (DialerAppInfo?) -> Unit,
        title: String = activity.getString(R.string.choose_app),
        filter: (DialerAppInfo) -> Boolean = { _ -> true }
    ) {

        val listView = LayoutInflater.from(activity).inflate(
            R.layout.call_dialog_view,
            null) as ListView
        listView.adapter = DialerAppInfoAdapter(dialerAppInfos.filter(filter))
        listView.divider = null

        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(listView)
            .setOnCancelListener { callback(null) }
            .show()

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            run {
                callback(dialerAppInfos[position])
                dialog.dismiss()
            }
        }
    }
}