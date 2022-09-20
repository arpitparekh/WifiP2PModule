package com.example.wifip2pmodule

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.wifip2pmodule.databinding.ActivityWifiMainBinding
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


class WifiMainActivity : AppCompatActivity() {

    lateinit var binding : ActivityWifiMainBinding
    lateinit var wifiLauncher: ActivityResultLauncher<Intent>
    lateinit var permissionLauncher: ActivityResultLauncher<String>
    lateinit var photoLauncher: ActivityResultLauncher<String>
    lateinit var manager: WifiP2pManager
    lateinit var channel: WifiP2pManager.Channel
    lateinit var wifiBroadCastReceiver: WifiBroadCastReceiver
    lateinit var intentFilter: IntentFilter
    lateinit var peers : ArrayList<WifiP2pDevice>
    lateinit var deviceNameList : ArrayList<String>
    lateinit var serverClass: ServerClass
    lateinit var clientClass: ClientClass
    lateinit var sendReceiveClass: SendReceiveClass


    companion object{

        val MESSAGE_READ = 1
    }

    val peerListener : WifiP2pManager.PeerListListener = WifiP2pManager.PeerListListener {
        if(!it.deviceList.equals(peers)){

            peers.clear()
            peers.addAll(it.deviceList)

            for(device : WifiP2pDevice in peers){

                deviceNameList.add(device.deviceName)

            }

            val adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceNameList)
            binding.lvDevices.adapter = adapter
        }
        if(peers.size == 0){
            Toast.makeText(this,"No Device Found",Toast.LENGTH_SHORT).show()
        }
    }

    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener {
        val inetAddress =  it.groupOwnerAddress

        if(it.isGroupOwner && it.groupFormed){
            binding.tvStatus.text = "Host"

            // start Server Class

            serverClass = ServerClass()
            serverClass.start()


        }else if(it.groupFormed){
            binding.tvStatus.text = "Client"

            //start client class

            clientClass = ClientClass(inetAddress)
            clientClass.start()

        }

    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this,mainLooper,null)
        wifiBroadCastReceiver = WifiBroadCastReceiver(manager,channel,this)
        intentFilter = IntentFilter()

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        wifiLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback {

                if(isWifiConnected(this) == true){
                    Toast.makeText(this,"Wifi Connected",Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this,"Please Turn on Wifi",Toast.LENGTH_SHORT).show()
                }

            })
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(),
            ActivityResultCallback {
                if(it){
                    Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
                }
            })
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        photoLauncher = registerForActivityResult(ActivityResultContracts.GetContent(),
            ActivityResultCallback {

            })

        binding.btnWifi.setOnClickListener {
            if(isWifiConnected(this) == true){
                Toast.makeText(this,"Wifi is Already Connected",Toast.LENGTH_SHORT).show()
            }else{

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                    wifiLauncher.launch(panelIntent)
                } else {
                    (getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.apply { isWifiEnabled = true }
                }

            }
        }

        binding.btnDiscover.setOnClickListener {
            manager.discoverPeers(channel,
                object : WifiP2pManager.ActionListener{
                    override fun onSuccess() {
                        binding.tvStatus.text = "Discovery Started"
                    }

                    override fun onFailure(p0: Int) {
                        binding.tvStatus.text = "Discovery Failed"
                    }

                })
        }

        binding.btnSend.setOnClickListener {

            photoLauncher.launch("images/*")

        }

        binding.lvDevices.setOnItemClickListener { parent, view, position, id ->

            val device = peers[position]
            val wifiP2pConfig = WifiP2pConfig()
            wifiP2pConfig.deviceAddress = device.deviceAddress

            manager.connect(channel,wifiP2pConfig,object : WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Toast.makeText(this@WifiMainActivity,"Connected to ${device.deviceName}",Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(p0: Int) {
                    Toast.makeText(this@WifiMainActivity,"Not Connected",Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.btnSendMessage.setOnClickListener {

            val messgae = binding.edtMessage.text.toString()

            sendReceiveClass.write(messgae.toByteArray())

        }
    }

    private fun isWifiConnected(context: Context): Boolean? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiBroadCastReceiver,intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiBroadCastReceiver)
    }

    inner class ServerClass : Thread() {

        lateinit var socket : Socket
        lateinit var serverSocket: ServerSocket

        override fun run() {
            try{
                serverSocket = ServerSocket(8888)
                socket = serverSocket.accept()
                sendReceiveClass = SendReceiveClass(socket)
                sendReceiveClass.start()
            }catch (e : Exception){
                Log.i("serverError",e.message.toString())
            }
        }

    }

    inner class ClientClass() : Thread(){

        lateinit var socket: Socket
        lateinit var hostAddress : String

        constructor(hostInetAddress: InetAddress) : this() {
            hostAddress = hostInetAddress.hostAddress
            socket = Socket()

        }

        override fun run() {

            try{
                socket.connect(InetSocketAddress(hostAddress,8888),500)
                sendReceiveClass = SendReceiveClass(socket)
                sendReceiveClass.start()

            }catch (e : Exception){
                Log.i("serverError",e.message.toString())
            }
        }
    }

    inner class SendReceiveClass(val socket: Socket) : Thread() {

        var inputStream : InputStream
        var outputStream: OutputStream

        init {
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes : Int

            while (socket!=null){

                try{
                    bytes = inputStream.read(buffer)
                    if(bytes>0){

                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget()

                    }
                }catch (e : Exception){
                    Log.i("serverError",e.message.toString())
                }

            }
        }

        fun write(byteArray : ByteArray){

            try{
                outputStream.write(byteArray)
            }catch (e : Exception){
                Log.i("serverError",e.message.toString())
            }


        }

    }

    val handler = Handler(Looper.getMainLooper(), Handler.Callback {
        when(it.what){
            MESSAGE_READ->{

                val byteBuff = it.obj as ByteArray
                val temp = String(byteBuff,0,it.arg1)
                Log.i("msg",temp)

            }
        }// main looper or my looper?
        true
    })


}