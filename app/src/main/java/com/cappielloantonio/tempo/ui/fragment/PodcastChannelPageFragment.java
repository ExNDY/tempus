package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentPodcastChannelPageBinding;
import com.cappielloantonio.tempo.interfaces.ClickCallback;
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel;
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.adapter.PodcastEpisodeAdapter;
import com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog.PodcastEpisodeBottomSheetDialog;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.util.UIUtil;
import com.cappielloantonio.tempo.viewmodel.PodcastChannelPageViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UnstableApi
public class PodcastChannelPageFragment extends Fragment implements ClickCallback {
    private FragmentPodcastChannelPageBinding bind;
    private MainActivity activity;
    private PodcastChannelPageViewModel podcastChannelPageViewModel;
    private PodcastEpisodeAdapter podcastEpisodeAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        bind = FragmentPodcastChannelPageBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        podcastChannelPageViewModel = new ViewModelProvider(requireActivity()).get(PodcastChannelPageViewModel.class);

        Bundle args = getArguments();
        PodcastChannel channel = args != null ? (PodcastChannel) args.getSerializable(Constants.PODCAST_OBJECT) : null;

        init(channel);
        initAppBar();
        initPodcastChannelInfo();
        initPodcastChannelEpisodesView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init(PodcastChannel channel) {
        podcastChannelPageViewModel.setPodcastChannel(channel);
    }

    private void initAppBar() {
        activity.setSupportActionBar(bind.toolbar);

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (podcastChannelPageViewModel.getPodcastChannel().getValue() != null) {
            bind.toolbar.setTitle(podcastChannelPageViewModel.getPodcastChannel().getValue().getTitle());
        }
        bind.toolbar.setNavigationOnClickListener(v -> activity.navController.navigateUp());
    }

    private void initPodcastChannelInfo() {
        String normalizePodcastChannelDescription = "";
        if (podcastChannelPageViewModel.getPodcastChannel().getValue() != null) {
            normalizePodcastChannelDescription = MusicUtil.forceReadableString(podcastChannelPageViewModel.getPodcastChannel().getValue().getDescription());
        }

        if (bind != null) {
            bind.podcastChannelDescriptionTextView.setVisibility(!normalizePodcastChannelDescription.trim().isEmpty() ? View.VISIBLE : View.GONE);
            bind.podcastChannelDescriptionTextView.setText(normalizePodcastChannelDescription);
            bind.podcastEpisodesFilterImageView.setOnClickListener(view -> showPopupMenu(view, R.menu.filter_podcast_episode_popup_menu));
        }
    }

    private void initPodcastChannelEpisodesView() {
        bind.podcastEpisodesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.podcastEpisodesRecyclerView.addItemDecoration(UIUtil.getDividerItemDecoration(requireContext()));

        podcastEpisodeAdapter = new PodcastEpisodeAdapter(this);
        bind.podcastEpisodesRecyclerView.setAdapter(podcastEpisodeAdapter);

        podcastChannelPageViewModel.getPodcastChannelEpisodes().observe(getViewLifecycleOwner(), episodes -> {
            if (episodes != null) {
                if (bind != null) bind.podcastEpisodesRecyclerView.setVisibility(View.VISIBLE);
                if (!episodes.isEmpty()) {
                    List<PodcastEpisode> availableEpisode = new ArrayList<>(episodes);
                    Collections.sort(availableEpisode, (p1, p2) -> {
                        if (p1.getPublishDate() == null || p2.getPublishDate() == null)
                            return 0;
                        return p2.getPublishDate().compareTo(p1.getPublishDate());
                    });
                    podcastEpisodeAdapter.setItems(availableEpisode);
                }
            } else {
                if (bind != null) bind.podcastEpisodesRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void showPopupMenu(View view, int menuRes) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(menuRes, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_podcast_filter_all) {
                podcastEpisodeAdapter.sort(Constants.PODCAST_FILTER_BY_ALL);
                return true;
            } else if (id == R.id.menu_podcast_filter_download) {
                podcastEpisodeAdapter.sort(Constants.PODCAST_FILTER_BY_DOWNLOAD);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public void onMediaClick(Bundle bundle) {
        // Handle episode click
    }

    @Override
    public void onMediaLongClick(Bundle bundle) {
        PodcastEpisode episode = (PodcastEpisode) bundle.getSerializable(Constants.PODCAST_OBJECT);
        if (episode != null) {
            PodcastEpisodeBottomSheetDialog bottomSheet = new PodcastEpisodeBottomSheetDialog();
            bottomSheet.setArguments(bundle);
            bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
        }
    }

    @Override
    public void onAlbumClick(Bundle bundle) {}

    @Override
    public void onAlbumLongClick(Bundle bundle) {}

    @Override
    public void onArtistClick(Bundle bundle) {}

    @Override
    public void onArtistLongClick(Bundle bundle) {}
}
