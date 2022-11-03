package cordova.plugin.miplugin;

//Paquetes requeridos por Cordova
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//API Nativo
import android.widget.Toast;

/**
 * This class echoes a string called from JavaScript.
 */
public class EntelGPS extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // Verificar que el usuario envió la acción "startGPS"
        if (action.equals("startGPS")) {
            String message = args.getString(0);
            this.startGPS(message, callbackContext);
            return true;
        }
        return false;
    }

    private void startGPS(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            // Crear el Toast
            Toast toast = Toast.makeText(cordova.getActivity(), message, Toast.LENGTH_SHORT);
            // Mostrar toast
            toast.show();
            // Envía un resultado al callbackContext
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
