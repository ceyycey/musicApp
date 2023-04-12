package com.example.emotionn_api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
public class MainActivity extends AppCompatActivity {


    private ImageView imageView;
    private TextView resultText;
    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int REQUEST_PERMISSION_CODE = 200;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout. activity_main );



        imageView = (ImageView) findViewById(R.id. imageView );
        resultText = (TextView) findViewById(R.id. resultText );
    }


    // duygu bul butonu
    public void getEmotion(View view) {

        GetEmotionCall emotionCall = new GetEmotionCall(imageView);
        emotionCall.execute();
    }


    //galeriden seç butonu
    public void getImage(View view) {
// galeri izin kontrol
        if(checkPermission()) {
            Intent choosePhotoIntent = new Intent(Intent. ACTION_PICK , android.provider.MediaStore.Images.Media. EXTERNAL_CONTENT_URI );
            startActivity(choosePhotoIntent );
        }
        else {
            requestPermission();
        }
    }


    // seçilen resmi galeriden alma
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


// galeriden fotoğraf URI'sini alma  URI'den dosya yolunu bulma
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {


            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
// galerideki görüntünün yolunu saklayacak bir dize değişkeni
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            imageView.setImageBitmap(bitmap);
        }
    }


    // görüntüyü Emotion API'ye gönderebilmek için görüntüyü taban 64'e dönüştürmek gerek
    public byte[] toBase64(ImageView imgPreview) {
        Bitmap bm = ((BitmapDrawable) imgPreview.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat. JPEG , 100, baos); //bm is the bitmap object
        return baos.toByteArray();
    }




    // izin
    private void requestPermission() {
        ActivityCompat. requestPermissions (MainActivity.this,new String[]{ READ_EXTERNAL_STORAGE }, REQUEST_PERMISSION_CODE );
    }




    public boolean checkPermission() {
        int result = ContextCompat. checkSelfPermission (getApplicationContext(), READ_EXTERNAL_STORAGE );
        return result == PackageManager. PERMISSION_GRANTED ;
    }



    private class GetEmotionCall extends AsyncTask<Void, Void, String> {


        private final ImageView img;


        GetEmotionCall(ImageView img) {
            this.img = img;
        }


        //API çağrısı yapılmadan önce bu işlev çağrılacak
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultText.setText("Sonuç bulunuyor...");
        }


        //API çağrısı yapıldığında bu işlev çağrılacak
        @Override

        protected String doInBackground(Void... params) {
            HttpClient httpclient = HttpClients. createDefault ();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode. setThreadPolicy (policy);


            try {
                URIBuilder builder = new URIBuilder("https://ceyy-face-api.cognitiveservices.azure.com/face/v1.0/");


                URI uri = builder.build();
                HttpPost request = new HttpPost(uri);
                request.setHeader("Content-Type", "application/octet-stream");
                request.setHeader("Ocp-Apim-Subscription-Key", "668b59b82999434f8ea85a67267766bf");



                request.setEntity(new ByteArrayEntity(toBase64(img)));



                HttpResponse response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();
                String res = EntityUtils. toString (entity);


                return res;


            }
            catch (Exception e){
                return "null";
            }


        }


        // API çağrısından bir sonuç alınca bu fonksiyon çağrılacak
        @Override
        protected void onPostExecute(String result) {
            JSONArray jsonArray = null;
            try {

                jsonArray = new JSONArray(result);
                String emotions = "";

                for(int i = 0;i<jsonArray.length();i++) {
                    JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                    JSONObject scores = jsonObject.getJSONObject("scores");
                    double max = 0;
                    String emotion = "";
                    for (int j = 0; j < scores.names().length(); j++) {
                        if (scores.getDouble(scores.names().getString(j)) > max) {
                            max = scores.getDouble(scores.names().getString(j));
                            emotion = scores.names().getString(j);
                        }
                    }
                    emotions += emotion + "\n";
                }
                resultText.setText(emotions);


            } catch (JSONException e) {
                resultText.setText("Duygu tespit edilmedi. Daha sonra tekrar deneyin.");
            }
        }
    }

}