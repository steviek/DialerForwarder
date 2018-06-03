package com.sixbynine.dialerforwarder.rules

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import com.google.common.base.Joiner
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfo
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfoAdapter
import com.sixbynine.dialerforwarder.dialerappinfo.WhatsAppDialerAppInfo
import com.sixbynine.dialerforwarder.inject.BaseDialogFragment
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class AddCountryRuleDialogFragment : BaseDialogFragment() {

    companion object {
        private const val KEY_FROM = "from"
        private const val KEY_COUNTRIES = "countries"
        private const val KEY_SPINNER_INDEX = "spinner_index"
    }

    @Inject
    internal lateinit var ruleManager: RuleManager
    @Inject
    internal lateinit var countryRulePresenterFactory: CountryRulePresenterFactory
    @Inject
    internal lateinit var allDialerApps: List<@JvmSuppressWildcards DialerAppInfo>

    private lateinit var fromRadioButton: RadioButton
    private lateinit var notFromRadioButton: RadioButton
    private lateinit var countriesTextView: TextView
    private lateinit var appSpinner: Spinner
    private lateinit var whatsAppWarning: TextView

    private var appSpinnerAdapter: DialerAppInfoAdapter? = null
    private var from: Boolean = true
    private var countries: MutableList<Locale> = ArrayList()
    private var savedSpinnerIndex = -1

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_rule, null)

        from = savedInstanceState?.getBoolean(KEY_FROM) ?: true
        countries = savedInstanceState?.getStringArray(KEY_COUNTRIES)
            ?.map { country -> Locale("", country) }
            ?.toMutableList() ?: ArrayList()
        savedSpinnerIndex = savedInstanceState?.getInt(KEY_SPINNER_INDEX, -1) ?: -1

        fromRadioButton = view.findViewById(R.id.fromRadioButton)
        notFromRadioButton = view.findViewById(R.id.notFromRadioButton)
        countriesTextView = view.findViewById(R.id.countriesTextView)
        appSpinner = view.findViewById(R.id.appSpinner)
        whatsAppWarning = view.findViewById(R.id.whatsAppWarning)

        fromRadioButton.setOnCheckedChangeListener { _, isChecked -> from = isChecked }
        notFromRadioButton.setOnCheckedChangeListener { _, isChecked -> from = !isChecked }

        return AlertDialog.Builder(context!!).setTitle(R.string.add_rule).setView(view)
            .setPositiveButton(R.string.add) { _, _ ->
                from = fromRadioButton.isChecked
                ruleManager.addRule(
                    countryRulePresenterFactory.create(
                        !from,
                        RulePresenter.IN,
                        countries.map { country -> country.country },
                        appSpinnerAdapter!!.getItem(appSpinner.selectedItemPosition)))
            }.setNegativeButton(android.R.string.cancel, null).create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FROM, from)
        outState.putStringArray(KEY_COUNTRIES, countries.map(Locale::getCountry).toTypedArray())
        outState.putInt(KEY_SPINNER_INDEX, appSpinner.selectedItemPosition)
    }

    override fun onResume() {
        super.onResume()
        syncViews()
    }

    private fun syncViews() {
        if (context == null) {
            return
        }

        fromRadioButton.isChecked = from
        notFromRadioButton.isChecked = !from

        if (countries.isEmpty()) {
            countriesTextView.setText(R.string.no_countries_selected)
        } else {
            countriesTextView.text = Joiner.on(", ").join(countries.map(Locale::getDisplayCountry))
        }

        val userIsoCountry =
            (context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                .simCountryIso

        val allCountries = Locale.getISOCountries().sortedWith(Comparator<String> { first, second ->
            when {
                first.equals(userIsoCountry, ignoreCase = true) -> -1
                second.equals(userIsoCountry, ignoreCase = true) -> 1
                else -> Locale("", first)
                    .getDisplayCountry(Locale.getDefault())
                    .compareTo(Locale("", second).getDisplayCountry(Locale.getDefault()))
            }
        })

        val selectedCountries = BooleanArray(allCountries.size, { i ->
            countries.contains(Locale("", allCountries[i]))
        })

        countriesTextView.setOnClickListener { _ ->
            AlertDialog.Builder(context!!).setTitle(R.string.select_countries)
                .setMultiChoiceItems(allCountries.map { country ->
                    Locale("", country).getDisplayCountry(Locale.getDefault())
                }.toTypedArray(), selectedCountries) { _, which, checked ->
                    selectedCountries[which] = checked
                }.setPositiveButton(android.R.string.ok) { _, _ ->
                    val newCountries = ArrayList<Locale>()
                    (0 until selectedCountries.size).filter { selectedCountries[it] }
                        .forEach { newCountries.add(Locale("", allCountries[it])) }
                    countries = newCountries
                    syncViews()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }

        if (appSpinnerAdapter == null) {
            appSpinnerAdapter =
                    DialerAppInfoAdapter(allDialerApps)
            appSpinner.adapter = appSpinnerAdapter
            appSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    syncViews()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    syncViews()
                }
            }
        }

        if (savedSpinnerIndex != -1) {
            appSpinner.setSelection(savedSpinnerIndex)
            savedSpinnerIndex = -1
        }

        if (allDialerApps[appSpinner.selectedItemPosition] is WhatsAppDialerAppInfo) {
            whatsAppWarning.visibility = View.VISIBLE
        } else {
            whatsAppWarning.visibility = View.INVISIBLE
        }

        if (dialog == null) {
            return
        }

        getPositiveButton().isEnabled = !countries.isEmpty()
    }

    private fun getPositiveButton() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
}