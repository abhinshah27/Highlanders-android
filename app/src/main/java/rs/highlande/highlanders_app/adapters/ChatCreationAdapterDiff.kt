/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.highlanders_app.R
import rs.highlande.highlanders_app.models.HLUserGeneric
import rs.highlande.highlanders_app.utility.Utils
import rs.highlande.highlanders_app.utility.helpers.MediaHelper
import rs.highlande.highlanders_app.voiceVideoCalls.tmp.VoiceVideoCallType


class ChatCreationAdapterDiff(diffUtilCallback: HLUserGenericDiffCallback, private val newChatCallback: NewChatCallback):
        ListAdapter<HLUserGeneric, ChatCreationAdapterDiff.ChatCreationVH>(diffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatCreationVH {
        return ChatCreationVH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_creation, parent, false))
    }

    override fun onBindViewHolder(holder: ChatCreationVH, position: Int) {
        holder.setItem(getItem(position))
    }

    override fun submitList(list: List<HLUserGeneric>?) {
        super.submitList(if (list != null) ArrayList(list) else null)
    }


    inner class ChatCreationVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var profilePicture: ImageView? = null
        private var userName: TextView? = null
        private var chatBtn: View? = null
        private var voiceBtn: View? = null
        private var videoBtn: View? = null

        private var currentUser: HLUserGeneric? = null


        init {
            with (itemView) {
                profilePicture = this.findViewById(R.id.profilePicture)
                userName = this.findViewById(R.id.userName)
                chatBtn = (this.findViewById(R.id.btnTextChat) as? View)?.apply {
                    this.setOnClickListener { newChatCallback.onChatClicked(currentUser) }
                }
                voiceBtn = (this.findViewById(R.id.btnVoice) as? View)?.apply {
                    this.setOnClickListener { newChatCallback.onCallClicked(currentUser, VoiceVideoCallType.VOICE) }
                }
                videoBtn = (this.findViewById(R.id.btnVideo) as? View)?.apply {
                    this.setOnClickListener { newChatCallback.onCallClicked(currentUser, VoiceVideoCallType.VIDEO) }
                }
            }
        }


        fun setItem(user: HLUserGeneric?) {

            with (user) {
                currentUser = user

                if (!this?.avatarURL.isNullOrBlank())
                    MediaHelper.loadProfilePictureWithPlaceholder(profilePicture?.context, this?.avatarURL, profilePicture)
                else
                    profilePicture?.setImageResource(R.drawable.ic_profile_placeholder)

                userName?.text = this?.completeName

                chatBtn?.visibility = if (this?.canChat() == true) View.VISIBLE else View.GONE
                voiceBtn?.visibility = if (this?.canAudiocall() == true) View.VISIBLE else View.GONE
                videoBtn?.visibility = if (this?.canVideocall() == true && Utils.hasDeviceCamera(videoBtn?.context))
                    View.VISIBLE else View.GONE

            }
        }
    }

    interface NewChatCallback {
        fun onChatClicked(user: HLUserGeneric?)
        fun onCallClicked(user: HLUserGeneric?, callType: VoiceVideoCallType)
    }

}


class HLUserGenericDiffCallback: DiffUtil.ItemCallback<HLUserGeneric>() {

    override fun areItemsTheSame(oldItem: HLUserGeneric, newItem: HLUserGeneric): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: HLUserGeneric, newItem: HLUserGeneric): Boolean {
        return (
                oldItem.avatarURL == newItem.avatarURL &&
                oldItem.completeName == newItem.completeName &&
                oldItem.canChat() == newItem.canChat() &&
                oldItem.canAudiocall() == newItem.canAudiocall() &&
                oldItem.canVideocall() == newItem.canVideocall()
                )
    }
}