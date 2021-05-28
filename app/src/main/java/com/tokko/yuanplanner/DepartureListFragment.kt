package com.tokko.yuanplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.squareup.okhttp.Callback
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import com.tokko.yuanplanner.databinding.DepartureitemBinding
import com.tokko.yuanplanner.databinding.FragmentFirstBinding
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import org.joda.time.DateTime
import java.io.IOException
import java.net.URLEncoder

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

data class SLResponse(val travels: List<Travel?>?, val searchLaterRef: String?)
data class Travel(val origin: Station?, val destination: Station?)
data class Station(val departureTime: DepartureTime?, val name: String?)
data class DepartureTime(val planned: String?, val realTime: String?)

class DepartureListFragment : Fragment() {

    val searchString =
        """https://webcloud.sl.se/api/travels?mode=travelPlanner&origId=1365&destId=9508&searchForArrival=false&transportTypes=111&desiredResults=3&origName=S%C3%B6dra+station+%28p%C3%A5+Rosenlundsg%29+%28Stockholm%29&destName=Ulriksdal+%28Solna%29&useCache=false"""
    private var _binding: FragmentFirstBinding? = null
    val adapter = GroupieAdapter()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding?.departureRecycler?.adapter = adapter
        _binding?.departureRecycler?.layoutManager = LinearLayoutManager(activity)
        _binding?.swiperefresh?.setOnRefreshListener {
            adapter.clear()
            loadData()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData(ref: String? = null) {
        val url =
            if (ref != null) "$searchString&scrollReference=${URLEncoder.encode(ref)}" else searchString
        OkHttpClient().newCall(Request.Builder().url(url).get().build()).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException?) {
                Toast.makeText(activity, "Failure", Toast.LENGTH_SHORT).show()
                activity?.runOnUiThread {
                    _binding?.swiperefresh?.isRefreshing = false
                }
            }

            override fun onResponse(response: Response?) {
                val payload = response?.body()?.string()
                val data = Gson().fromJson(payload, SLResponse::class.java)
                activity?.runOnUiThread {
                    adapter.addAll(data.travels?.filterNotNull()?.map { DepartureItem(it) }
                        ?: emptyList())
                    adapter.notifyDataSetChanged()
                    _binding?.swiperefresh?.isRefreshing = false

                    _binding?.loadMoreButton?.setOnClickListener {
                        loadData(data.searchLaterRef)
                    }
                }
            }


        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DepartureItem(val travel: Travel) : BindableItem<DepartureitemBinding>() {
    override fun bind(viewBinding: DepartureitemBinding, position: Int) {
        if (travel.origin?.departureTime?.realTime?.isNullOrBlank() == true) return
        val dt = DateTime.parse(
            travel.origin?.departureTime?.realTime ?: travel.origin?.departureTime?.planned
            ?: "Unknown time :("
        )
        viewBinding.departureTime.text =
            "${dt.hourOfDay.toStringPadZero()}:${dt.minuteOfHour.toStringPadZero()}"
        viewBinding.stationName.text = travel.origin?.name ?: "Unknown station"
        viewBinding.destinationStationName.text = travel.destination?.name ?: "Unknown station"
    }

    override fun getLayout() = R.layout.departureitem

    override fun initializeViewBinding(view: View) = DepartureitemBinding.bind(view)


}

fun Int.toStringPadZero(): String {
    if (this > 9) return toString()
    return "0${toString()}"
}