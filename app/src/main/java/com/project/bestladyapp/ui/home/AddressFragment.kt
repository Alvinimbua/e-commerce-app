package com.project.bestladyapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.bestladyapp.R
import com.project.bestladyapp.data.utils.StoreDataStatus
import com.project.bestladyapp.databinding.FragmentAddressBinding
import com.project.bestladyapp.viewModels.HomeViewModel

private const val TAG = "AddressFragment"

class AddressFragment : Fragment() {
	private lateinit var binding: FragmentAddressBinding
	private lateinit var addressAdapter: AddressAdapter
	private val viewModel: HomeViewModel by activityViewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentAddressBinding.inflate(layoutInflater)
		setViews()
		setObservers()
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.getUserAddresses()
	}

	private fun setViews() {
		binding.addressAppBar.topAppBar.title = "Address"
		binding.addressAppBar.topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
		binding.addressAddBtn.visibility = View.GONE
		binding.addressAddBtn.setOnClickListener {
			navigateToAddEditAddress(false)
		}
		binding.addressEmptyTextView.visibility = View.GONE
		if (context != null) {
			val addressList = viewModel.userAddresses.value ?: emptyList()
			addressAdapter = AddressAdapter(requireContext(), addressList, false)
			addressAdapter.onClickListener = object : AddressAdapter.OnClickListener {
				override fun onEditClick(addressId: String) {
					Log.d(TAG, "onEditAddress: initiated")
					navigateToAddEditAddress(true, addressId)
				}

				override fun onDeleteClick(addressId: String) {
					Log.d(TAG, "onDeleteAddress: initiated")
					showDeleteDialog(addressId)
				}
			}
			binding.addressAddressesRecyclerView.adapter = addressAdapter
		}
	}

	private fun setObservers() {
		viewModel.dataStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				StoreDataStatus.LOADING -> {
					binding.addressEmptyTextView.visibility = View.GONE
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
					binding.loaderLayout.circularLoader.showAnimationBehavior
				}
				else -> {
					binding.addressAddBtn.visibility = View.VISIBLE
					binding.loaderLayout.circularLoader.hideAnimationBehavior
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
				}
			}

			if (status != null && status != StoreDataStatus.LOADING) {
				viewModel.userAddresses.observe(viewLifecycleOwner) { addressList ->
					if (addressList.isNotEmpty()) {
						addressAdapter.data = addressList
						binding.addressAddressesRecyclerView.adapter = addressAdapter
						binding.addressAddressesRecyclerView.adapter?.notifyDataSetChanged()
					} else if (addressList.isEmpty()) {
						binding.addressAddressesRecyclerView.visibility = View.GONE
						binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
						binding.loaderLayout.circularLoader.hideAnimationBehavior
						binding.addressEmptyTextView.visibility = View.VISIBLE
					}
				}
				binding.addressAddBtn.visibility = View.VISIBLE
			}
		}
	}

	private fun showDeleteDialog(addressId: String) {
		context?.let {
			MaterialAlertDialogBuilder(it)
				.setTitle(getString(R.string.delete_dialog_title_text))
				.setMessage(getString(R.string.delete_address_message_text))
				.setNeutralButton(getString(R.string.pro_cat_dialog_cancel_btn)) { dialog, _ ->
					dialog.cancel()
				}
				.setPositiveButton(getString(R.string.delete_dialog_delete_btn_text)) { dialog, _ ->
					viewModel.deleteAddress(addressId)
					dialog.cancel()
				}
				.show()
		}
	}

	private fun navigateToAddEditAddress(isEdit: Boolean, addressId: String? = null) {
		findNavController().navigate(
			R.id.action_addressFragment_to_addEditAddressFragment,
			bundleOf("isEdit" to isEdit, "addressId" to addressId)
		)
	}
}