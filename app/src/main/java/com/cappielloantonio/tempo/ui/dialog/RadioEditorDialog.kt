package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogRadioEditorBinding
import com.cappielloantonio.tempo.databinding.ItemRadioSearchResultBinding
import com.cappielloantonio.tempo.databinding.PopupRadioSearchBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.interfaces.RadioCallback
import com.cappielloantonio.tempo.radiobrowser.RadioBrowserCountry
import com.cappielloantonio.tempo.radiobrowser.RadioBrowserRepository
import com.cappielloantonio.tempo.radiobrowser.RadioBrowserStation
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.RadioEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*

@UnstableApi
class RadioEditorDialog(private val radioCallback: RadioCallback?) : DialogFragment() {
    private var bind: DialogRadioEditorBinding? = null
    private lateinit var radioEditorViewModel: RadioEditorViewModel

    private var radioName: String? = null
    private var radioStreamURL: String? = null
    private var radioHomepageURL: String? = null
    private var radioCoverArtUrl: String? = null

    private var searchPopup: PopupWindow? = null
    private var popupBind: PopupRadioSearchBinding? = null
    private val countryNames = mutableListOf<String>()
    private var searchAdapter: SearchResultsAdapter? = null
    private var countryListAdapter: ArrayAdapter<String>? = null
    private var suppressCountryFilter = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogRadioEditorBinding.inflate(layoutInflater)
        radioEditorViewModel = ViewModelProvider(this).get(RadioEditorViewModel::class.java)

        arguments?.getSerializable(Constants.INTERNET_RADIO_STATION_OBJECT)?.let {
            radioEditorViewModel.setRadioToEdit(it as com.cappielloantonio.tempo.subsonic.models.InternetRadioStation)
        }

        setupObservers()
        setupSourceToggle()
        setupSearchButton()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind!!.root)
            .setTitle(R.string.radio_editor_dialog_title)
            .setPositiveButton(R.string.radio_editor_dialog_positive_button) { _, _ -> }
            .setNeutralButton(R.string.radio_editor_dialog_neutral_button) { _, _ ->
                radioEditorViewModel.deleteRadio()
            }
            .setNegativeButton(R.string.radio_editor_dialog_negative_button) { dialog, _ ->
                dialog.cancel()
            }
            .create()
    }

    private fun setupSourceToggle() {
        if (radioEditorViewModel.isEditing()) {
            bind?.sourceToggleLayout?.visibility = View.GONE
            bind?.searchDirectoryButton?.visibility = View.GONE
        } else {
            bind?.sourceToggleLayout?.visibility = View.VISIBLE
            bind?.searchDirectoryButton?.visibility = View.VISIBLE
            bind?.radioSourceToggle?.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val isLocal = checkedId == R.id.radio_source_local
                    radioEditorViewModel.setLocal(isLocal)
                    bind?.internetRadioStationCoverArtUrlLayout?.visibility = if (isLocal) View.VISIBLE else View.GONE
                }
            }
            bind?.radioSourceToggle?.check(R.id.radio_source_server)
        }
    }

    private fun setupSearchButton() {
        bind?.searchDirectoryButton?.setOnClickListener {
            if (searchPopup?.isShowing == true) {
                searchPopup?.dismiss()
                return@setOnClickListener
            }
            showSearchPopup()
        }
    }

    private fun showSearchPopup() {
        val pb = PopupRadioSearchBinding.inflate(layoutInflater)
        popupBind = pb

        searchAdapter = SearchResultsAdapter { station ->
            fillFromSearchResult(station)
            searchPopup?.dismiss()
        }
        pb.popupResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        pb.popupResultsRecycler.adapter = searchAdapter

        pb.popupSearchName.setText(bind?.internetRadioStationNameTextView?.text)

        setupPopupCountryDropdown()

        pb.popupCancelButton.setOnClickListener { searchPopup?.dismiss() }

        pb.popupSearchButton.setOnClickListener {
            val query = pb.popupSearchName.text?.toString()?.trim() ?: ""
            val country = pb.popupCountryDropdown.text?.toString()?.trim() ?: ""
            if (query.isEmpty() && country.isEmpty()) {
                pb.popupSearchName.error = getString(R.string.radio_search_empty_query)
                return@setOnClickListener
            }
            hideCountryList()
            performSearch(query, country)
        }

        val windowRect = Rect()
        requireActivity().window.decorView.getWindowVisibleDisplayFrame(windowRect)
        val dp16 = (16 * resources.displayMetrics.density).toInt()
        val popupWidth = windowRect.width() - dp16 * 2
        val maxPopupHeight = (windowRect.height() * 0.6).toInt()

        searchPopup = PopupWindow(pb.root, popupWidth, maxPopupHeight, true).apply {
            isFocusable = true
            elevation = 16f
            inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
            isClippingEnabled = true
            setOnDismissListener { searchPopup = null }
            showAtLocation(bind!!.root, Gravity.CENTER, 0, 0)
        }

        val initialQuery = pb.popupSearchName.text?.toString()?.trim() ?: ""
        if (initialQuery.isNotEmpty()) {
            performSearch(initialQuery, "")
        }
    }

    private fun setupPopupCountryDropdown() {
        val pb = popupBind ?: return
        countryListAdapter = ArrayAdapter(requireContext(), R.layout.item_country_dropdown)
        pb.popupCountryList.adapter = countryListAdapter

        pb.popupCountryList.setOnItemClickListener { _, _, position, _ ->
            val selected = countryListAdapter?.getItem(position)
            suppressCountryFilter = true
            pb.popupCountryDropdown.setText(selected)
            pb.popupCountryDropdown.setSelection(selected?.length ?: 0)
            suppressCountryFilter = false
            hideCountryList()
        }

        pb.popupCountryDropdown.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateCountryEndIcon()
                if (suppressCountryFilter || popupBind == null) return
                showCountryList(s?.toString() ?: "")
            }
        })

        pb.popupCountryDropdown.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showCountryList(pb.popupCountryDropdown.text?.toString() ?: "")
            }
        }

        pb.popupCountryLayout.setEndIconOnClickListener {
            val text = pb.popupCountryDropdown.text
            if (!text.isNullOrEmpty()) {
                pb.popupCountryDropdown.setText("")
                pb.popupCountryDropdown.requestFocus()
            } else if (pb.popupCountryList.visibility == View.VISIBLE) {
                hideCountryList()
            } else {
                pb.popupCountryDropdown.requestFocus()
                showCountryList("")
            }
        }
        updateCountryEndIcon()

        pb.popupSearchName.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) hideCountryList()
        }

        if (countryNames.isNotEmpty()) return

        lifecycleScope.launch {
            val countries = App.get(RadioBrowserRepository::class.java).getCountries()
            if (!isAdded || popupBind == null) return@launch
            countryNames.clear()
            countries.forEach { if (it.stationCount > 0) countryNames.add(it.name) }
            countryNames.sortWith(String.CASE_INSENSITIVE_ORDER)
            if (pb.popupCountryList.visibility == View.VISIBLE) {
                showCountryList(pb.popupCountryDropdown.text?.toString() ?: "")
            }
        }
    }

    private fun showCountryList(query: String) {
        val pb = popupBind ?: return
        val q = query.trim().lowercase()
        val filtered = countryNames.filter { q.isEmpty() || it.lowercase().contains(q) }
        countryListAdapter?.apply {
            clear()
            addAll(filtered)
            notifyDataSetChanged()
        }
        positionCountryList()
        pb.popupCountryList.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun positionCountryList() {
        val pb = popupBind ?: return
        val parent = pb.popupCountryList.parent as View
        val parentLocation = IntArray(2)
        parent.getLocationOnScreen(parentLocation)
        val fieldLocation = IntArray(2)
        pb.popupCountryLayout.getLocationOnScreen(fieldLocation)

        val dp16 = (16 * resources.displayMetrics.density).toInt()
        val params = pb.popupCountryList.layoutParams as FrameLayout.LayoutParams
        params.width = FrameLayout.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = (fieldLocation[0] - parentLocation[0]).coerceAtLeast(0)
        params.rightMargin = dp16
        pb.popupCountryList.layoutParams = params
    }

    private fun hideCountryList() {
        popupBind?.popupCountryList?.visibility = View.GONE
    }

    private fun updateCountryEndIcon() {
        val pb = popupBind ?: return
        val hasText = !pb.popupCountryDropdown.text.isNullOrEmpty()
        pb.popupCountryLayout.setEndIconDrawable(if (hasText) R.drawable.ic_close else R.drawable.ic_expand_more)
    }

    private fun performSearch(query: String, country: String) {
        val pb = popupBind ?: return
        if (searchAdapter?.itemCount == 0) {
            pb.popupProgressBar.visibility = View.VISIBLE
            pb.popupEmptyResults.visibility = View.GONE
        }

        lifecycleScope.launch {
            val stations = if (query.isNotEmpty() && country.isNotEmpty()) {
                App.get(RadioBrowserRepository::class.java).searchAdvanced(query, country, null, null)
            } else if (country.isNotEmpty()) {
                App.get(RadioBrowserRepository::class.java).searchByCountryExact(country)
            } else {
                App.get(RadioBrowserRepository::class.java).searchByName(query)
            }

            if (!isAdded || popupBind == null) return@launch
            pb.popupProgressBar.visibility = View.GONE
            if (stations.isNotEmpty()) {
                searchAdapter?.setItems(stations)
                pb.popupResultsRecycler.visibility = View.VISIBLE
                pb.popupEmptyResults.visibility = View.GONE
            } else {
                searchAdapter?.setItems(emptyList())
                pb.popupResultsRecycler.visibility = View.GONE
                pb.popupEmptyResults.visibility = View.VISIBLE
            }
        }
    }

    private fun fillFromSearchResult(station: RadioBrowserStation) {
        val streamUrl = station.urlResolved ?: station.url
        bind?.internetRadioStationNameTextView?.setText(station.name)
        bind?.internetRadioStationStreamUrlTextView?.setText(streamUrl)
        bind?.internetRadioStationHomepageUrlTextView?.setText(station.homepage)

        val coverArtUrl = if (!station.favicon.isNullOrEmpty()) station.favicon else null
        bind?.internetRadioStationCoverArtUrlTextView?.setText(coverArtUrl)
    }

    private fun setupObservers() {
        radioEditorViewModel.isSuccess.observe(this) { isSuccess ->
            if (isSuccess == true) {
                Toast.makeText(requireContext(),
                    if (radioEditorViewModel.radioToEdit == null)
                        App.getContext().getString(R.string.radio_editor_dialog_added) else App.getContext().getString(R.string.radio_editor_dialog_updated),
                    Toast.LENGTH_SHORT).show()
                dismissDialog()
            }
        }
        radioEditorViewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                radioEditorViewModel.clearError()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setParameterInfo()

        (dialog as? androidx.appcompat.app.AlertDialog)?.getButton(Dialog.BUTTON_POSITIVE)?.setOnClickListener {
            performSave()
        }
    }

    private fun performSave() {
        if (!validateInput()) return

        if (radioEditorViewModel.radioToEdit == null) {
            radioEditorViewModel.createRadio(radioName!!, radioStreamURL!!,
                if (radioHomepageURL!!.isEmpty()) null else radioHomepageURL,
                if (radioCoverArtUrl!!.isEmpty()) null else radioCoverArtUrl)
        } else {
            radioEditorViewModel.updateRadio(radioName!!, radioStreamURL!!,
                if (radioHomepageURL!!.isEmpty()) null else radioHomepageURL,
                if (radioCoverArtUrl!!.isEmpty()) null else radioCoverArtUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchPopup?.dismiss()
        bind = null
    }

    private fun setParameterInfo() {
        val toEdit = radioEditorViewModel.radioToEdit
        if (toEdit != null) {
            bind?.internetRadioStationNameTextView?.setText(toEdit.name)
            bind?.internetRadioStationStreamUrlTextView?.setText(toEdit.streamUrl)
            bind?.internetRadioStationHomepageUrlTextView?.setText(toEdit.homePageUrl)

            bind?.sourceToggleLayout?.visibility = View.GONE
            bind?.searchDirectoryButton?.visibility = View.GONE

            if (radioEditorViewModel.isLocal) {
                bind?.internetRadioStationCoverArtUrlLayout?.visibility = View.VISIBLE
                bind?.internetRadioStationCoverArtUrlTextView?.setText(toEdit.coverArt)
            }

            (dialog as? androidx.appcompat.app.AlertDialog)?.getButton(Dialog.BUTTON_NEUTRAL)?.visibility = View.VISIBLE
        }
    }

    private fun validateInput(): Boolean {
        radioName = bind?.internetRadioStationNameTextView?.text?.toString()?.trim()
        radioStreamURL = bind?.internetRadioStationStreamUrlTextView?.text?.toString()?.trim()
        radioHomepageURL = bind?.internetRadioStationHomepageUrlTextView?.text?.toString()?.trim()
        radioCoverArtUrl = bind?.internetRadioStationCoverArtUrlTextView?.text?.toString()?.trim() ?: ""

        if (TextUtils.isEmpty(radioName)) {
            bind?.internetRadioStationNameTextView?.error = getString(R.string.error_required)
            return false
        }
        if (TextUtils.isEmpty(radioStreamURL)) {
            bind?.internetRadioStationStreamUrlTextView?.error = getString(R.string.error_required)
            return false
        }
        return true
    }

    private fun dismissDialog() {
        radioCallback?.onDismiss()
        dialog?.dismiss()
    }

    private class SearchResultsAdapter(private val listener: (RadioBrowserStation) -> Unit) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {
        private var stations = emptyList<RadioBrowserStation>()

        fun setItems(items: List<RadioBrowserStation>) {
            stations = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRadioSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val station = stations[position]
            holder.binding.stationNameTextView.text = station.name

            val details = mutableListOf<String>()
            if (!station.country.isNullOrEmpty()) details.add(station.country!!)
            if (!station.codec.isNullOrEmpty()) details.add(station.codec!!)
            if (station.bitrate > 0) details.add("${station.bitrate} kbps")
            holder.binding.stationDetailsTextView.text = details.joinToString(" · ")

            val coverUrl = station.favicon
            if (!coverUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(coverUrl)
                    .apply(CustomGlideRequest.createRequestOptions(holder.itemView.context, coverUrl, CustomGlideRequest.ResourceType.Radio))
                    .into(holder.binding.stationCoverImageView)
            } else {
                holder.binding.stationCoverImageView.setImageDrawable(null)
            }

            holder.binding.addStationButton.setText(R.string.radio_search_use)
            holder.binding.addStationButton.setOnClickListener { listener(station) }
        }

        override fun getItemCount(): Int = stations.size

        class ViewHolder(val binding: ItemRadioSearchResultBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
