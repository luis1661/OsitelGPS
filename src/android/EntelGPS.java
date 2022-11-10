package cordova.plugin.miplugin;

//Paquetes requeridos por Cordova
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
// API axmobility
import androidx.annotation.NonNull;
import com.axiros.axmobility.AxEvents;
import com.axiros.axmobility.android.AxMobility;
import com.axiros.axmobility.android.AxSettings;
import com.axiros.axmobility.android.utils.Constants;
import com.axiros.axmobility.events.TR143Events;
import com.axiros.axmobility.tr143.DownloadDiagnostic;
import com.axiros.axmobility.tr143.TCPDiagnostic;
import com.axiros.axmobility.tr143.TR143Diagnostic;
import com.axiros.axmobility.tr143.UDPDiagnostic;
import com.axiros.axmobility.tr143.UploadDiagnostic;
//API FIN
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//API Nativo
import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import org.apache.cordova.PluginResult; //PluginResult

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import io.cordova.hellocordova.BuildConfig;

/**
 * This class echoes a string called from JavaScript.
 */
public class EntelGPS extends CordovaPlugin {

    private PluginResult pluginResultNORESULT = new  PluginResult(PluginResult.Status.NO_RESULT);
    private CallbackContext newCallbackContext = null;
    private String Key_Osiptel;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // Verificar que el usuario envió la acción "startGPS"
        if (action.equals("startGPS")) {
            newCallbackContext = callbackContext;
            String message = args.getString(0);
            this.startGPS(message, callbackContext);
            return true;
        }
        return false;
    }

    private void startGPS(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            cordova.getActivity().runOnUiThread(new Runnable(){
                public void run(){
                    pluginResultNORESULT.setKeepCallback(true);
                    Key_Osiptel = message;
                    solicitarPermiso();
                }
            });
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    private void solicitarPermiso(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String[] permissions = {
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if(hasAllPermissions(permissions)){
                axmobilityStart();
            }else{
                cordova.requestPermissions(
                        this,
                        2,
                        permissions
                );
            }
        }
    }
    //Valida si lo servicio estas habilitados
    private boolean hasAllPermissions(String[] permissions){
        for (String permission : permissions) {
            if(!cordova.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (!AxMobility.requestUserPermission(cordova.getActivity())) {
            axmobilityStart();
        }
    }

    private void axmobilityStart() {
        /*
         * Here we add callbacks to be called when TR143 events is triggered
         * during the diagnostics.
         */
        final AxEvents events = new AxEvents.Builder()
                .withTR143Events(new TR143Events() {
                    @Override
                    public void onBegin(TR143Diagnostic tr143Diagnostic) {
                        handleOnBegin(tr143Diagnostic);
                    }

                    @Override
                    public void onCompleted(TR143Diagnostic tr143Diagnostic) {
                        handleOnCompleted(tr143Diagnostic);
                    }
                })
                .build();

        try {
            String clientVersion = cordova.getActivity().getPackageManager().getPackageInfo(cordova.getActivity().getPackageName(), 0)
                    .versionName;

            int stringId = cordova.getActivity().getApplicationInfo().labelRes;
            String appName = stringId == 0 ? cordova.getActivity().getApplicationInfo().nonLocalizedLabel.toString()
                    : cordova.getActivity().getApplicationContext().getString(stringId);

            /*
             * The library is configured to start using the ACS key, some client
             * specific information and the events chosen to be monitored.
             */
            AxSettings settings = AxSettings.Builder()
                    .withKey(Key_Osiptel)
                    .withClientName(appName)
                    .withClientVersion(clientVersion)
                    .withEvents(events)
                    .build();

            /* Starts the library */
            AxMobility.start(cordova.getContext().getApplicationContext(), settings);
            newCallbackContext.success();
            newCallbackContext=null;
        } catch (Exception e) {
            Log.e(Constants.DEFAULT_LOG_TAG, "Client MainActivity axmobilityStart()", e);
        }
    }
    private void handleOnBegin(TR143Diagnostic tr143Diagnostic) {
        String name = "unknown";

        if (tr143Diagnostic instanceof DownloadDiagnostic) {
            name = "Download";
        } else if (tr143Diagnostic instanceof UploadDiagnostic) {
            name = "Upload";
        } else if (tr143Diagnostic instanceof UDPDiagnostic) {
            name = "UDP";
        } else if (tr143Diagnostic instanceof TCPDiagnostic) {
            name = "TCP";
        }

        final String text = "Starting " + name + " diagnostic";

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cordova.getContext().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleOnCompleted(TR143Diagnostic tr143Diagnostic) {
        Properties properties =  new Properties();
        double averageSpeed;
        String state;
        String date = SimpleDateFormat.getDateTimeInstance().format(new Date());

        if (tr143Diagnostic instanceof DownloadDiagnostic) {
            DownloadDiagnostic downloadDiagnostic = (DownloadDiagnostic) tr143Diagnostic;
            averageSpeed = downloadDiagnostic.getAverageSpeed();
            state = downloadDiagnostic.getState();
            properties.setProperty(CommonUtils.KEY_DOWNLOAD_SPEED, String.valueOf(averageSpeed));
            properties.setProperty(CommonUtils.KEY_DOWNLOAD_STATE, state);
            properties.setProperty(CommonUtils.KEY_DOWNLOAD_DATE, date);
        } else if (tr143Diagnostic instanceof UploadDiagnostic) {
            UploadDiagnostic uploadDiagnostic = (UploadDiagnostic) tr143Diagnostic;
            averageSpeed = uploadDiagnostic.getAverageSpeed();
            state = uploadDiagnostic.getState();
            properties.setProperty(CommonUtils.KEY_UPLOAD_SPEED, String.valueOf(averageSpeed));
            properties.setProperty(CommonUtils.KEY_UPLOAD_STATE, state);
            properties.setProperty(CommonUtils.KEY_UPLOAD_DATE, date);
        } else if (tr143Diagnostic instanceof UDPDiagnostic) {
            UDPDiagnostic udpDiagnostic = (UDPDiagnostic) tr143Diagnostic;
            averageSpeed = udpDiagnostic.getAverageResponseTime();
            state = udpDiagnostic.getState();
            properties.setProperty(CommonUtils.KEY_UDP_SPEED, String.valueOf(averageSpeed));
            properties.setProperty(CommonUtils.KEY_UDP_STATE, state);
            properties.setProperty(CommonUtils.KEY_UDP_DATE, date);
        } else if (tr143Diagnostic instanceof TCPDiagnostic) {
            TCPDiagnostic tcpDiagnostic = (TCPDiagnostic) tr143Diagnostic;
            averageSpeed = tcpDiagnostic.getAverageResponseTime();
            state = tcpDiagnostic.getState();
            properties.setProperty(CommonUtils.KEY_TCP_SPEED, String.valueOf(averageSpeed));
            properties.setProperty(CommonUtils.KEY_TCP_STATE, state);
            properties.setProperty(CommonUtils.KEY_TCP_DATE, date);
        }
        saveResults(properties);
    }
    private void saveResults(Properties properties) {
        String filename = cordova.getActivity().getApplicationContext().getFilesDir().getAbsolutePath() + "/" + CommonUtils.FILE_RESULTS;
        File file = new File(filename);

        try {
            FileWriter writer;

            if (file.exists()) {
                writer = new FileWriter(file, true);
            } else {
                writer = new FileWriter(file);
            }

            properties.store(writer, "TR143 Results");
            writer.close();
        } catch (Exception e) {
            Log.e(Constants.DEFAULT_LOG_TAG, "serialize:", e);
        }
    }

    public class CommonUtils {
        public static final String KEY_DOWNLOAD_SPEED = "averageDownloadSpeed";
        public static final String KEY_DOWNLOAD_STATE = "downloadState";
        public static final String KEY_DOWNLOAD_DATE = "lastDownloadTest";
        public static final String KEY_UPLOAD_SPEED = "averageUploadSpeed";
        public static final String KEY_UPLOAD_STATE = "uploadState";
        public static final String KEY_UPLOAD_DATE = "lastUploadTest";
        public static final String KEY_UDP_SPEED = "averageResponseTime";
        public static final String KEY_UDP_STATE = "UDPState";
        public static final String KEY_UDP_DATE = "lastUDPTest";
        public static final String KEY_TCP_SPEED = "TCPAverageResponseTime";
        public static final String KEY_TCP_STATE = "TCPState";
        public static final String KEY_TCP_DATE = "lastTCPTest";
        public static final String FILE_RESULTS = "results.dat";
    }
}
