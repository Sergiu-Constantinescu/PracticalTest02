package ro.pub.cs.systems.eim.practicaltest02;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class PracticalTest02MainActivity extends AppCompatActivity {

    public final static String SERVER_HOST = "localhost";
    public final static Integer SERVER_PORT = 2021;
    public final static String TAG = "PracticalTest02";

    private ButtonClickListener buttonClickListener = new ButtonClickListener();
    private ServerThread serverThread;
    private TextView text_view_type;
    private TextView text_view_ability;

    public static BufferedReader getReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static PrintWriter getWriter(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    private class ButtonClickListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            EditText pokemon_name = (EditText) findViewById(R.id.edit_text_pokemon_name);

            Client clientAsyncTask = new Client(text_view_type, text_view_ability);
            clientAsyncTask.execute(SERVER_HOST, SERVER_PORT.toString());
        }
    }

    public class ServerThread extends Thread {
        private boolean isRunning;
        private ServerSocket serverSocket;
        private String pokemon_type;
        private String pokemon_abilities;

        public ServerThread() {
        }

        public void startServer() {
            isRunning = true;
            start();
            Log.v(TAG, "startServer() method");

            pokemon_type = "aaaa";
            pokemon_abilities = "bbbb, cccc";
        }

        public void stopServer() {
            isRunning = false;
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                Log.e(TAG, "An exception has occurred: " + ioException.getMessage());
            }
            Log.v(TAG, "stopServer() method");
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT, 50, InetAddress.getByName("0.0.0.0"));
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    Log.v(TAG, "accept() " + socket.getInetAddress());
                    if (socket != null) {
                        CommThread communicationThread = new CommThread(socket, pokemon_type, pokemon_abilities);
                        communicationThread.start();
                    }
                }
            } catch (IOException ioException) {
                Log.e(TAG, "An exception has occurred: " + ioException.getMessage());
            }
        }
    }

    public class CommThread extends Thread {

        private Socket socket;
        private String pokemon_type;
        private String pokemon_abilities;

        public CommThread(Socket socket, String pokemon_type, String pokemon_abilities) {
            this.socket = socket;
            this.pokemon_type = pokemon_type;
            this.pokemon_abilities = pokemon_abilities;
        }

        @Override
        public void run() {
            try {
                Log.v(TAG, "Connection opened to " + socket.getLocalAddress() + ":" + socket.getLocalPort()+ " from " + socket.getInetAddress());
                PrintWriter printWriter = getWriter(socket);
                printWriter.println(pokemon_type + "-" + pokemon_abilities);
                socket.close();
                Log.v(TAG, "Connection closed");
            } catch (IOException ioException) {
                Log.e(TAG, "An exception has occurred: " + ioException.getMessage());
            }
        }
    }

    public class Client extends AsyncTask<String, String, Void> {

        private TextView type;
        private TextView ability;

        public Client(TextView type, TextView ability) {
            this.type = type;
            this.ability = ability;
        }

        @Override
        protected Void doInBackground(String... params) {
            Socket socket = null;
            try {
                String serverAddress = params[0];
                int serverPort = Integer.parseInt(params[1]);
                socket = new Socket(serverAddress, serverPort);
                if (socket == null) {
                    return null;
                }
                Log.v(TAG, "Connection opened with " + socket.getInetAddress() + ":" + socket.getLocalPort());
                BufferedReader bufferedReader = getReader(socket);
                String currentLine;
                while ((currentLine = bufferedReader.readLine()) != null) {
                    publishProgress(currentLine);
                }
            } catch (IOException ioException) {
                Log.e(TAG, "An exception has occurred: " + ioException.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    Log.v(TAG, "Connection closed");
                } catch (IOException ioException) {
                    Log.e(TAG, "An exception has occurred: " + ioException.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            type.setText("");
            ability.setText("");
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            String[] info = progress[0].split("-");
            type.append("Type: " + info[0] + "\n");
            ability.append("Ability: " + info[1] + "\n");
        }

        @Override
        protected void onPostExecute(Void result) {}

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);

        Button get_data_button = (Button)findViewById(R.id.button_get_data);
        text_view_type = (TextView)findViewById(R.id.text_type);
        text_view_ability = (TextView)findViewById(R.id.text_ability);
        get_data_button.setOnClickListener(buttonClickListener);

        serverThread = new ServerThread();
        serverThread.startServer();

        text_view_type.setText("-");
        text_view_ability.setText("-");
        Log.v(TAG, "Starting server");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serverThread.stopServer();
        Log.v(TAG, "Stopping server");
    }
}