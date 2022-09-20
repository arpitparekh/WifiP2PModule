package com.example.wifip2pmodule

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.widget.Toast


class WifiBroadCastReceiver(val manager: WifiP2pManager,val channel: WifiP2pManager.Channel,val activity: WifiMainActivity) : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {

        when(intent?.action){

            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION->{
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1)
                if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                    Toast.makeText(activity,"Wifi is On",Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(activity,"Wifi is Off",Toast.LENGTH_SHORT).show()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION->{

                if(manager!=null){
                    manager.requestPeers(channel,activity.peerListener)
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION->{
                if(manager==null){
                    return
                }
                val info  = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if(info?.isConnected == true){
                    manager.requestConnectionInfo(channel,activity.connectionInfoListener)
                }else{
                    activity.binding.tvStatus.text = "Device Disconnected"
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION->{

            }

        }
    }
}