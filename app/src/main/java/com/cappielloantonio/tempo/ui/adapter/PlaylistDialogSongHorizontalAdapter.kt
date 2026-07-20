package com.cappielloantonio.tempo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHorizontalPlaylistDialogTrackBinding
import com.cappielloantonio.tempo.image.CoilImageRequest
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.MusicUtil

class PlaylistDialogSongHorizontalAdapter :
    RecyclerView.Adapter<PlaylistDialogSongHorizontalAdapter.ViewHolder>() {

    private var songs: List<Child> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalPlaylistDialogTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
    }

    override fun getItemCount(): Int = songs.size

    fun getItems(): List<Child> = songs

    fun setItems(songs: List<Child>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Child = songs[id]

    class ViewHolder(private val binding: ItemHorizontalPlaylistDialogTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.playlistDialogSongTitleTextView.isSelected = true
        }

        fun bind(song: Child) {
            binding.playlistDialogSongTitleTextView.text = song.title
            binding.playlistDialogAlbumArtistTextView.text = song.artist
            binding.playlistDialogSongDurationTextView.text =
                MusicUtil.getReadableDurationString(song.duration?.toLong() ?: 0L, false)

            CoilImageRequest.Builder
                .from(
                    itemView.context,
                    song.coverArtId ?: "",
                    CoilImageRequest.ResourceType.Song
                )
                .build()
                .into(binding.playlistDialogSongCoverImageView)
        }
    }
}
