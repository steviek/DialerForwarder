package com.sixbynine.dialerforwarder.contacts

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.github.tamir7.contacts.Contact
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.sixbynine.dialerforwarder.ContactCaller
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.getDistinctPhoneNumbers
import com.squareup.picasso.Picasso

internal class ContactsAdapter @AutoFactory constructor(
    @Provided private val listener: OnContactClickListener,
    @Provided private val contactCaller: ContactCaller,
    @Provided private val contactManager: ContactManager,
    var contacts: List<Contact>,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CONTACT = 0
        private const val VIEW_TYPE_DIVIDER = 1
    }

    private val colorGenerator: ColorGenerator = ColorGenerator.MATERIAL
    var selectedContact: Contact? = null
    private var numFavourites = 0

    interface OnContactClickListener {
        fun onContactClick(contact: Contact)

        fun onContactLongClick(contact: Contact)

        fun onDialerAppIconClick(contact: Contact)
    }

    init {
        numFavourites = contacts.count { contact -> contactManager.isFavourite(contact) }
    }

    internal class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    internal class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.findViewById(R.id.container)
        val imageViewCircle: ImageView = itemView.findViewById(R.id.profile_image_circle)
        val imageView: ImageView = itemView.findViewById(R.id.profile_image)
        val textView: TextView = itemView.findViewById(R.id.profile_name)
        var appImageView: ImageView = itemView.findViewById(R.id.app_image)
    }

    override fun getItemCount() = contacts.size + if (numFavourites > 0) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (numFavourites > 0 && position == numFavourites) {
            VIEW_TYPE_DIVIDER
        } else {
            VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == VIEW_TYPE_DIVIDER) {
            return DividerViewHolder(layoutInflater.inflate(R.layout.divider, parent, false))
        }

        return ContactViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.contact_list_item, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is ContactViewHolder) {
            return
        }

        val relativePosition =
            if (numFavourites != 0 && numFavourites < position) {
                position - 1
            } else {
                position
            }

        val contact = contacts[relativePosition]
        holder.container.layoutParams.width = recyclerView.width
        holder.container.setOnClickListener({ listener.onContactClick(contact) })
        holder.container.setOnLongClickListener { listener.onContactLongClick(contact); true }
        holder.textView.text = contact.displayName
        if (contact.photoUri != null) {
            holder.imageView.visibility = View.INVISIBLE
            holder.imageViewCircle.visibility = View.VISIBLE
            Picasso.with(holder.imageViewCircle.context).load(contact.photoUri)
                .into(holder.imageViewCircle)
        } else {
            holder.imageView.visibility = View.VISIBLE
            holder.imageViewCircle.visibility = View.INVISIBLE
            val textDrawable = TextDrawable.builder()
                .buildRound(
                    contact.displayName.substring(0, 1),
                    colorGenerator.getColor(contact.id)
                )
            holder.imageView.setImageDrawable(textDrawable)
        }

        val knownDialers = contact.getDistinctPhoneNumbers()
            .mapNotNull { phoneNumber -> contactCaller.getAppInfoForContact(phoneNumber, contact) }
            .distinct()

        if (knownDialers.size == 1) {
            val icon = knownDialers.first().icon
            holder.appImageView.setImageDrawable(icon)
        } else {
            holder.appImageView.setImageResource(R.drawable.ic_call)
        }
        holder.appImageView.setOnClickListener { listener.onDialerAppIconClick(contact) }

        holder.itemView.background = null
        if (contact == selectedContact) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            val outValue = TypedValue()
            holder.itemView.context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true
            )
            holder.itemView.setBackgroundResource(outValue.resourceId)
        }
    }

    fun onContactsChanged() {
        numFavourites = contacts.count { contact -> contactManager.isFavourite(contact) }
        notifyDataSetChanged()
    }
}