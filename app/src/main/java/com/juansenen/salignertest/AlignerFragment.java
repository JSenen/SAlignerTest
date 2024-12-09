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

import java.util.ArrayDeque;

public class AlignerFragment extends Fragment implements ServiceConnection, SerialListener {

    private static final String TAG = "AlignerFragment";
    private enum Connected {False, Pending, True}
    private TextView connectionStatus, mFramesTxt;
    private Button mButtonSample3, mButtonPrevious, mButtonNext;
    private String deviceAddress;
    private SerialService service;
    private Connected connected = Connected.False;
    private boolean waitForFirstResponse = true;
    //TODO: Clean variables dont use
    private static final String EXPECTED_FIRST_RESPONSE = "550008860D00F0AA";
    //private static final String START_SAMMPLING = "55000B06010000010068AA"; //55000B06010000010068AA
    private static final String START_SAMPLING = "55000B060400C8010033AA"; // 4 samples with 200ms between them
    private static final String READ_FRAME0 = "5500090300000061AA"; //5500090304000065AA
    private static final String READ_FRAME1 = "5500090300010062AA";
    private static final String READ_FRAME2 = "5500090300020063AA";
    private static final String READ_FRAME3 = "5500090300030064AA";
    private static final String SELECT_ORDER_COMAND = "550008F2010050AA";
    private static final String DEL_FRAMES_COMAND = "55000904FF000061AA";
    private static final String STOP_SAMPLE = "550007070063AA";

    private String OptionClicked; //Control de la opcion pulsada
    private MatrixView matrixView;
    // Declarar una variable para almacenar la cadena hexadecimal recibida hasta que esté completa
    private String strFrame0 = "";
    private String strFrame1 = "";
    private String strFrame2 = "";
    private String strFrame3 = "";
    private int CurrentSample, MaxSamples;
    private int[][] matrix = new int[48][48];


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
        mButtonSample3 = alignerView.findViewById(R.id.ButSample3);

        mButtonPrevious = alignerView.findViewById(R.id.ButPrev);
        mButtonNext = alignerView.findViewById(R.id.ButNext);
        mFramesTxt = alignerView.findViewById(R.id.FrameTxt);

        connectionStatus = alignerView.findViewById(R.id.connectionStatus);
        matrixView = alignerView.findViewById(R.id.matrixView);

        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CurrentSample == 0)
                {
                    mFramesTxt.setText("0");
                    mButtonNext.setEnabled(false);
                    mButtonPrevious.setEnabled(false);
                }
                else if (CurrentSample < MaxSamples)
                {
                    CurrentSample++;
                    mButtonPrevious.setEnabled(true);
                    if (CurrentSample==1)
                    {
                        drawMatrixFromHexString(strFrame0);
                        mFramesTxt.setText("1");
                    }
                    else if (CurrentSample==2)
                    {
                        drawMatrixFromHexString(strFrame1);
                        mFramesTxt.setText("2");
                    }
                    else if (CurrentSample==3)
                    {
                        drawMatrixFromHexString(strFrame2);
                        mFramesTxt.setText("3");
                    }
                    else if (CurrentSample==4)
                    {
                        drawMatrixFromHexString(strFrame3);
                        mFramesTxt.setText("4");
                        mButtonNext.setEnabled(false);
                    }
                }
            }
        });

        mButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CurrentSample == 0)
                {
                    mFramesTxt.setText("0");
                    mButtonNext.setEnabled(false);
                    mButtonPrevious.setEnabled(false);
                }
                else if (CurrentSample > 1)
                {
                    CurrentSample--;
                    mButtonNext.setEnabled(true);
                    if (CurrentSample==1)
                    {
                        drawMatrixFromHexString(strFrame0);
                        mButtonPrevious.setEnabled(false);
                        mFramesTxt.setText("1");
                    }
                    else if (CurrentSample==2)
                    {
                        drawMatrixFromHexString(strFrame1);
                        mFramesTxt.setText("2");
                    }
                    else if (CurrentSample==3)
                    {
                        drawMatrixFromHexString(strFrame2);
                        mFramesTxt.setText("3");
                    }
                }
            }
        });

        mButtonSample3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OptionClicked = "setsample";
                // Limpiar la cadena combinada para la próxima lectura
                combinedHexString.setLength(0);
                totalReceivedBytes = 0;
                mButtonSample3.setEnabled(false);

                // Envía DEL_FRAMES_COMAND inmediatamente
                Log.i(TAG, "Enviar DEL_FRAMES_COMAND");
                send(DEL_FRAMES_COMAND);
                CurrentSample = 0;
                MaxSamples = 0;

                // Usa un Handler para comprobar la respuesta del DEL_FRAMES_COMAND y enviar START_SAMPLING después de 500 ms
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run()
                    {
                        // Comprobar respuesta a DEL_FRAMES_COMAND
                        String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");
                        Log.i(TAG, "Respuesta a delete frames = " +fullHexString.length() +" /" + fullHexString);

                        // Verificar si la longitud de la cadena es suficiente para contener la respuesta esperada
                        if (fullHexString.length() >= 20)
                        {
                            if (fullHexString.compareTo("55000A84FF000000E2AA")==0)
                            {
                                // Limpiar la cadena combinada para la próxima lectura
                                combinedHexString.setLength(0);
                                totalReceivedBytes = 0;

                                Log.i(TAG, "Enviar START_SAMPLING");
                                send(START_SAMPLING);
                                OptionClicked = "readFrame";
                                // Usa un Handler para leer respuesta a START_SAMPLING y enviar READ_FRAME0 después de 2 segundo
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        // Comprobar respuesta a START_SAMPLING
                                        String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");
                                        Log.i(TAG, "Respuesta a start sampling = " +fullHexString.length() +" " + fullHexString);

                                        // Verificar si la longitud de la cadena es suficiente para contener la respuesta esperada
                                        if (fullHexString.length() >= 16)
                                        {
                                            if (fullHexString.compareTo("550008860000E3AA") == 0)
                                            {
                                                // Limpiar la cadena combinada para la próxima lectura
                                                combinedHexString.setLength(0);
                                                totalReceivedBytes = 0;

                                                Log.i(TAG, "Enviar READ_FRAME0");
                                                send(READ_FRAME0);
                                                // Usa un Handler para leer respuesta a READ_FRAME0 y enviar READ_FRAME1 después de 200 ms
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run()
                                                    {
                                                        // Comprobar respuesta a READ_FRAME0
                                                        String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");
                                                        Log.i(TAG, "Respuesta a read frame 0 = " +fullHexString.length() +" " + fullHexString);

                                                        // Verificar si la longitud de la cadena es suficiente para contener la respuesta esperada
                                                        if (fullHexString.length() >= 9238)
                                                        {
                                                            // hlos 16 primeros caracteres y los 6 últimos
                                                            fullHexString = fullHexString.substring(16, fullHexString.length() - 6);
                                                            Log.i(TAG, "Matriz frame 0 = " +fullHexString.length() +" " + fullHexString);
                                                            strFrame0 = String.copyValueOf(fullHexString.toCharArray());
                                                            drawMatrixFromHexString(strFrame0);
                                                            CurrentSample = 1;
                                                            MaxSamples = 1;
                                                            mFramesTxt.setText("1");

                                                            // Limpiar la cadena combinada para la próxima lectura
                                                            combinedHexString.setLength(0);
                                                            totalReceivedBytes = 0;

                                                            //Log.i(TAG, "Enviar READ_FRAME1");
                                                            //send(READ_FRAME1);

                                                            // Usa un Handler para leer respuesta a READ_FRAME1 y enviar READ_FRAME2 después de 200 ms
//                                                            new Handler().postDelayed(new Runnable() {
//                                                                @Override
//                                                                public void run()
//                                                                {
//                                                                    // Comprobar respuesta a READ_FRAME1
//                                                                    String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");
//                                                                    Log.i(TAG, "Respuesta a read frame 1 = " +fullHexString.length() +" " + fullHexString);
//
//                                                                    // Verificar si la longitud de la cadena es suficiente para contener la respuesta esperada
//                                                                    if (fullHexString.length() >= 9238)
//                                                                    {
//                                                                        // Eliminar los 16 primeros caracteres y los 6 últimos
//                                                                        fullHexString = fullHexString.substring(16, fullHexString.length() - 6);
//                                                                        Log.i(TAG, "Matriz frame 1 = " +fullHexString.length() +" " + fullHexString);
//                                                                        strFrame1 = String.copyValueOf(fullHexString.toCharArray());
//                                                                        MaxSamples++;
//                                                                        mButtonNext.setEnabled(true);
//
//                                                                        // Limpiar la cadena combinada para la próxima lectura
//                                                                        combinedHexString.setLength(0);
//                                                                        totalReceivedBytes = 0;
//
//                                                                        Log.i(TAG, "Enviar READ_FRAME2");
//                                                                        send(READ_FRAME2);

                                                                        // Usa un Handler para leer respuesta a READ_FRAME2 y enviar READ_FRAME3 después de 200 ms
//                                                                        new Handler().postDelayed(new Runnable() {
//                                                                            @Override
//                                                                            public void run()
//                                                                            {
//                                                                                // Comprobar respuesta a READ_FRAME2
//                                                                                String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");
//                                                                                Log.i(TAG, "Respuesta a read frame 2 = " +fullHexString.length() +" " + fullHexString);
//
//                                                                                // Verificar si la longitud de la cadena es suficiente para contener la respuesta esperada
//                                                                                if (fullHexString.length() >= 9238)
//                                                                                {
//                                                                                    // Eliminar los 16 primeros caracteres y los 6 últimos
//                                                                                    fullHexString = fullHexString.substring(16, fullHexString.length() - 6);
//                                                                                    Log.i(TAG, "Matriz frame 2 = " +fullHexString.length() +" " + fullHexString);
//                                                                                    strFrame2 = String.copyValueOf(fullHexString.toCharArray());
//                                                                                    MaxSamples++;
//
//                                                                                    // Limpiar la cadena combinada para la próxima lectura
//                                                                                    combinedHexString.setLength(0);
//                                                                                    totalReceivedBytes = 0;
//
//                                                                                    Log.i(TAG, "Enviar READ_FRAME3");
//                                                                                    send(READ_FRAME3);
//
//                                                                                    // Usa un Handler para leer respuesta a READ_FRAME3 después de 200 ms
//                                                                                    new Handler().postDelayed(new Runnable() {
//                                                                                        @Override
//                                                                                        public void run()
//                                                                                        {
//                                                                                            // Comprobar respuesta a READ_FRAME3
//                                                                                            String fullHexString = combinedHexString.toString().replaceAll("\\s+", "");
//                                                                                            Log.i(TAG, "Respuesta a read frame 3 = " +fullHexString.length() +" " + fullHexString);
//
//                                                                                            // Verificar si la longitud de la cadena es suficiente para contener la respuesta esperada
//                                                                                            if (fullHexString.length() >= 9238)
//                                                                                            {
//                                                                                                // Eliminar los 16 primeros caracteres y los 6 últimos
//                                                                                                fullHexString = fullHexString.substring(16, fullHexString.length() - 6);
//                                                                                                Log.i(TAG, "Matriz frame 3 = " +fullHexString.length() +" " + fullHexString);
//                                                                                                strFrame3 = String.copyValueOf(fullHexString.toCharArray());
//                                                                                                MaxSamples++;
//
//                                                                                                // Limpiar la cadena combinada para la próxima lectura
//                                                                                                combinedHexString.setLength(0);
//                                                                                                totalReceivedBytes = 0;
//                                                                                            }
//                                                                                           mButtonSample3.setEnabled(true);
//                                                                                        }
//                                                                                    }, 2000); // Retardo de 200 ms
//                                                                                }
//                                                                                else
//                                                                                {
//                                                                                    mButtonSample3.setEnabled(true);
//                                                                                }
//                                                                            }
//                                                                        }, 2000); // Retardo de 200 ms
//                                                                    }
//                                                                    else
//                                                                    {
//                                                                        mButtonSample3.setEnabled(true);
//                                                                    }
//                                                                }
//                                                            }, 2000); // Retardo de 200 ms
                                                        }
                                                        else
                                                        {
                                                            mButtonSample3.setEnabled(true);
                                                        }
                                                    }
                                                }, 2000); // Retardo de 200 ms
                                            }
                                            else
                                            {
                                                mButtonSample3.setEnabled(true);
                                            }
                                        }
                                        else
                                        {
                                            mButtonSample3.setEnabled(true);
                                        }
                                    }
                                }, 2000); // Retardo de 2000 milisegundos (2 segundos)
                                // Reactivar botón después de procesar el frame
                                mButtonSample3.setEnabled(true);
                            }
                            else
                            {
                                Log.i(TAG, "Not matching I");
                                mButtonSample3.setEnabled(true);
                            }
                            combinedHexString.setLength(0);
                            totalReceivedBytes = 0;
                        }
                        else
                        {
                            mButtonSample3.setEnabled(true);
                        }
                    }
                }, 500); // Retardo de 500 ms
            }
        });

        return alignerView;
    }


    private void send(String message) {
        try {
                if (service != null)
                {
                    // Convertir el string hexadecimal a bytes
                    byte[] data = TextUtil.fromHexString(message);
                    service.write(data);
                    Log.i(TAG, "Send data (Hex): " + message);
                }
                else
                {
                    Log.e(TAG, "SerialService is null, unable to write data");
                }
            } catch (Exception e)
            {
                onSerialIoError(e);
            }
    }

    @Override
    public void onSerialConnect() {
        connected = Connected.True;
        updateConnectionStatus(true);
        Log.d("AlignerFragment", "onSerialConnect() called");
        // Usa un Handler para enviar SELECT_ORDER_COMAND después de 100 msegundo
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                send(SELECT_ORDER_COMAND);
            }
        }, 100); // Retardo de 100 milisegundos
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
            Log.i(TAG,"Cadena Total HEX " + combinedHexString);
            Log.i(TAG,"Bytes recibidos " + totalReceivedBytes);
        }
    }

    private void drawMatrixFromHexString(String hexString) {
        // Tamaño de la matriz
        int matrixSize = 48;

        // Recorrer la cadena hexadecimal con pasos de 4 caracteres
        int row = 0;
        int col = 0;
        for (int i = 0; i < hexString.length(); i += 4)
        {
            // Verificar si hay suficientes caracteres restantes en la cadena
            if (i + 4 <= hexString.length())
            {
                // Extraer el substring de 4 caracteres
                String subHex = hexString.substring(i, i + 4);
                try {
                    // Tomar los 4 caracteres y convertirlos a un valor entero hexadecimal
                    int Value = Integer.parseInt(subHex, 16);
                    // Asignar la suma como el valor en la matriz
                    matrix[row][col] = Value;
                    // Mover al siguiente índice de columna
                    col++;
                    Log.i(TAG,"Valor fila  " + row + " col " + col + " = " + Value);
                    // Verificar si llegamos al final de la fila
                    if (col >= matrixSize)
                    {
                        // Mover a la siguiente fila
                        row++;
                        // Reiniciar la columna
                        col = 0;
                    }
                } catch (NumberFormatException e)
                {
                    Log.e(TAG, "drawMatrixFromHexString error parsing hex substring: " + subHex);
                    // Manejar errores si es necesario
                }
            }
            else
            {
                Log.e(TAG, "drawMatrixFromHexString error hex size");
                break; // Salir si no hay suficientes caracteres restantes
            }
        }

        // Dibujar la matriz en la vista de la matriz
        matrixView.addMatrix(matrix);
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