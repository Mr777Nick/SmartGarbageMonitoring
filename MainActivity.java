package mr777nick.smartgarbagemonitoring.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import mr777nick.smartgarbagemonitoring.R;
import mr777nick.smartgarbagemonitoring.helpers.MQTTHelper;

public class MainActivity extends AppCompatActivity {
    MQTTHelper mqttHelper;

    TextView dataReceived;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataReceived = (TextView) findViewById(R.id.dataReceived);

        //startMqtt();

        Thread thread = new Thread() {
            @Override
            public void run() {
                try { Thread.sleep(2000); }
                catch (InterruptedException e) {}

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startMqtt();
                    }
                });
            }
        };

        thread.start();
    }



    private void startMqtt() {
        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug", mqttMessage.toString());
                dataReceived.setText(mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
}