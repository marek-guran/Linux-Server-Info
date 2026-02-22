package com.emgi2.serverinfo.ui.hardware

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emgi2.serverinfo.ApiAddressManager
import com.emgi2.serverinfo.R as R2
import com.emgi2.serverinfo.databinding.FragmentHardwareBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HardwareFragment : Fragment() {

    private var _binding: FragmentHardwareBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HardwareViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHardwareBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val cpuUsageTextView: TextView = binding.cpuUsage
        val cpuTempTextView: TextView = binding.cpuTemp
        val cpuClockTextView: TextView = binding.cpuSpeed
        val ramUsageTextView: TextView = binding.ramUsage
        val ramFreeTextView: TextView = binding.ramFree
        val ramTotalTextView: TextView = binding.ramTotal
        val storageContainer: LinearLayout = binding.storageContainer
        val networkContainer: LinearLayout = binding.networkContainer
        val cpuUsageProgressBar: ProgressBar = binding.cpuUsageProgress
        val cpuTempProgressBar: ProgressBar = binding.cpuTemperatureProgress
        val ramUsagePercent: TextView = binding.ramUsagePercent
        val ramUsageProgress: ProgressBar = binding.ramUsageProgress

        viewModel.jsonUrl = getApi()

        viewModel.jsonData.observe(viewLifecycleOwner) { jsonObject ->
            jsonObject?.let {
                updateTextViews(
                    it,
                    cpuUsageTextView,
                    cpuTempTextView,
                    cpuClockTextView,
                    ramUsageTextView,
                    ramFreeTextView,
                    ramTotalTextView,
                    cpuUsageProgressBar,
                    cpuTempProgressBar,
                    ramUsagePercent,
                    ramUsageProgress
                )
                createStorageViews(it, storageContainer)
                createNetworkViews(it, networkContainer)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getApi(): String {
        val apiAddressManager = ApiAddressManager(requireContext())
        return apiAddressManager.getApiAddress()
    }

    private fun updateTextViews(
        jsonObject: JSONObject,
        cpuUsageTextView: TextView,
        cpuTempTextView: TextView,
        cpuClockTextView: TextView,
        ramUsageTextView: TextView,
        ramFreeTextView: TextView,
        ramTotalTextView: TextView,
        cpuUsageProgressBar: ProgressBar,
        cpuTempProgressBar: ProgressBar,
        ramUsagePercent: TextView,
        ramUsageProgress: ProgressBar
    ) {
        try {
            val cpuInfo = jsonObject.getJSONObject("cpu")
            val cpuUsage = cpuInfo.getString("usage")
            val cpuUsageProgressValue = cpuUsage.split(".")[0].toIntOrNull() ?: 0
            val cpuTemp = cpuInfo.getString("temperature")
            val cpuTempProgressValue = cpuTemp.split(".")[0].toIntOrNull() ?: 0
            val cpuClock = cpuInfo.getString("speed")

            val ram = jsonObject.getJSONObject("ram")
            val ramUsage = ram.getString("used")
            val ramFree = ram.getString("free")
            val ramTotal = ram.getString("total")
            val ramPercent = ram.getString("usage_percent")
            val ramProgressValue = ramPercent.split("%")[0].toIntOrNull() ?: 0

            cpuUsageTextView.text = cpuUsage
            cpuUsageProgressBar.progress = cpuUsageProgressValue
            cpuTempProgressBar.progress = cpuTempProgressValue
            cpuTempTextView.text = "$cpuTemp℃"
            cpuClockTextView.text = "Speed: $cpuClock"
            ramFreeTextView.text = "Free: $ramFree"
            ramUsageTextView.text = "Used: $ramUsage"
            ramTotalTextView.text = "Total: $ramTotal"
            ramUsagePercent.text = ramPercent
            ramUsageProgress.progress = ramProgressValue
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNetworkViews(jsonObject: JSONObject, networkContainer: LinearLayout) {
        networkContainer.removeAllViews()

        val networkObject = jsonObject.getJSONObject("network")

        val networkNames = networkObject.keys()
        while (networkNames.hasNext()) {
            val name = networkNames.next()
            val networkData = networkObject.getJSONObject(name)

            val isUp = networkData.getBoolean("is_up")
            val speed = networkData.getString("speed")

            val networkNameTextView = TextView(requireContext())
            networkNameTextView.text = name
            networkNameTextView.setTextColor(Color.parseColor("#f2f2fb"))
            networkNameTextView.textSize = 16F

            val networkSpeedTextView = TextView(requireContext())
            val runningLabel = getString(R2.string.running)
            val speedLabel = getString(R2.string.speed)

            networkSpeedTextView.text = "$runningLabel $isUp\n$speedLabel $speed"
            networkSpeedTextView.setTextColor(Color.parseColor("#f2f2fb"))
            networkSpeedTextView.setPadding(0, 0, 0, 10)
            networkSpeedTextView.textSize = 16F

            networkContainer.addView(networkNameTextView)
            networkContainer.addView(networkSpeedTextView)
        }
    }

    private fun createStorageViews(jsonObject: JSONObject, storageContainer: LinearLayout) {
        storageContainer.removeAllViews()

        val storageArray = jsonObject.getJSONArray("storage")

        for (i in 0 until storageArray.length()) {
            val storageObject = storageArray.getJSONObject(i)
            val name = storageObject.getString("name")
            val usagePercent = storageObject.getString("usage_percent")
            val usedStorage = storageObject.getString("used")
            val totalStorage = storageObject.getString("size")
            val mountedStorage = storageObject.getString("mountpoint")
            val fstypeStorage = storageObject.getString("fstype")

            val mounted = getString(R2.string.mounted_at)
            val fs = getString(R2.string.fs)

            val storageNameTextView = TextView(requireContext())
            storageNameTextView.text = "$name \n$mounted $mountedStorage\n$fs $fstypeStorage"
            storageNameTextView.setTextColor(Color.parseColor("#f2f2fb"))
            storageNameTextView.textSize = 16F
            storageNameTextView.setPadding(0, 0, 0, 5)

            val progressBar = ProgressBar(
                requireContext(),
                null,
                android.R.attr.progressBarStyleHorizontal
            )
            progressBar.progress = usagePercent.replace("%", "").toFloat().toInt()
            progressBar.progressDrawable = ContextCompat.getDrawable(requireContext(), R2.drawable.custom_progress_bar)

            val progressBarHeightInPixels = 40
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                progressBarHeightInPixels
            )
            progressBar.layoutParams = layoutParams

            val storage1 = getString(R2.string.used_storage1)
            val storage2 = getString(R2.string.used_storage2)

            val progressBarTextView = TextView(requireContext())
            progressBarTextView.text = "$storage1 $usedStorage $storage2 $totalStorage"
            progressBarTextView.setTextColor(Color.parseColor("#f2f2fb"))
            progressBarTextView.textSize = 16F
            progressBarTextView.setPadding(0, 5, 0, 10)

            storageContainer.addView(storageNameTextView)
            storageContainer.addView(progressBar)
            storageContainer.addView(progressBarTextView)
        }
    }
}

class HardwareViewModel : ViewModel() {
    private val _jsonData = MutableLiveData<JSONObject?>()
    val jsonData: LiveData<JSONObject?> = _jsonData

    var jsonUrl: String = ""

    init {
        fetchDataPeriodically()
    }

    private fun fetchDataPeriodically() {
        viewModelScope.launch {
            fetchData() // Fetch data immediately
            while (true) {
                delay(5000)
                fetchData()
            }
        }
    }

    private suspend fun fetchData() {
        withContext(Dispatchers.IO) {
            val jsonObject = fetchJsonData(jsonUrl)
            _jsonData.postValue(jsonObject)
        }
    }

    private fun fetchJsonData(urlString: String): JSONObject? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                JSONObject(response)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}