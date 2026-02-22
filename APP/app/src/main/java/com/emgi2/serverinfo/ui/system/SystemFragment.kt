package com.emgi2.serverinfo.ui.system

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.emgi2.serverinfo.ApiAddressManager
import com.emgi2.serverinfo.databinding.FragmentSystemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.emgi2.serverinfo.R as R2

class SystemFragment : Fragment() {

    private var _binding: FragmentSystemBinding? = null
    private val binding get() = _binding!!

    private fun getApi(): String {
        val apiAddressManager = ApiAddressManager(requireContext())
        return apiAddressManager.getApiAddress()
    }

    private val jsonUrl: String
        get() = getApi()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch JSON data initially
        fetchDataAndUpdateViews()

        // Schedule periodic updates every second
        schedulePeriodicUpdates()
    }

    private fun fetchDataAndUpdateViews() {
        viewLifecycleOwner.lifecycleScope.launch {
            val jsonObject = fetchJsonData(jsonUrl)
            updateViews(jsonObject)
        }
    }

    private fun schedulePeriodicUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                fetchDataAndUpdateViews()
                delay(1000) // Update every 1 second
            }
        }
    }

    private suspend fun fetchJsonData(urlString: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                try {
                    val inputStream: InputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }

                    JSONObject(stringBuilder.toString())
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateViews(jsonObject: JSONObject?) {
        val localBinding = _binding
        if (localBinding == null || jsonObject == null) {
            return
        }

        with(localBinding) {
            jsonObject?.let { json ->
                // Parse OS information
                val os = json.getJSONObject("os")
                val distribution = os.getString("distribution")
                val kernel = os.getString("kernel_version")
                val uptimeInSeconds = os.getString("uptime")

                // Parse CPU information
                val cpu = json.getJSONObject("cpu")
                val coresJson = cpu.getJSONObject("cores")
                var coreCount = 0
                val keys = coresJson.keys()
                val cpuName = cpu.getString("hardware")
                val cpuArchitecture = cpu.getString("architecture")
                val cpuArchitectureType = cpu.getString("architecture_type")
                val cpuType = cpu.getString("type")
                val cpuCores = cpu.getString("cores")

                // Parse network information
                val networkJson = json.getJSONObject("network")
                var networkDeviceCount = 0
                val networkkeys = networkJson.keys()

                while (networkkeys.hasNext()) {
                    networkkeys.next() // Move to the next key
                    networkDeviceCount++
                }

                val networkDevicesText = getString(R2.string.network_devices)
                val networkDevicesDisplay = "$networkDevicesText $networkDeviceCount"
                NetworkDevices.text = networkDevicesDisplay

                // Update UI components with the parsed data
                if (!TextUtils.isEmpty(distribution)) {
                    Distribution.text = "${getString(R2.string.distribution)} $distribution"
                }
                if (!TextUtils.isEmpty(kernel)) {
                    Kernel.text = "${getString(R2.string.kernel)} $kernel"
                }
                if (!TextUtils.isEmpty(uptimeInSeconds)) {
                    val uptimeSplit = uptimeInSeconds.split(".")
                    val uptimeInSecondsInt = uptimeSplit[0].toLong()
                    val uptimeFraction = if (uptimeSplit.size > 1) uptimeSplit[1].substring(0, 2).toLong() else 0L

                    val timestamp = uptimeInSecondsInt * 1000 + uptimeFraction // Convert to milliseconds
                    val currentTimeMillis = System.currentTimeMillis()
                    val uptimeMillis = currentTimeMillis - timestamp

                    val days = TimeUnit.MILLISECONDS.toDays(uptimeMillis)
                    val hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis) % 24
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis) % 60

                    val formattedUptime = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)
                    Uptime.text = "${getString(R2.string.uptime)} $formattedUptime"
                }
                if (!TextUtils.isEmpty(cpuName)) {
                    CpuName.text = "${getString(R2.string.cpu_sys)} $cpuName"
                }
                if (!TextUtils.isEmpty(cpuArchitecture)) {
                    CpuArchitecture.text = "${getString(R2.string.architecture)} $cpuArchitectureType - $cpuArchitecture"
                }
                if (!TextUtils.isEmpty(cpuType)) {
                    CpuType.text = "${getString(R2.string.type)} $cpuType"
                }
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.startsWith("core_")) {
                        coreCount++
                    }
                }

                CpuCores.text = "${getString(R2.string.cores)} $coreCount"

                val distributionImageResource = when {
                    distribution.lowercase(Locale.ROOT)
                        .contains("ubuntu") -> R2.drawable.sys_ubuntu

                    distribution.lowercase(Locale.ROOT)
                        .contains("debian") -> R2.drawable.sys_debian

                    distribution.lowercase(Locale.ROOT)
                        .contains("raspbian") -> R2.drawable.sys_raspbian

                    distribution.lowercase(Locale.ROOT)
                        .contains("raspberry") -> R2.drawable.sys_raspbian

                    else -> R2.drawable.sys_default
                }
                binding.distributionImage.setImageResource(distributionImageResource)

                // Set CPU image based on CPU name
                val cpuImageResource = when {
                    cpuName.lowercase(Locale.ROOT).contains("amd") -> R2.drawable.amd
                    cpuName.lowercase(Locale.ROOT).contains("intel") -> R2.drawable.intel
                    cpuName.lowercase(Locale.ROOT).contains("bcm") -> R2.drawable.broadcom
                    else -> R2.drawable.sys_cpu
                }
                binding.cpuImage.setImageResource(cpuImageResource)
            }
        }
    }
}