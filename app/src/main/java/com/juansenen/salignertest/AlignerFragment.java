package com.juansenen.salignertest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;


public class AlignerFragment extends Fragment implements ServiceConnection, SerialListener {

    private static final String TAG = "AlignerFragment";
    private enum Connected {False, Pending, True}
    private TextView connectionStatus;
    private Button mButtonSample3, mButtonReadFrame, mButtonStopSample;
    private String deviceAddress;
    private SerialService service;
    private Connected connected = Connected.False;
    private boolean waitForFirstResponse = true;
    //TODO: Clean variables dont use
    private static final String EXPECTED_FIRST_RESPONSE = "550008860D00F0AA";
    private static final String SEND_SETSAMPLE = "55000B06010000010068AA"; //55000B06010000010068AA
    private static final String READ_FRAME = "5500090304000065AA"; //5500090304000065AA
    private static final String STOP_SAMPLE = "550007070063AA";
    private String OptionClicked; //Control de la opcion pulsada
    private MatrixView matrixView;
    // Declarar una variable para almacenar la cadena hexadecimal recibida hasta que esté completa
    private String receivedHexString = "";
    private boolean isFirstResponse = true;



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
        mButtonReadFrame = alignerView.findViewById(R.id.butReadFrame);
        mButtonSample3 = alignerView.findViewById(R.id.ButSample3);


        connectionStatus = alignerView.findViewById(R.id.connectionStatus);
        matrixView = alignerView.findViewById(R.id.matrixView);


        mButtonSample3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OptionClicked = "setsample";
                send(SEND_SETSAMPLE);
                // Limpiar la vista de la matriz
                matrixView.clearMatrices();
            }
        });
        mButtonReadFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OptionClicked = "readFrame";
                send(READ_FRAME);

            }
        });

        return alignerView;
    }


    private void send(String message) {
        try {
            if (service != null) {
                // Agregar el retorno de carro y nueva línea al final del mensaje en formato hexadecimal
                final String modifiedMessage = message + "0D0A";
            // Convertir el string hexadecimal a bytes
                byte[] data = TextUtil.fromHexString(modifiedMessage);
                switch (OptionClicked) {
                    case "setsample":
                        // Enviar los datos al servicio serial
                        service.write(data);
                        Log.i(TAG, "Send data SETSAMPLE (Hex): " + message);
                        break;
                    case "readFrame":
                        // Enviar los datos al servicio serial después de 2 segundos
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    service.write(data);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                Log.i(TAG, "Send data READFRAME (Hex): " + modifiedMessage);
                            }
                        }, 500); // Retraso de 1/2 s

                        break;
                    case "stopSample":
                        service.write(data);
                        Log.i(TAG, "Send data STOP SAMPLE (Hex): " + message);
                        break;
                }
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

        }


    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        // Implementa esta versión del método para recibir datos como una cola de arrays de bytes
        receive(datas);
    }


    private void receive(ArrayDeque<byte[]> datas) {
        // Inicializa la cadena hexadecimal recibida
        StringBuilder receivedHexString = new StringBuilder();

        // Concatena todas las respuestas recibidas en una sola cadena hexadecimal
        for (byte[] data : datas) {
            receivedHexString.append(TextUtil.toHexString(data));
        }

        // Verifica la opción pulsada y procesa la cadena según corresponda
        switch (OptionClicked) {
            case "setsample":
                // Muestra los datos recibidos en el Log
                Log.d(TAG, "Received data setSample: " + receivedHexString.toString());
                break;

            case "readFrame":
                // Elimina los primeros 14 caracteres de la primera respuesta
                String hexString = receivedHexString.toString();
                if (isFirstResponse && !datas.isEmpty()) {
                    Log.d(TAG, "isFirstResponse: " + isFirstResponse);
                    hexString = hexString.substring(14);
                    isFirstResponse = false; // Ya no es la primera respuesta
                }
                // Elimina los últimos 6 caracteres de la última respuesta
                if (datas.size() == 1) {
                    hexString = hexString.substring(0, hexString.length() - 6);
                }
                // Convierte la cadena hexadecimal en matrices de valores y dibuja las matrices
                drawMatricesFromHexString(hexString);
                break;
        }
    }
    private void drawMatricesFromHexString(String hexString) {
        // Calcula el tamaño de cada matriz individual (48x48)
        int matrixSize = 48;
        int subHexStringLength = matrixSize * matrixSize * 2; // Cada byte se representa por dos caracteres hexadecimales

        // Divide la cadena hexadecimal en subcadenas de tamaño subHexStringLength
        for (int i = 0; i < hexString.length(); i += subHexStringLength) {
            String subHexString = hexString.substring(i, Math.min(i + subHexStringLength, hexString.length()));
            // Convierte la subcadena en una matriz de valores y agrega la matriz a MatrixView
            int[][] matrix = convertHexStringToMatrix(subHexString);
            matrixView.addMatrix(matrix);
        }
    }

    private int[][] convertHexStringToMatrix(String hexString) {
        // Eliminar los espacios en blanco de la cadena
        hexString = hexString.replaceAll("\\s+", "");

        // Calcular el tamaño de la matriz en función de la longitud de la cadena
        int matrixSize = (int) Math.ceil(Math.sqrt(hexString.length() / 2.0));

        // Ajustar el tamaño de la matriz a 16x16 si es necesario
        if (matrixSize != 16) {
            matrixSize = 16;
        }

        int[][] matrix = new int[matrixSize][matrixSize];
        int hexIndex = 0;

        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                // Verificar si quedan caracteres suficientes en la cadena
                if (hexIndex < hexString.length()) {
                    // Extraer el byte correspondiente de la cadena hexadecimal y eliminar espacios en blanco
                    String byteString = hexString.substring(hexIndex, Math.min(hexIndex + 2, hexString.length())).trim();
                    //Log.d(TAG, "Byte string: " + byteString); // Para depurar
                    try {
                        int value = Integer.parseInt(byteString, 16);
                        matrix[i][j] = value;
                       // Log.d(TAG, "Value at [" + i + "][" + j + "]: " + value);

                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing byte string: " + byteString);
                        // Manejo de errores o información de depuración adicional si es necesario
                    }
                    hexIndex += 2;
                } else {
                    // Si no quedan más caracteres en la cadena, salir del bucle
                    break;
                }
            }
        }

        Log.d(TAG, "Hex string for matrix: " + hexString);
        return matrix;
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