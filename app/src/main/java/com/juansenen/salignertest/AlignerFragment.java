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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final String READ_FRAME = "5500090300000061AA"; //5500090304000065AA
    private static final String READ_FRAME_FIRST_COMAND = "550008F200004FAA";
    private static final String STOP_SAMPLE = "550007070063AA";
    //TODO
    //TODO SETSAMPLE
    //TODO FRAME
    
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
                // Envía READ_FRAME_FIRST_COMAND inmediatamente
                send(READ_FRAME_FIRST_COMAND);

                // Usa un Handler para enviar SEND_SETSAMPLE después de 1 segundo
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        send(SEND_SETSAMPLE);
                    }
                }, 1000); // Retardo de 1000 milisegundos (1 segundo)
                // Limpiar la vista de la matriz
                matrixView.clearMatrices();
            }
        });
        mButtonReadFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OptionClicked = "readFrame";
                send(READ_FRAME);
                // Esperar 3 segundos antes de procesar la cadena completa
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Obtener la cadena hexadecimal completa y eliminar los espacios en blanco
                        String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");

                        // Verificar si la longitud de la cadena es suficiente para eliminar los caracteres
                        if (fullHexString.length() >= 36) {
                            // Eliminar los 30 primeros caracteres y los 6 últimos
                            fullHexString = fullHexString.substring(30, fullHexString.length() - 6);

                            Log.i(TAG, "Cadena hexadecimal completa = " + fullHexString);
                            drawMatrixFromHexString(fullHexString);

                            // Limpiar la cadena combinada para la próxima lectura
                            combinedHexString.setLength(0);
                            // Restablecer el recuento total de bytes recibidos
                            totalReceivedBytes = 0;
                        } else {
                            // Mostrar un mensaje de advertencia si la longitud de la cadena no es suficiente
                            Log.w(TAG, "La longitud de la cadena no es suficiente para eliminar los caracteres");
                        }
                    }
                }, 3000); // 3000 milisegundos = 3 segundos
            }
        });
        return alignerView;
    }


    private void send(String message) {
        try {
            if (service != null) {
                // Agregar el retorno de carro y nueva línea al final del mensaje en formato hexadecimal
                final String modifiedMessage = message /* + "0D0A"*/;
            // Convertir el string hexadecimal a bytes
                byte[] data = TextUtil.fromHexString(modifiedMessage);
                switch (OptionClicked) {
                    case "setsample":
                        // Enviar los datos al servicio serial
                        service.write(data);
                        Log.i(TAG, "Send data SETSAMPLE (Hex): " + message);
                        break;
                    case "readFrame":
                        // Enviar los datos al servicio serial después de 1 segundos
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
                        }, 1000); // Retraso de 1/2 s

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

    private StringBuilder combinedHexString = new StringBuilder();
    // Variable para mantener el recuento total de bytes recibidos
    private int totalReceivedBytes = 0;

    // Método para manejar la recepción de datos
    private void receive(ArrayDeque<byte[]> datas) {
        // Concatenar todas las respuestas recibidas en una sola cadena de hexadecimales
        for (byte[] data : datas) {
            String hexString = TextUtil.toHexString(data);
            combinedHexString.append(hexString);
            // Actualizar el recuento total de bytes recibidos
            totalReceivedBytes += data.length;
            Log.i(TAG,"Cadena RECEIVE HEX " + hexString);
        }

        // Verificar la opción pulsada y procesar la cadena según corresponda
        switch (OptionClicked) {
            case "setsample":
                // Muestra los datos recibidos en el Log
                Log.d(TAG, "Received data setSample: " + combinedHexString.toString());
                break;

            case "readFrame":
                // Convierte la cadena hexadecimal completa en matriz de valores y dibuja la matriz
                String fullHexString = combinedHexString.toString();

                // Eliminar los espacios en blanco de la cadena
                fullHexString = fullHexString.replaceAll("\\s+", "");

                Log.i(TAG, "Cadena hexadecimal completa = " + fullHexString);
                //drawMatrixFromHexString(fullHexString);

                // Limpiar la cadena combinada para la próxima lectura
                //combinedHexString.setLength(0);
                // Restablecer el recuento total de bytes recibidos
                totalReceivedBytes = 0;
                break;
        }
    }

    private void drawMatrixFromHexString(String hexString) {
        // Tamaño de la matriz
        int matrixSize = 48;
        int[][] matrix = new int[matrixSize][matrixSize];

        // Recorrer la cadena hexadecimal con pasos de 4 caracteres
        int row = 0;
        int col = 0;
        for (int i = 0; i < hexString.length(); i += 4) {
            // Verificar si hay suficientes caracteres restantes en la cadena
            if (i + 4 <= hexString.length()) {
                // Extraer el substring de 4 caracteres
                String subHex = hexString.substring(i, i + 4);
                try {
                    // Tomar los primeros dos caracteres y convertirlos a un valor entero hexadecimal
                    int firstValue = Integer.parseInt(subHex.substring(0, 2), 16);
                    // Tomar los siguientes dos caracteres y convertirlos a un valor entero hexadecimal
                    int secondValue = Integer.parseInt(subHex.substring(2), 16);
                    // Calcular la suma de los dos valores
                    int value = firstValue + secondValue;
                    // Asignar la suma como el valor en la matriz
                    matrix[row][col] = value;
                    // Mover al siguiente índice de columna
                    col++;
                    // Verificar si llegamos al final de la fila
                    if (col >= matrixSize) {
                        // Mover a la siguiente fila
                        row++;
                        // Reiniciar la columna
                        col = 0;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing hex substring: " + subHex);
                    // Manejar errores si es necesario
                }
            } else {
                break; // Salir si no hay suficientes caracteres restantes
            }
        }

        // Dibujar la matriz en la vista de la matriz
        matrixView.addMatrix(matrix);
    }
    private int[][] convertHexStringToMatrix(String hexString) {
        // Eliminar los espacios en blanco de la cadena
        hexString = hexString.replaceAll("\\s+", "");

        // Tamaño fijo de la matriz
        int matrixSize = 48;
        int[][] matrix = new int[matrixSize][matrixSize];

        // Recorremos la cadena hexadecimal con pasos de 4 caracteres
        for (int i = 0; i < hexString.length(); i += 4) {
            // Verificar si hay suficientes caracteres restantes en la cadena
            if (i + 4 <= hexString.length()) {
                // Extraer el substring de 4 caracteres
                String subHex = hexString.substring(i, i + 4);
                try {
                    // Tomar los primeros dos caracteres y convertirlos a un entero hexadecimal
                    int firstValue = Integer.parseInt(subHex.substring(0, 2), 16);
                    // Tomar los siguientes dos caracteres y convertirlos a un entero hexadecimal
                    int secondValue = Integer.parseInt(subHex.substring(2), 16);
                    // Calcular la suma de los dos valores
                    int value = firstValue + secondValue;
                    // Calcular la posición en la matriz
                    int row = (i / 4) / matrixSize;
                    int col = (i / 4) % matrixSize;
                    // Asignar el valor a la matriz
                    matrix[row][col] = value;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing hex substring: " + subHex);
                    // Manejar errores si es necesario
                }
            } else {
                break; // Salir si no hay suficientes caracteres restantes
            }
        }

        return matrix;
    }

//    private int[][] convertHexStringToMatrix(String hexString) {
//        // Eliminar los espacios en blanco de la cadena
//        hexString = hexString.replaceAll("\\s+", "");
//
//        // Calcular el tamaño de la matriz en función de la longitud de la cadena
//        int matrixSize = (int) Math.ceil(Math.sqrt(hexString.length() / 2.0));
//
//        // Ajustar el tamaño de la matriz a 16x16 si es necesario
//        if (matrixSize != 16) {
//            matrixSize = 16;
//        }
//
//        int[][] matrix = new int[matrixSize][matrixSize];
//        int hexIndex = 0;
//        //TODO 4 bits *16 + suma del siguiente
//        for (int i = 0; i < matrixSize; i++) {
//            for (int j = 0; j < matrixSize; j++) {
//                // Verificar si quedan caracteres suficientes en la cadena
//                if (hexIndex < hexString.length()) {
//                    // Extraer el byte correspondiente de la cadena hexadecimal y eliminar espacios en blanco
//                    String byteString = hexString.substring(hexIndex, Math.min(hexIndex + 2, hexString.length())).trim();
//                    //Log.d(TAG, "Byte string: " + byteString); // Para depurar
//                    try {
//                        int value = Integer.parseInt(byteString, 16);
//                        matrix[i][j] = value;
//                       // Log.d(TAG, "Value at [" + i + "][" + j + "]: " + value);
//
//                    } catch (NumberFormatException e) {
//                        Log.e(TAG, "Error parsing byte string: " + byteString);
//                        // Manejo de errores o información de depuración adicional si es necesario
//                    }
//                    hexIndex += 2;
//                } else {
//                    // Si no quedan más caracteres en la cadena, salir del bucle
//                    break;
//                }
//            }
//        }
//
//        Log.d(TAG, "Hex string for matrix: " + hexString);
//        return matrix;
//    }


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