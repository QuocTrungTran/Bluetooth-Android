package com.example.wheelchaircontroller


import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import java.io.IOException
import java.util.*

enum class Direction {
    Up, Right, Down, Left, Stop
}
class MainActivity : AppCompatActivity() {

    companion object {
        var m_myUUID: UUID = UUID.fromString("74fbcbeb-acd7-4c2d-bd7f-2a223923a8dc")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toggleBtn = findViewById<ToggleButton>(R.id.toggle_button)
        toggleBtn.isEnabled = false
        toggleBtn.setTextColor(Color.GRAY)

        val stopBtn = findViewById<Button>(R.id.stop_button)
        stopBtn.setBackgroundColor(Color.RED)
        stopBtn.setOnClickListener { control(Direction.Stop)}


        val rightBtn = findViewById<ImageButton>(R.id.right_button)
        rightBtn.setOnClickListener {control(Direction.Right) }
        rightBtn.setOnLongClickListener { v -> handleLongPress(v, Direction.Right) }


        val leftBtn = findViewById<ImageButton>(R.id.left_button)
        leftBtn.setOnClickListener {control(Direction.Left, )}
        leftBtn.setOnLongClickListener { v -> handleLongPress(v, Direction.Left)}

        val upBtn = findViewById<ImageButton>(R.id.up_button)
        upBtn.setOnClickListener {control(Direction.Up)}
        upBtn.setOnLongClickListener {v -> handleLongPress(v, Direction.Up)}


        val downBtn = findViewById<ImageButton>(R.id.down_button)
        downBtn.setOnClickListener {control(Direction.Down) }
        downBtn.setOnLongClickListener {v -> handleLongPress(v, Direction.Down) }

        val connectBtn = findViewById<Button>(R.id.connect_button)
        connectBtn.setOnClickListener { connectDevice() }

        val disconnectBtn = findViewById<Button>(R.id.disconnect_button)
        disconnectBtn.setOnClickListener{disconnectDevice(toggleBtn)}

        // get the connected device's MAC bluetooth address
        m_address = intent.getStringExtra(SelectDevice.EXTRA_ADDRESS).toString()
        if (m_address != "null") {
            ConnectToDevice(this, toggleBtn).execute()
        }
    }


    /**
     * send a string to other device via bluetooth
     * @param input: the string we want to send
     */
    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     *  start another activity that prompts user to select a device to connect with
     */
    private fun connectDevice() {
        val intent = Intent(this, SelectDevice::class.java)
        startActivity(intent)
    }

    /**
     * close the socket and set toggle button to off
     */
    private fun disconnectDevice(toggleBtn: ToggleButton) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
                toggleBtn.toggle()
                toggleBtn.setTextColor(Color.GRAY)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * send command for each 100 milliseconds when we're holding down a button
     * @param v: our buttons
     * @param direction:
     */
    private fun handleLongPress (v: View, direction: Direction): Boolean {
        val handler = Handler(Looper.myLooper()!!)
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler.removeCallbacks(this)
                if (v.isPressed) {
                    control(direction)
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.postDelayed(runnable, 0)
        return true
    }

    /**
     * function to send directions
     * @param direction: up, down, left, right
     */
    private fun control(direction: Direction): Boolean {

        when(direction) {
            Direction.Up ->  {
                sendCommand("Up click")
                return true
            }
            Direction.Down -> {
                sendCommand("Down click")
                return true
            }
            Direction.Right -> {
                sendCommand("Right click")
                return true
            }
            Direction.Left -> {
                sendCommand("Left click")
                return true
            }
            Direction.Stop -> {
                sendCommand("Stop click")
                return true
            }
        }
    }

    private class ConnectToDevice(c : Context, toggleButton: ToggleButton) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context
        private val toggleBtn: ToggleButton

        init {
            this.context = c
            this.toggleBtn = toggleButton
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("data", "couldn't connect")
                Toast.makeText(context, "Fail to connect to the selected device", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()
                m_isConnected = true
                toggleBtn.toggle()
                toggleBtn.setTextColor(Color.GREEN)
            }
            m_progress.dismiss()
        }
    }
}

