package com.juansenen.salignertest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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


public class TemperatureFragment extends Fragment implements ServiceConnection, SerialListener {

    private static final String TAG = "TemperatureFragment";
    private enum Connected {False, Pending, True}

    private TextView resposteText;
    private TextView connectionStatus;
    private Button buttonServOn, buttonSetOff, buttonTempOffset, buttonGetTemp;
    private String deviceAddress;
    private SerialService service;
    private Connected connected = Connected.False;
    private boolean waitForFirstResponse = true;

    private static final String EXPECTED_FIRST_RESPONSE = "020606031104";


    public TemperatureFragment() {
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
        View temperatureView = inflater.inflate(R.layout.fragment_temperature, container, false);
        resposteText = temperatureView.findViewById(R.id.textCode);
        buttonServOn = temperatureView.findViewById(R.id.ButServOn);
        buttonSetOff = temperatureView.findViewById(R.id.butSetOff);
        buttonTempOffset = temperatureView.findViewById(R.id.butTempOffSet);
        buttonGetTemp = temperatureView.findViewById(R.id.butGetTemp);
        connectionStatus = temperatureView.findViewById(R.id.connectionStatus);


        buttonServOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("0207E20103EF04");
            }
        });
        buttonSetOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("0208F90037033D04");
            }
        });
        buttonTempOffset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("0206F8030304");
            }
        });
        buttonGetTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("0206E403EF04");
                // Aquí agregamos una verificación para asegurarnos de que el procesamiento adicional
                // solo se realice cuando se presiona el botón de getTemp
                waitForFirstResponse = true; // Reiniciar el flag para esperar la primera respuesta
            }
        });

        return temperatureView;
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
        Log.d("TemperatureFragment", "onSerialConnect() called");
    }

    @Override
    public void onSerialConnectError(Exception e) {
        connected = Connected.False;
        updateConnectionStatus(false);
        Log.e("TemperatureFragment", "onSerialConnectError() called with error: " + e.getMessage());
    }

    @Override
    public void onSerialRead(byte[] data) {
        // Convertir los datos a su representación hexadecimal
        String hexString = TextUtil.toHexString(data);

        // Mostrar los datos recibidos en el Log
        Log.d(TAG, "Received data: " + hexString);

        // Convertir el código esperado a una cadena hexadecimal
        String expectedHexString = TextUtil.toHexString(TextUtil.fromHexString(EXPECTED_FIRST_RESPONSE));

        if (waitForFirstResponse) {
            // Verificar si la primera respuesta coincide con el código esperado
            if (hexString.equals(expectedHexString)) {
                // Si es el código esperado, esperar la segunda respuesta
                waitForFirstResponse = false;
            } else {
                // Si no es el código esperado, mostrar en el TextView
                resposteText.setText(hexString);
            }
        } else {
            // Convertir la parte relevante de la segunda respuesta a decimal
            String relevantDataHexString = hexString.substring(10, 14);
            int decimalValue = Integer.parseInt(relevantDataHexString, 16);

            // Mostrar el resultado en el TextView
            resposteText.setText("Decimal value: " + decimalValue);
        }
    }

//        // Mostrar los datos recibidos en el TextView
//        resposteText.setText(hexString + "\n");
//
//        // También puedes agregar un registro para mostrar los datos recibidos en el Log
//        Log.d(TAG, "Received data in hex: " + hexString);




    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        // Implementa esta versión del método para recibir datos como una cola de arrays de bytes
        receive(datas);
    }


//    private void receive(ArrayDeque<byte[]> datas) {
//        for (byte[] data : datas) {
//            // Convertir los datos a su representación hexadecimal
//            String hexString = TextUtil.toHexString(data);
//            // Mostrar los datos recibidos en el TextView
//
//            resposteText.append(hexString + "\n");
//            // También puedes agregar un registro para mostrar los datos recibidos en el Log
//            Log.d(TAG, "Received data: " + hexString);
//        }
//    }
private void receive(ArrayDeque<byte[]> datas) {
    // Variable para controlar si ya hemos mostrado la segunda respuesta
    boolean secondResponseShown = false;

    // Convertir el código esperado a una cadena hexadecimal
    String expectedHexString = TextUtil.toHexString(TextUtil.fromHexString(EXPECTED_FIRST_RESPONSE));

    for (byte[] data : datas) {
        // Convertir los datos a su representación hexadecimal
        String hexString = TextUtil.toHexString(data);

        // Mostrar los datos recibidos en el Log
        Log.d(TAG, "Received data: " + hexString);

        // Verificar si la primera respuesta coincide con el código esperado
        if (waitForFirstResponse && hexString.equals(expectedHexString)) {
            // Si es el código esperado, marcar que hemos recibido la primera respuesta
            waitForFirstResponse = false;
        } else {
            // Si ya hemos mostrado la segunda respuesta, no necesitamos seguir procesando más datos
            if (secondResponseShown) {
                break;
            }

            // Mostrar la respuesta en el TextView solo si ya hemos recibido la primera respuesta esperada
            if (!waitForFirstResponse) {
                // Convertir la parte relevante de la segunda respuesta a decimal
                String relevantDataHexString = hexString.substring(10, 14).replaceAll("\\s+", ""); //Quitamos espacios en blanco
                float decimalValue = (Integer.parseInt(relevantDataHexString, 16))/10;

                // Mostrar el resultado en el TextView
                resposteText.setText("Decimal value: " + decimalValue);

                secondResponseShown = true;
            }
        }
    }
}

    @Override
    public void onSerialIoError(Exception e) {
        connected = Connected.False;
        updateConnectionStatus(false);
        Log.e("TemperatureFragment", "onSerialIoError() called with error: " + e.getMessage());
    }
    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        updateConnectionStatus(false);
    }
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            connectionStatus.setText("Connected");
        } else {
            connectionStatus.setText("Disconnected");
        }
    }

}