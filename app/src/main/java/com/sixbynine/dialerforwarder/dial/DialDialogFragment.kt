package com.sixbynine.dialerforwarder.dial

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.sixbynine.dialerforwarder.ContactCaller
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.inject.BaseDialogFragment
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.android.synthetic.main.fragment_dial.*
import java.util.Locale
import javax.inject.Inject

class DialDialogFragment : BaseDialogFragment() {

    @Inject
    internal lateinit var phoneNumberUtil: PhoneNumberUtil
    @Inject
    internal lateinit var contactCaller: ContactCaller
    @Inject
    internal lateinit var permissionChecker: PermissionChecker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            numberEditText.showSoftInputOnFocus = false
        } else {
            numberEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
            numberEditText.setTextIsSelectable(true)
        }

        dialButton0.setOnClickListener { dial(0) }
        dialButton1.setOnClickListener { dial(1) }
        dialButton2.setOnClickListener { dial(2) }
        dialButton3.setOnClickListener { dial(3) }
        dialButton4.setOnClickListener { dial(4) }
        dialButton5.setOnClickListener { dial(5) }
        dialButton6.setOnClickListener { dial(6) }
        dialButton7.setOnClickListener { dial(7) }
        dialButton8.setOnClickListener { dial(8) }
        dialButton9.setOnClickListener { dial(9) }
        dialButtonNumber.setOnClickListener { dial("#") }
        dialButtonStar.setOnClickListener { dial("*") }
        dialButton0.setOnLongClickListener { dial("+"); true }
        backspaceButton.setOnClickListener {
            if (numberEditText.selectionStart > 0) {
                numberEditText.text.delete(
                    numberEditText.selectionStart - 1,
                    numberEditText.selectionStart
                )
            }
        }
        backspaceButton.setOnLongClickListener {
            if (numberEditText.selectionStart > 0) {
                numberEditText.text.delete(
                    0,
                    numberEditText.selectionStart
                )
            }
            true
        }

        numberEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val potentialPhoneNumber = s.toString()
                try {
                    val callAction =
                        if (permissionChecker.shouldMakeCalls()) Intent.ACTION_CALL else Intent.ACTION_DIAL
                    phoneNumberUtil.parse(potentialPhoneNumber, Locale.getDefault().country)
                    val dialerAppInfo = contactCaller.getAppInfoForContact(potentialPhoneNumber)
                    callButton.visibility = View.VISIBLE
                    callButton.setOnClickListener {
                        contactCaller.forwardIntentForContact(
                            phoneNumber = potentialPhoneNumber, action = callAction
                        )
                    }
                    callButton.setOnLongClickListener {
                        contactCaller.forwardIntentForContact(
                            phoneNumber = potentialPhoneNumber,
                            action = callAction,
                            showPicker = true
                        )
                        true
                    }
                    if (dialerAppInfo != null) {
                        callButton.setImageDrawable(dialerAppInfo.icon)
                    } else {
                        callButton.setImageResource(R.drawable.ic_call)
                    }
                } catch (e: Exception) {
                    // Wasn't a valid phone number
                    callButton.visibility = View.INVISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun dial(number: Int) {
        dial(number.toString())
    }

    private fun dial(string: String) {
        numberEditText.text.insert(numberEditText.selectionStart, string)
    }
}