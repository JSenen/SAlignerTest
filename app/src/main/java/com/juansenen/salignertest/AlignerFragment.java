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

    private static final String EXPECTED_FIRST_RESPONSE = "550008860D00F0AA";
    private static final String START_SAMPLING = "55000B060400C8010033AA"; // 4 samples with 200ms between them
    private static final String READ_FRAME0 = "5500090300000061AA";
    private static final String DEL_FRAMES_COMAND = "55000904FF000061AA";

    private String OptionClicked; // Control de la opción pulsada
    private Matrix3DView matrix3DView; // Sustituimos MatrixView por Matrix3DView
    private int CurrentSample, MaxSamples;
    private int[][] matrix = new int[48][48];

    // Acumulador de datos recibidos
    private StringBuilder combinedHexString = new StringBuilder();
    private int totalReceivedBytes = 0;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View alignerView = inflater.inflate(R.layout.fragment_aligner, container, false);

        mButtonSample3 = alignerView.findViewById(R.id.ButSample3);
        mButtonPrevious = alignerView.findViewById(R.id.ButPrev);
        mButtonNext = alignerView.findViewById(R.id.ButNext);
        mFramesTxt = alignerView.findViewById(R.id.FrameTxt);
        connectionStatus = alignerView.findViewById(R.id.connectionStatus);

        // Inicializar la vista 3D
        matrix3DView = new Matrix3DView(getContext());
        ((ViewGroup) alignerView.findViewById(R.id.matrixContainer)).addView(matrix3DView);

        setupButtonListeners();

        return alignerView;
    }

    private void setupButtonListeners() {
        mButtonNext.setOnClickListener(v -> {
            if (CurrentSample < MaxSamples) {
                CurrentSample++;
                updateMatrixVisualization();
            }
        });

        mButtonPrevious.setOnClickListener(v -> {
            if (CurrentSample > 1) {
                CurrentSample--;
                updateMatrixVisualization();
            }
        });

        mButtonSample3.setOnClickListener(v -> {
            mButtonSample3.setEnabled(false);
            sendCommandForSampling();
        });
    }

    private void sendCommandForSampling() {
        send(DEL_FRAMES_COMAND); // Limpiar frames
        new Handler().postDelayed(() -> {
            send(START_SAMPLING); // Comando para iniciar sampling
            new Handler().postDelayed(() -> {
                send(READ_FRAME0); // Comando para leer frame 0
                new Handler().postDelayed(this::processFrame0, 2000); // Procesar frame 0 después de 2 segundos
            }, 2000);
        }, 500);
    }

    private void processFrame0() {
        // Procesar los datos recibidos del frame 0 y actualizar la matriz
        String fullHexString = combinedHexString.toString();
        int[][] parsedMatrix = parseMatrixFromHexString(fullHexString);
        update3DMatrix(parsedMatrix);
    }

    private int[][] parseMatrixFromHexString(String hexString) {
        int matrixSize = 48;
        int[][] matrix = new int[matrixSize][matrixSize];
        int row = 0, col = 0;

        for (int i = 0; i < hexString.length(); i += 4) {
            if (i + 4 <= hexString.length()) {
                String subHex = hexString.substring(i, i + 4);
                try {
                    int value = Integer.parseInt(subHex, 16);
                    matrix[row][col] = value;
                    col++;
                    if (col >= matrixSize) {
                        col = 0;
                        row++;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing hex value: " + subHex);
                }
            }
        }
        return matrix;
    }

    private void update3DMatrix(int[][] newMatrix) {
        Matrix3DRenderer renderer = matrix3DView.getRenderer();
        if (renderer != null) {
            renderer.setMatrix(newMatrix);
            matrix3DView.requestRender(); // Forzar el redibujado en 3D
        }
    }

    private void updateMatrixVisualization() {
        if (CurrentSample == 1) {
            processFrame0();
            mFramesTxt.setText("Frame 1");
        }
    }

    private void send(String message) {
        try {
            if (service != null) {
                byte[] data = TextUtil.fromHexString(message);
                service.write(data);
                Log.i(TAG, "Send data (Hex): " + message);
            } else {
                Log.e(TAG, "SerialService is null, unable to write data");
            }
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public void onSerialConnect() {
        connected = Connected.True;
        updateConnectionStatus(true);
    }

    @Override
    public void onSerialConnectError(Exception e) {
        connected = Connected.False;
        updateConnectionStatus(false);
    }

    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        // Procesar cada bloque de datos recibido
        for (byte[] data : datas) {
            String hexString = TextUtil.toHexString(data);
            combinedHexString.append(hexString); // Acumula los datos recibidos en formato hexadecimal
            totalReceivedBytes += data.length;

            Log.i(TAG, "Datos recibidos (Hex): " + hexString);
            Log.i(TAG, "Cadena acumulada (Hex): " + combinedHexString.toString());
            Log.i(TAG, "Bytes totales recibidos: " + totalReceivedBytes);
        }
    }
    @Override
    public void onSerialRead(byte[] data) {
        // Convierte los datos recibidos a una cadena hexadecimal
        String hexString = TextUtil.toHexString(data);

        // Acumula los datos en el StringBuilder
        combinedHexString.append(hexString);

        // Registra los datos en los logs para depuración
        Log.i(TAG, "Datos recibidos (Hex): " + hexString);
        Log.i(TAG, "Cadena acumulada (Hex): " + combinedHexString.toString());
    }


    @Override
    public void onSerialIoError(Exception e) {
        connected = Connected.False;
        updateConnectionStatus(false);
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
