package com.example.personal.subirimagengaleria;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE=1;
    static final int REQUEST_TAKE_PHOTO=1;
    String pathFoto;
    private StorageReference mStorageRef;
    private Uri downloadUrl;
    private String nombreCompletoImagen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference mStorageRef = storage.getReferenceFromUrl("gs://subirimagengaleria.appspot.com");
        /*mStorageRef=FirebaseStorage.getInstance().getReference();
        mStorage=mStorageRef.FirebaseStorage.get;*/
        //sacarFoto();

    }

    public void sacarFoto(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
        File photofile=null;
        try{
            photofile=crearImagen();

        }catch(IOException ex){
            Log.d("ERROR","No creado");
        }
        if(photofile!=null){
            Uri photoURI= Uri.fromFile(photofile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            //Toast.makeText(this, "Imagen creada satisfactoriamente", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ImageView imagen = (ImageView) findViewById(R.id.imagen);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(data != null){
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imagen.setImageBitmap(imageBitmap);}
        }

        setPic();
    }

    public File crearImagen() throws IOException {

        String nombreImagen = "JPEG_" + new Date().getTime() + "_";
        nombreCompletoImagen = nombreImagen + ".jpg";
        File directorio=getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imagen = File.createTempFile(nombreImagen, ".jpg", directorio);
        pathFoto=imagen.getAbsolutePath();
        return imagen;

    }

    public void galleryAddPic(View view){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(pathFoto);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "El path de la foto es" + pathFoto,  Toast.LENGTH_LONG).show();
        Toast.makeText(this, "Imagen añadida satisfactoriamente", Toast.LENGTH_SHORT).show();
    }

    private void setPic(){
        ImageView imagen = (ImageView) findViewById(R.id.imagen);
        int targetW = imagen.getWidth();
        Toast.makeText(this, "El tamaño horizontal del ImageView es: " + targetW, Toast.LENGTH_SHORT).show();
        int targetH = imagen.getHeight();
        Toast.makeText(this, "El tamaño vertical del ImageView es: " + targetH, Toast.LENGTH_SHORT).show();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(pathFoto, bmOptions);
        int photoW = bmOptions.outWidth;
        Toast.makeText(this, "El tamaño vertical de la imagen es: " + photoW, Toast.LENGTH_SHORT).show();
        int photoH = bmOptions.outHeight;
        Toast.makeText(this, "El tamaño horizontal de la imagen es: " + photoH, Toast.LENGTH_SHORT).show();

        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        bmOptions.inJustDecodeBounds=false;
        bmOptions.inSampleSize=scaleFactor;
        bmOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(pathFoto,bmOptions);
        imagen.setImageBitmap(bitmap);
        guardarImagenRedimensionada(bitmap);
        Toast.makeText(this, "Imagen escalada satisfactoriamente", Toast.LENGTH_SHORT).show();
    }

    public void guardarImagenRedimensionada(Bitmap bitmap){

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        //guardar en memoria interna

        nombreCompletoImagen = new Date().getTime() + ".png";
        try {
            FileOutputStream outputStream = getApplicationContext().openFileOutput( nombreCompletoImagen, Context.MODE_PRIVATE);
            outputStream.write(byteArray);
            outputStream.close();
            Toast.makeText(this, "La imagen ha sido guardada correctamente " , Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Ha habido un error en la cpnversión ERROR, " + e.getMessage() , Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e2) {
            Toast.makeText(this, "Ha habido un error en la cpnversión ERROR, " + e2.getMessage() , Toast.LENGTH_SHORT).show();
            e2.printStackTrace();
        }


    }

    public void subirImagen(View view){


        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://subirimagengaleria.appspot.com");
        Uri file = Uri.fromFile(new File(getApplicationContext().getFilesDir().getPath()+ "/" + nombreCompletoImagen));
        StorageReference imagesRef = storageRef.child("images/" + nombreCompletoImagen);

        imagesRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        downloadUrl = taskSnapshot.getDownloadUrl();
                        Toast.makeText(MainActivity.this, "Imagen subida correctamente", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Toast.makeText(MainActivity.this, "Ha habido algún error en la carga de datos, el error es " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        // ...
                    }
                });

    }
}
