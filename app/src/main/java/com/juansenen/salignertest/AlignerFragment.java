package com.juansenen.salignertest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayDeque;
import java.util.Arrays;


public class AlignerFragment extends Fragment implements ServiceConnection, SerialListener {

    private static final String TAG = "AlignerFragment";
    private enum Connected {False, Pending, True}

    private TextView resposteText;
    private TextView connectionStatus, textReadFrame;
    private Button mButtonSample3, mButtonReadFrame;
    private String deviceAddress;
    private SerialService service;
    private Connected connected = Connected.False;
    private boolean waitForFirstResponse = true;
    //TODO: Clean variables dont use
    private static final String EXPECTED_FIRST_RESPONSE = "550008860300E6AA";
    private static final String SEND_SETSAMPLE = "55000B0603000A000073AA";
    private static final String READ_FRAME = "5500090300000061AA";


    public AlignerFragment() {
        // Required empty public constructor
    }
    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        Log.d(TAG, "Device address: " + deviceAddress);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class));
    }
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }
    @Override
    public void onResume() {
        super.onResume();
        connectIfNeeded();
    }
    private void connectIfNeeded() {
        if (connected == Connected.False && service != null) {
            connect();
        }
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        connectIfNeeded();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        updateConnectionStatus(false);
    }


    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
            Log.d(TAG, "Connecting to Bluetooth device: " + deviceAddress);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View alignerView = inflater.inflate(R.layout.fragment_aligner, container, false);
        resposteText = alignerView.findViewById(R.id.textCode);
        textReadFrame = alignerView.findViewById(R.id.textReadFrame);
        mButtonReadFrame = alignerView.findViewById(R.id.butReadFrame);
        mButtonSample3 = alignerView.findViewById(R.id.ButSample3);
        connectionStatus = alignerView.findViewById(R.id.connectionStatus);


        mButtonSample3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(SEND_SETSAMPLE);
            }
        });


        return alignerView;
    }


    private void send(String message) {
        try {
            if (service != null) {
                // Agregar el retorno de carro y nueva línea al final del mensaje en formato hexadecimal
                message += "0D0A";

                // Convertir el string hexadecimal a bytes
                byte[] data = TextUtil.fromHexString(message);

                // Enviar los datos al servicio serial
                service.write(data);
                Log.i(TAG, "Send data: " + Arrays.toString(data));
                Log.i(TAG, "Send data: " + message);
            } else {
                Log.e(TAG, "SerialService is null, unable to write data");
            }
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    @Override
    public void onSerialConnect() {
        connected = Connected.True;
        updateConnectionStatus(true);
        Log.d("AlignerFragment", "onSerialConnect() called");
    }

    @Override
    public void onSerialConnectError(Exception e) {
        connected = Connected.False;
        updateConnectionStatus(false);
        Log.e("AlignerFragment", "onSerialConnectError() called with error: " + e.getMessage());
    }

    @Override
    public void onSerialRead(byte[] data) {
//        // Convertir los datos a su representación hexadecimal
//        String hexString = TextUtil.toHexString(data);
//
//        // Mostrar los datos recibidos en el Log
//        Log.d(TAG, "Received data: " + hexString);
//
//
//                resposteText.setText(hexString);
//
//
//            resposteText.setText("HEX recibido: " + hexString);
        }


    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        // Implementa esta versión del método para recibir datos como una cola de arrays de bytes
        receive(datas);
    }

private void receive(ArrayDeque<byte[]> datas) {

    for (byte[] data : datas) {
        // Convertir los datos a su representación hexadecimal
        String hexString = TextUtil.toHexString(data);

        // Mostrar los datos recibidos en el Log
        Log.d(TAG, "Received data: " + hexString);

                // Mostrar el resultado en el TextView

                resposteText.setText("HEX: " + hexString);

            }
    }

    @Override
    public void onSerialIoError(Exception e) {
        connected = Connected.False;
        updateConnectionStatus(false);
        Log.e("AlignerFragment", "onSerialIoError() called with error: " + e.getMessage());
    }
    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        updateConnectionStatus(false);
    }
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            connectionStatus.setText("Connected");
            connectionStatus.setTextColor(Color.parseColor("#75e84f"));

        } else {
            connectionStatus.setText("Disconnected");
            connectionStatus.setTextColor(Color.parseColor("#f05746"));
        }
    }

}