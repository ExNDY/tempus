package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ItemHorizontalPlaylistDialogBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil

class PlaylistDialogHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PlaylistDialogHorizontalAdapter.ViewHolder>() {

    private var playlists: List<Playlist> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalPlaylistDialogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.bind(playlist)
    }

    override fun getItemCount(): Int = playlists.size

    fun setItems(playlists: List<Playlist>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Playlist = playlists[id]

    inner class ViewHolder(private val binding: ItemHorizontalPlaylistDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.playlistDialogTitleTextView.isSelected = true
            itemView.setOnClickListener { onClick() }
        }

        fun bind(playlist: Playlist) {
            binding.playlistDialogTitleTextView.text = playlist.name
            binding.playlistDialogCountTextView.text = itemView.context.getString(
                R.string.playlist_counted_tracks,
                playlist.songCount,
                MusicUtil.getReadableDurationString(playlist.duration, false)
            )
        }

        private fun onClick() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val bundle = Bundle().apply {
                    putSerializable(Constants.PLAYLIST_OBJECT, playlists[position])
                }
                click.onPlaylistClick(bundle)
            }
        }
    }
}
