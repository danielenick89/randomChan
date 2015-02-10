package it.nicassio.randomchan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;


public class RandomActivity extends Activity {

    private boolean already_inited = false;

    private class ImageContainer{
        public Bitmap getImage() {
            return image;
        }

        public void setImage(Bitmap image) {
            this.image = image;
        }

        Bitmap image = null;

        public ImageContainer() {

        }

    }

    class UrlPusher extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                String url= urls[0];
                Log.e("Info", "Processing url '" + url +"'");
                String apiEndpoint = "http://www.nicassio.it/daniele/randomChan/api.php?u="+url;
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(new HttpGet(apiEndpoint));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    String responseString = out.toString();
                    out.close();

                    return responseString;

                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(String responseString) {
            Log.e("Error", responseString);
            if(responseString.equals("ok")) {
                Log.e("Error", "Api accepted url");
                Context context = getApplicationContext();
                CharSequence text = "Url correctly pushed. Thanks!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            } else {
                Log.e("Error", "Api refused url");
                Context context = getApplicationContext();
                CharSequence text = "The url has been refused. Is it an image?";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
    }

    public class UploadFile extends AsyncTask<Intent, Void, String> {

        private final ProgressDialog dialog = new ProgressDialog(
                RandomActivity.this);

        protected void onPreExecute() {
            this.dialog.setMessage("Loading...");
            this.dialog.setCancelable(false);
            this.dialog.show();
        }


        @Override
        protected String doInBackground(Intent... params){
            //attempting upload

            String url = "http://www.nicassio.it/daniele/randomChan/api.php";
            //String url = "http://192.168.0.108/randomChan/api.php";
            Intent intent = params[0];
            Uri imageUri = intent.getData();

            if(imageUri == null) { //pushed by sharing, not picking
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }

            if(imageUri == null) {
                Log.e("Info", "imageUri is null");
                return null;
            }

            Log.e("Info", "imageuri: "+imageUri.getPath());



            try {
                InputStream in = getContentResolver().openInputStream(imageUri);

                //converting inputstream to file somehow (java sucks)

                String filename ="cache"+String.valueOf(Math.random());


                final File file = new File(getCacheDir(),filename );
                final OutputStream output = new FileOutputStream(file);
                try {
                    try {
                        final byte[] buffer = new byte[1024];
                        int read;

                        while ((read = in.read(buffer)) != -1)
                            output.write(buffer, 0, read);

                        output.flush();
                    } finally {
                        output.close();
                    }
                } catch (Exception e) {
                    Log.e("Error", "Error converting inputstream to file");
                    e.printStackTrace();
                    in.close();
                    return null;
                }



                HttpClient httpclient = new DefaultHttpClient();

                HttpPost httppost = new HttpPost(url);

                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                entity.addPart("upfile",new FileBody(file));

                /*InputStreamEntity reqEntity = new InputStreamEntity(in, -1);
                reqEntity.setContentType(intent.getType());
                reqEntity.setChunked(true); // Send in multiple parts if needed*/
                httppost.setEntity(entity);
                HttpResponse response = httpclient.execute(httppost);
                return  EntityUtils.toString(response.getEntity());
            } catch(FileNotFoundException e) {
                Log.e("Error", "File not found");
                return null;
            } catch (Exception e) {
                // show error
                e.printStackTrace();
                Log.e("Error", "Some other error");
                return null;
            }
        }

        protected void onPostExecute(String responseString) {


            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if(responseString == null) {
                Log.e("Error", "null responseString");
            } else {
                Log.e("Error", responseString);
            }
            if(responseString!= null && responseString.equals("ok")) {
                Log.e("Error", "Api accepted url");
                Context context = getApplicationContext();
                CharSequence text = "Url correctly pushed. Thanks!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            } else {
                Log.e("Error", "Api refused url");
                Context context = getApplicationContext();
                CharSequence text = "The url has been refused. It may be too big, or something evil just happened. I'm disappointed as you are. Or maybe more";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }

            initImages();

        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageContainer image;
        RandomActivity activity;
        ProgressDialog barProgressDialog;
        Boolean show;


        public DownloadImageTask(RandomActivity activity,ImageContainer image) {
            this(activity,image,false);
        }

        public DownloadImageTask(RandomActivity activity,ImageContainer image, Boolean show) {
            this.activity = activity;
            this.image = image;
            this.show = show;

            if(show) {
                barProgressDialog = new ProgressDialog(RandomActivity.this);
                barProgressDialog.setCanceledOnTouchOutside(false);
                barProgressDialog.setTitle("Loading Image ...");
                barProgressDialog.setMessage("Loading in progress ...");
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                barProgressDialog.show();
            }
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if(result != null) {
                image.setImage(result);
                if(show) {
                    activity.show();
                }
                Log.e("Info", "Image loaded");
            } else {
                Log.e("Error", "Cannot retrieve given image");
            }
            if(show) {
                barProgressDialog.dismiss();
            }
        }
    }

    ImageView image;
    ImageContainer currentImage,nextImage;
    ArrayList<ImageContainer> images = new ArrayList<ImageContainer>();
    final static Integer BUFFER_LENGTH = 10;
    final static Integer REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random);
        image = (ImageView) findViewById(R.id.imageView);

        for(int i=0; i<BUFFER_LENGTH; i++) {
            images.add(new ImageContainer());
        }

        Intent intent = getIntent();
        if(intent != null) {


            // Figure out what to do based on the intent type
            if (intent.getType() == null) {
                initImages();
            } else
            if (intent.getType().startsWith("text/")) {

                final String data = intent.getStringExtra(Intent.EXTRA_TEXT);

                if(data == null) {
                    Log.e("Error", "Fuck null string");
                }

                //confirming and pushing this up
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        new UrlPusher().execute(data);
                        initImages();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        initImages();
                    }
                });


                // 2. Chain together various setter methods to set the dialog characteristics
                builder.setMessage("Do you wanna push the URL up?")
                        .setTitle("Pushing URL!");




                // 3. Get the AlertDialog from create()
                AlertDialog dialog = builder.create();

                dialog.show();
            } else if(intent.getType().startsWith("image/")) {
                Log.e("Info", "Image detected!");

                new UploadFile().execute(intent);
            }
        } else {
            initImages();
        }
    }

    public void initImages() {
        if(already_inited == true) return;
        already_inited = true;
        for(int i=0; i<BUFFER_LENGTH; i++) {
            preloadImage(images.get(i),i==0);
        }
    }

    public void show() {
        image.setImageBitmap(images.get(0).getImage());
    }


    private void preloadImage(ImageContainer container) {
        preloadImage(container,false);
    }

    private void preloadImage(ImageContainer container, Boolean show) {
        new DownloadImageTask(this,container,show).execute("http://www.nicassio.it/daniele/randomChan/api.php?" + (new Date()).getTime());
    }

    public void onImageClick(View view) {
        images.remove(0); //shifting
        if(images.get(0).getImage() == null) {
            preloadImage(images.get(0),true);
        } else {
            show();
        }
        ImageContainer toPush = new ImageContainer();
        preloadImage(toPush);
        images.add(toPush);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_random, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {


            // Set an EditText view to get user input
            final EditText input = new EditText(this);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    Log.e("Info", "user input: " + input.getText().toString());
                    new UrlPusher().execute(input.getText().toString());
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });



            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("Paste the URL of the image you want to push up!")
                    .setTitle("Pushing URL!");




            // 3. Get the AlertDialog from create()
            AlertDialog dialog = builder.create();


            dialog.setView(input);

            dialog.show();
            return true;
        } else  if (id == R.id.action_push_image) {

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, REQUEST_CODE);


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            new UploadFile().execute(data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
