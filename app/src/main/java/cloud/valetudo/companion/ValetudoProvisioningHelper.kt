package cloud.valetudo.companion

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLConnection


class ValetudoProvisioningHelper(
    var wifiManager: WifiManager,
    var connectivityManager: ConnectivityManager
) {
    fun checkForValetudo(): DiscoveredUnprovisionedValetudoInstance? {
        var discoveredInstance: DiscoveredUnprovisionedValetudoInstance? = null
        val wifiNetwork = this.getRobotWifiNetwork()

        if (wifiNetwork != null) {
            try {
                val valetudoVersionConnection =
                    wifiNetwork.openConnection(URL("http://${this.gatewayIp}/api/v2/valetudo/version"))
                valetudoVersionConnection.connect()

                val valetudoVersionJSON = this.getJSON(valetudoVersionConnection)


                val valetudoRobotInfoConnection =
                    wifiNetwork.openConnection(URL("http://${this.gatewayIp}/api/v2/robot"))
                valetudoRobotInfoConnection.connect()

                val valetudoRobotJSON = this.getJSON(valetudoRobotInfoConnection)


                Log.d("ValetudoVersion", valetudoVersionJSON.toString())
                Log.d("RobotInfo", valetudoRobotJSON.toString())

                discoveredInstance = DiscoveredUnprovisionedValetudoInstance(
                    valetudoRobotJSON.getString("modelName"),
                    valetudoRobotJSON.getString("manufacturer"),
                    valetudoVersionJSON.getString("release"),
                    this.gatewayIp ?: ""
                )
            } catch (ex: Exception) {
                Log.e("checkForValetudo", ex.toString())
            }
        } else {
            Log.w("checkForValetudo", "Couldn't find wifi network")
        }

        return discoveredInstance
    }

    fun provisionValetudo(ssid: String, password: String): Int {
        val wifiNetwork = this.getRobotWifiNetwork()
        var result: Int = -1

        if (wifiNetwork != null) {
            try {
                val connection =
                    wifiNetwork.openConnection(URL("http://${this.gatewayIp}/api/v2/robot/capabilities/WifiConfigurationCapability")) as HttpURLConnection
                val payload =
                    "{\"ssid\":\"$ssid\",\"credentials\":{\"type\":\"wpa2_psk\",\"typeSpecificSettings\":{\"password\":\"$password\"}}}".toByteArray()

                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "*/*")
                connection.doOutput = true

                connection.outputStream.write(payload, 0, payload.size)

                result = connection.responseCode
            } catch (ex: Exception) {
                Log.e("provisionValetudo", ex.toString())
            }
        } else {
            Log.w("provisionValetudo", "Couldn't find wifi network")
        }

        return result
    }

    fun getRobotWifiNetwork(): Network? {
        val allNetworks = connectivityManager.allNetworks

        val wifiNetwork = allNetworks.find {
            val capabilities = connectivityManager.getNetworkCapabilities(it)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        }

        return wifiNetwork
    }

    private fun getJSON(connection: URLConnection): JSONObject {
        val bufferedReader = BufferedReader(InputStreamReader(connection.getInputStream()))
        val stringBuffer = StringBuffer()
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuffer.append(line)
        }

        return JSONObject(stringBuffer.toString())
    }

    private val gatewayIp: String?
        get() {
            //https://stackoverflow.com/a/16712367
            val dhcpInfo = wifiManager.dhcpInfo
            val gatewayIpBytes = BigInteger.valueOf(dhcpInfo.gateway.toLong()).toByteArray()

            return if (gatewayIpBytes.size == 4) {
                val gatewayIp = InetAddress.getByAddress(gatewayIpBytes.reversedArray())

                gatewayIp.hostAddress
            } else {
                null
            }
        }

}
