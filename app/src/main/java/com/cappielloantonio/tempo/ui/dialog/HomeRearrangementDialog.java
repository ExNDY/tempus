package com.cappielloantonio.tempo.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.model.HomeSector;
import com.cappielloantonio.tempo.databinding.DialogHomeRearrangementBinding;
import com.cappielloantonio.tempo.ui.adapter.HomeSectorHorizontalAdapter;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.viewmodel.HomeRearrangementViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class HomeRearrangementDialog extends DialogFragment {
    private DialogHomeRearrangementBinding bind;
    private HomeRearrangementViewModel homeRearrangementViewModel;
    private HomeSectorHorizontalAdapter homeSectorHorizontalAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        bind = DialogHomeRearrangementBinding.inflate(getLayoutInflater());

        homeRearrangementViewModel = new ViewModelProvider(requireActivity()).get(HomeRearrangementViewModel.class);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(bind.getRoot())
                .setTitle(R.string.home_rearrangement_dialog_title)
                .setPositiveButton(R.string.home_rearrangement_dialog_positive_button, (dialog, id) -> { })
                .setNeutralButton(R.string.home_rearrangement_dialog_neutral_button, (dialog, id) -> { })
                .setNegativeButton(R.string.home_rearrangement_dialog_negative_button, (dialog, id) -> dialog.cancel())
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();

        setButtonAction();
        initSectorView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        homeRearrangementViewModel.closeDialog();
        bind = null;
    }

    private void setButtonAction() {
        androidx.appcompat.app.AlertDialog alertDialog = (androidx.appcompat.app.AlertDialog) Objects.requireNonNull(getDialog());

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            homeRearrangementViewModel.saveHomeSectorList(homeSectorHorizontalAdapter.getItems());
            dismiss();
        });

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            homeRearrangementViewModel.resetHomeSectorList();
            dismiss();
        });
    }

    private void initSectorView() {
        bind.homeSectorItemRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.homeSectorItemRecyclerView.setHasFixedSize(true);

        homeSectorHorizontalAdapter = new HomeSectorHorizontalAdapter();
        bind.homeSectorItemRecyclerView.setAdapter(homeSectorHorizontalAdapter);
        homeSectorHorizontalAdapter.setItems(homeRearrangementViewModel.getHomeSectorList(fillStandardHomeSectorList()));

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            int originalPosition = -1;
            int fromPosition = -1;
            int toPosition = -1;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (originalPosition == -1) originalPosition = viewHolder.getBindingAdapterPosition();

                fromPosition = viewHolder.getBindingAdapterPosition();
                toPosition = target.getBindingAdapterPosition();

                Collections.swap(homeSectorHorizontalAdapter.getItems(), fromPosition, toPosition);
                Objects.requireNonNull(recyclerView.getAdapter()).notifyItemMoved(fromPosition, toPosition);

                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                homeRearrangementViewModel.orderSectorLiveListAfterSwap(homeSectorHorizontalAdapter.getItems());

                originalPosition = -1;
                fromPosition = -1;
                toPosition = -1;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        }
        ).attachToRecyclerView(bind.homeSectorItemRecyclerView);
    }

    private ArrayList<HomeSector> fillStandardHomeSectorList() {
        ArrayList<HomeSector> sectors = new ArrayList<>();

        sectors.add(new HomeSector(Constants.HOME_SECTOR_DISCOVERY, requireContext().getString(R.string.home_title_discovery), true, 1));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_MADE_FOR_YOU, requireContext().getString(R.string.home_title_made_for_you), true, 2));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_BEST_OF, requireContext().getString(R.string.home_title_best_of), true, 3));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_RADIO_STATION, requireContext().getString(R.string.home_title_radio_station), true, 4));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_TOP_SONGS, requireContext().getString(R.string.home_title_top_songs), true, 5));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_STARRED_TRACKS, requireContext().getString(R.string.home_title_starred_tracks), true, 6));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_STARRED_ALBUMS, requireContext().getString(R.string.home_title_starred_albums), true, 7));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_STARRED_ARTISTS, requireContext().getString(R.string.home_title_starred_artists), true, 8));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_NEW_RELEASES, requireContext().getString(R.string.home_title_new_releases), true, 9));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_FLASHBACK, requireContext().getString(R.string.home_title_flashback), true, 10));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_MOST_PLAYED, requireContext().getString(R.string.home_title_most_played), true, 11));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_LAST_PLAYED, requireContext().getString(R.string.home_title_last_played), true, 12));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_RECENTLY_ADDED, requireContext().getString(R.string.home_title_recently_added), true, 13));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_PINNED_PLAYLISTS, requireContext().getString(R.string.home_title_pinned_playlists), true, 14));
        sectors.add(new HomeSector(Constants.HOME_SECTOR_SHARED, requireContext().getString(R.string.home_title_shares), true, 15));

        return sectors;
    }
}
