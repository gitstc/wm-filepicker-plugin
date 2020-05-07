/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.wavemaker.cordova.plugin;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class FileServicePlugin extends CordovaPlugin {
    private static final String TAG = "FILE";

    //PERMISSION REQUEST CODE
    private static final int WRITE_PERMISSION_REQUEST_CODE = 1000;

    //ACTIVITY REQUEST CODE
    private static final int FILE_REQUEST_CODE = 1000;

    //STATE PROPERTY KEYS
    private static final String STATE_PROPERTY_MIME_TYPE = "mimeType";
    private static final String STATE_PROPERTY_MULTIPLE = "multiple";

    //MESSAGES
    private static final String PERMISSION_DENIED = "1";
    private static final String ERROR_HAPPENED = "2";

    private String mimeType;
    private boolean multiple;
    private CallbackContext mCallbackContext;

    /**
     * Constructor.
     */
    public FileServicePlugin() {
        Log.d(TAG, "File plugin instance created");
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putString(STATE_PROPERTY_MIME_TYPE, mimeType);
        bundle.putBoolean(STATE_PROPERTY_MULTIPLE, multiple);
        return bundle;
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        super.onRestoreStateForActivityResult(state, callbackContext);
        this.mimeType = state.getString(STATE_PROPERTY_MIME_TYPE);
        this.multiple = state.getBoolean(STATE_PROPERTY_MULTIPLE);
        this.mCallbackContext = callbackContext;
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("selectFiles".equals(action)) {
            JSONObject options = args.getJSONObject(0);
            String type = options.getString("type");
            if (type == "IMAGE") {
                this.mimeType = "image/*";
            } else if (type == "VIDEO") {
                this.mimeType = "video/*";
            } else if (type == "AUDIO") {
                this.mimeType = "audio/*";
            } else {
                this.mimeType = "*/*";
            }
            this.mimeType = options.getString(STATE_PROPERTY_MIME_TYPE);
            this.multiple = options.getBoolean(STATE_PROPERTY_MULTIPLE);
            this.mCallbackContext = callbackContext;
            if (!this.cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                this.cordova.requestPermission(this, 0, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                this.selectFiles();
            }
        } else {
            return false;
        }
        return true;
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        JSONArray files = new JSONArray();
            if (requestCode == FILE_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            files.put("" + FileHelper.getPath(this.cordova.getActivity(), clipData.getItemAt(i).getUri()));
                        }
                    } else if (data.getData() != null) {
                        files.put("" + FileHelper.getPath(this.cordova.getActivity(), data.getData()));
                    }
                    this.mCallbackContext.success(files);
                    this.mCallbackContext = null;
                }
            }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.mCallbackContext.error(PERMISSION_DENIED);
                this.mCallbackContext = null;
                return;
            }
        }
        if (requestCode == WRITE_PERMISSION_REQUEST_CODE) {
            selectFiles();
        }
    }

    private void selectFiles() {
        Intent showBrowser = new Intent();
        showBrowser.setAction(Intent.ACTION_GET_CONTENT);
        showBrowser.addCategory(Intent.CATEGORY_OPENABLE);
        showBrowser.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        showBrowser.setType(mimeType);
        this.cordova.startActivityForResult(this, showBrowser, FILE_REQUEST_CODE);
    }
}

