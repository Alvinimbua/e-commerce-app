package com.project.bestladyapp.ui.home

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.project.bestladyapp.R
import com.project.bestladyapp.data.utils.StoreDataStatus
import com.project.bestladyapp.databinding.FragmentOrderSuccessBinding
import com.project.bestladyapp.viewModels.OrderViewModel

class OrderSuccessFragment : Fragment() {

	private lateinit var binding: FragmentOrderSuccessBinding
	private val orderViewModel: OrderViewModel by activityViewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentOrderSuccessBinding.inflate(layoutInflater)

		binding.loaderLayout.loaderCard.visibility = View.VISIBLE
		binding.loaderLayout.loadingMessage.text = getString(R.string.process_order_msg)
		binding.loaderLayout.circularLoader.showAnimationBehavior
		binding.orderConstraintGroup.visibility = View.GONE
		setObservers()
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.backToHomeBtn.setOnClickListener {
			findNavController().navigate(R.id.action_orderSuccessFragment_to_homeFragment)
		}
	}

	private fun setObservers() {
		orderViewModel.orderStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				StoreDataStatus.LOADING -> {
					binding.loaderLayout.loaderCard.visibility = View.VISIBLE
				}
				else -> {
					binding.orderConstraintGroup.visibility = View.VISIBLE
					binding.loaderLayout.loaderCard.visibility = View.GONE
					binding.redirectHomeTimerTv.text =
						getString(R.string.redirect_home_timer_text, "5")
					countDownTimer.start()
				}
			}
		}
	}

	private val countDownTimer = object : CountDownTimer(5000, 1000) {
		override fun onTick(millisUntilFinished: Long) {
			val sec = millisUntilFinished / 1000
			binding.redirectHomeTimerTv.text =
				getString(R.string.redirect_home_timer_text, sec.toString())
		}

		override fun onFinish() {
			findNavController().navigate(R.id.action_orderSuccessFragment_to_homeFragment)
		}
	}
}